package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.IDs
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.OffenderDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.OfficeUpwTeamMappingEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.OfficeUpwTeamMappingRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.CourseCompletionAutoResolutionService
import java.util.UUID

@ExtendWith(MockKExtension::class)
class CourseCompletionAutoResolutionServiceTest {

  @RelaxedMockK
  lateinit var personSearchClient: ProbationOffenderSearchClient

  @RelaxedMockK
  lateinit var officeUpwTeamMappingRepository: OfficeUpwTeamMappingRepository

  @RelaxedMockK
  lateinit var draftResolutionRepository: EteCourseCompletionDraftResolutionRepository

  private lateinit var service: CourseCompletionAutoResolutionService

  @BeforeEach
  fun setUp() {
    service = CourseCompletionAutoResolutionService(
      personSearchClient,
      officeUpwTeamMappingRepository,
      draftResolutionRepository,
    )
  }

  @Nested
  inner class ResolveDraft {

    @Test
    fun `persists draft with CRN when person search returns a single match`() {
      val event = EteCourseCompletionEventEntity.valid()
      every { personSearchClient.searchPerson(any()) } returns listOf(
        OffenderDetail(otherIds = IDs(crn = "X12345")),
      )

      val saved = slot<EteCourseCompletionDraftResolutionEntity>()
      every { draftResolutionRepository.save(capture(saved)) } answers { firstArg() }

      service.resolveAndPersistDraft(event)

      assertThat(saved.captured.crn).isEqualTo("X12345")
      assertThat(saved.captured.eteCourseCompletionEvent).isEqualTo(event)
    }

    @Test
    fun `persists draft with null CRN when person search returns no matches`() {
      val event = EteCourseCompletionEventEntity.valid()
      every { personSearchClient.searchPerson(any()) } returns emptyList()

      val saved = slot<EteCourseCompletionDraftResolutionEntity>()
      every { draftResolutionRepository.save(capture(saved)) } answers { firstArg() }

      service.resolveAndPersistDraft(event)

      assertThat(saved.captured.crn).isNull()
    }

    @Test
    fun `persists draft with null CRN when person search returns multiple matches`() {
      val event = EteCourseCompletionEventEntity.valid()
      every { personSearchClient.searchPerson(any()) } returns listOf(
        OffenderDetail(otherIds = IDs(crn = "X00001")),
        OffenderDetail(otherIds = IDs(crn = "X00002")),
      )

      val saved = slot<EteCourseCompletionDraftResolutionEntity>()
      every { draftResolutionRepository.save(capture(saved)) } answers { firstArg() }

      service.resolveAndPersistDraft(event)

      assertThat(saved.captured.crn).isNull()
    }

    @Test
    fun `passes firstName, lastName and dateOfBirth from event to person search`() {
      val event = EteCourseCompletionEventEntity.valid()
      every { personSearchClient.searchPerson(any()) } returns emptyList()
      every { draftResolutionRepository.save(any()) } answers { firstArg() }

      service.resolveAndPersistDraft(event)

      verify {
        personSearchClient.searchPerson(
          match {
            it.firstName == event.firstName &&
              it.surname == event.lastName &&
              it.dateOfBirth == event.dateOfBirth
          },
        )
      }
      verify { draftResolutionRepository.save(any()) }
    }

    @Test
    fun `persists draft with team code when office maps to a UPW team`() {
      val event = EteCourseCompletionEventEntity.valid().copy(office = "St Albans")
      every { personSearchClient.searchPerson(any()) } returns emptyList()
      every {
        officeUpwTeamMappingRepository.findByPduAndOffice(event.pdu, "St Albans")
      } returns OfficeUpwTeamMappingEntity(
        id = UUID.randomUUID(),
        pdu = event.pdu,
        office = "St Albans",
        teamCode = "N56ST",
      )

      val saved = slot<EteCourseCompletionDraftResolutionEntity>()
      every { draftResolutionRepository.save(capture(saved)) } answers { firstArg() }

      service.resolveAndPersistDraft(event)

      assertThat(saved.captured.teamCode).isEqualTo("N56ST")
      verify {
        officeUpwTeamMappingRepository.findByPduAndOffice(event.pdu, "St Albans")
      }
    }

    @Test
    fun `persists draft with null team code when office has no mapping`() {
      val event = EteCourseCompletionEventEntity.valid().copy(office = "Watford")
      every { personSearchClient.searchPerson(any()) } returns emptyList()
      every {
        officeUpwTeamMappingRepository.findByPduAndOffice(event.pdu, "Watford")
      } returns null

      val saved = slot<EteCourseCompletionDraftResolutionEntity>()
      every { draftResolutionRepository.save(capture(saved)) } answers { firstArg() }

      service.resolveAndPersistDraft(event)

      assertThat(saved.captured.teamCode).isNull()
    }

    @Test
    fun `persists draft with null team code when office mapping deliberately has no UPW team`() {
      val event = EteCourseCompletionEventEntity.valid().copy(office = "Watford")
      every { personSearchClient.searchPerson(any()) } returns emptyList()
      every {
        officeUpwTeamMappingRepository.findByPduAndOffice(event.pdu, "Watford")
      } returns OfficeUpwTeamMappingEntity(
        id = UUID.randomUUID(),
        pdu = event.pdu,
        office = "Watford",
        teamCode = null,
      )

      val saved = slot<EteCourseCompletionDraftResolutionEntity>()
      every { draftResolutionRepository.save(capture(saved)) } answers { firstArg() }

      service.resolveAndPersistDraft(event)

      assertThat(saved.captured.teamCode).isNull()
    }

    @Test
    fun `persists draft even when person search fails`() {
      val event = EteCourseCompletionEventEntity.valid()
      every { personSearchClient.searchPerson(any()) } throws RuntimeException("Search failed")

      val saved = slot<EteCourseCompletionDraftResolutionEntity>()
      every { draftResolutionRepository.save(capture(saved)) } answers { firstArg() }

      assertThrows<RuntimeException> {
        service.resolveAndPersistDraft(event)
      }

      verify(exactly = 0) { draftResolutionRepository.save(any()) }
    }
  }

  @Nested
  inner class GetDraftResolutionForCourseCompletion {
    @Test
    fun `delegates to repository method`() {
      val courseCompletionEventId = UUID.randomUUID()
      val expected = EteCourseCompletionDraftResolutionEntity.valid()
      every { draftResolutionRepository.findByEteCourseCompletionEventId(courseCompletionEventId) } returns expected

      val result = service.getDraftResolutionForCourseCompletion(courseCompletionEventId)

      assertThat(result).isEqualTo(expected)
    }
  }
}
