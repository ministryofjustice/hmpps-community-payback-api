package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProjectService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.SessionService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.SessionMappers
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class SessionServiceTest {

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var sessionMappers: SessionMappers

  @RelaxedMockK
  lateinit var sessionSupervisorEntityRepository: SessionSupervisorEntityRepository

  @RelaxedMockK
  lateinit var contextService: ContextService

  @RelaxedMockK
  lateinit var projectService: ProjectService

  @InjectMockKs
  private lateinit var service: SessionService

  @Nested
  inner class GetSessions {

    @Test
    fun `if date range greater than 7 days throw exception`() {
      assertThatThrownBy {
        service.getSessions(
          providerCode = "provider code 1",
          teamCode = "team code 1",
          startDate = LocalDate.of(2025, 1, 1),
          endDate = LocalDate.of(2025, 1, 9),
          projectTypeGroup = null,
        )
      }.isInstanceOf(BadRequestException::class.java).hasMessage("Date range cannot be greater than 7 days")
    }

    @Test
    fun `success path`() {
      every {
        projectService.projectTypesForGroup(ProjectTypeGroupDto.GROUP)
      } returns listOf(ProjectTypeDto.valid().copy(code = "PT1"))

      every {
        communityPaybackAndDeliusClient.getSessions(
          providerCode = "provider code 1",
          teamCode = "team code 1",
          startDate = LocalDate.of(2025, 1, 1),
          endDate = LocalDate.of(2025, 1, 5),
          projectTypeCodes = listOf("PT1"),
        )
      } returns NDSessionSummaries.valid().copy(
        sessions = listOf(
          NDSessionSummary.valid(),
          NDSessionSummary.valid(),
          NDSessionSummary.valid(),
        ),
      )

      val result = service.getSessions(
        providerCode = "provider code 1",
        teamCode = "team code 1",
        startDate = LocalDate.of(2025, 1, 1),
        endDate = LocalDate.of(2025, 1, 5),
        projectTypeGroup = ProjectTypeGroupDto.GROUP,
      )

      assertThat(result.allocations).hasSize(3)
    }
  }
}
