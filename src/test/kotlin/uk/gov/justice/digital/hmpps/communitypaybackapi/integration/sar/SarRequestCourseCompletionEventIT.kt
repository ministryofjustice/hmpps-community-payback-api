package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atFirstSecondOfDay
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atLastSecondOfDay
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionResolution
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar.SarRequestIT.Companion.CRN
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar.SarRequestIT.Companion.RANGE_TEST_FROM_DATE
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar.SarRequestIT.Companion.RANGE_TEST_TO_DATE
import java.time.LocalDate
import java.time.LocalTime

/**
 * Top level SAR tests are defined in [SarRequestIT]
 *
 * This class defines tests to ensure that the correct course completion event data
 * is returned from the API endpoint
 */
class SarRequestCourseCompletionEventIT : IntegrationTestBase() {

  @BeforeEach
  fun clearTestData() {
    FixtureFactory(ctx).clearTestData()
  }

  fun setupTestData() {
    FixtureFactory(ctx).setupRangeTestData()
  }

  @Test
  fun `filter with no dates`() {
    setupTestData()

    webTestClient.get().uri("/subject-access-request?crn=$CRN")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.eteCourseCompletionEvents[0].firstName").isEqualTo("resolution recorded just outside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[1].firstName").isEqualTo("received at just outside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[2].firstName").isEqualTo("resolution recorded just inside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[3].firstName").isEqualTo("received at just inside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[4].firstName").isEqualTo("resolution recorded just inside start of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[5].firstName").isEqualTo("received at just inside start of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[6].firstName").isEqualTo("resolution recorded just before requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[7].firstName").isEqualTo("received at just before requested range")
  }

  @Test
  fun `filter on from and to date`() {
    setupTestData()

    webTestClient.get()
      .uri("/subject-access-request?crn=$CRN&fromDate=$RANGE_TEST_FROM_DATE&toDate=$RANGE_TEST_TO_DATE")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.eteCourseCompletionEvents[0].firstName").isEqualTo("resolution recorded just inside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[1].firstName").isEqualTo("received at just inside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[2].firstName").isEqualTo("resolution recorded just inside start of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[3].firstName").isEqualTo("received at just inside start of requested range")
  }

  @Test
  fun `filter on from date`() {
    setupTestData()

    webTestClient.get().uri("/subject-access-request?crn=$CRN&fromDate=$RANGE_TEST_FROM_DATE")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.eteCourseCompletionEvents[0].firstName").isEqualTo("resolution recorded just outside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[1].firstName").isEqualTo("received at just outside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[2].firstName").isEqualTo("resolution recorded just inside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[3].firstName").isEqualTo("received at just inside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[4].firstName").isEqualTo("resolution recorded just inside start of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[5].firstName").isEqualTo("received at just inside start of requested range")
  }

  @Test
  fun `filter on to date`() {
    setupTestData()

    webTestClient.get().uri("/subject-access-request?crn=$CRN&toDate=$RANGE_TEST_TO_DATE")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.eteCourseCompletionEvents[0].firstName").isEqualTo("resolution recorded just inside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[1].firstName").isEqualTo("received at just inside end of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[2].firstName").isEqualTo("resolution recorded just inside start of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[3].firstName").isEqualTo("received at just inside start of requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[4].firstName").isEqualTo("resolution recorded just before requested range")
      .jsonPath("$.content.eteCourseCompletionEvents[5].firstName").isEqualTo("received at just before requested range")
  }

  class FixtureFactory(
    private val ctx: ApplicationContext,
  ) {
    val appointmentEventRepository = ctx.getBean<AppointmentEventEntityRepository>()
    val eteCourseCompletionEventEntityRepository = ctx.getBean<EteCourseCompletionEventEntityRepository>()
    val pduEntityRepository = ctx.getBean<CommunityCampusPduEntityRepository>()
    val appointmentEventFixtureFactory = SarRequestAppointmentEventIT.FixtureFactory(ctx)

    fun clearTestData() {
      eteCourseCompletionEventEntityRepository.deleteAll()
    }

    fun setupRangeTestData() {
      val dateFarOutsideOfRange = RANGE_TEST_TO_DATE.plusDays(100).atLastSecondOfDay()

      eteCourseCompletionEventEntityRepository.saveAll(
        listOf(
          baselineCourseCompletion().run {
            copy(
              receivedAt = RANGE_TEST_FROM_DATE.minusDays(1).atLastSecondOfDay().minusSeconds(1),
              firstName = "received at just before requested range",
              resolution = resolution!!.copy(
                createdAt = dateFarOutsideOfRange,
              ),
            )
          },
          baselineCourseCompletion().run {
            copy(
              receivedAt = dateFarOutsideOfRange,
              firstName = "resolution recorded just before requested range",
              resolution = resolution!!.copy(
                createdAt = RANGE_TEST_FROM_DATE.minusDays(1).atLastSecondOfDay(),
              ),
            )
          },
          baselineCourseCompletion().run {
            copy(
              receivedAt = RANGE_TEST_FROM_DATE.atFirstSecondOfDay(),
              firstName = "received at just inside start of requested range",
              resolution = resolution!!.copy(
                createdAt = dateFarOutsideOfRange,
              ),
            )
          },
          baselineCourseCompletion().run {
            copy(
              receivedAt = dateFarOutsideOfRange,
              firstName = "resolution recorded just inside start of requested range",
              resolution = resolution!!.copy(
                createdAt = RANGE_TEST_FROM_DATE.minusDays(1).atFirstSecondOfDay().plusSeconds(1),
              ),
            )
          },
          baselineCourseCompletion().run {
            copy(
              receivedAt = RANGE_TEST_TO_DATE.atLastSecondOfDay().minusSeconds(1),
              firstName = "received at just inside end of requested range",
              resolution = resolution!!.copy(
                createdAt = dateFarOutsideOfRange,
              ),
            )
          },
          baselineCourseCompletion().run {
            copy(
              receivedAt = dateFarOutsideOfRange,
              firstName = "resolution recorded just inside end of requested range",
              resolution = resolution!!.copy(
                createdAt = RANGE_TEST_TO_DATE.atLastSecondOfDay(),
              ),
            )
          },
          baselineCourseCompletion().run {
            copy(
              receivedAt = RANGE_TEST_TO_DATE.plusDays(1).atFirstSecondOfDay(),
              firstName = "received at just outside end of requested range",
              resolution = resolution!!.copy(
                createdAt = dateFarOutsideOfRange,
              ),
            )
          },
          baselineCourseCompletion().run {
            copy(
              receivedAt = dateFarOutsideOfRange,
              firstName = "resolution recorded just outside end of requested range",
              resolution = resolution!!.copy(
                createdAt = RANGE_TEST_TO_DATE.plusDays(1).atFirstSecondOfDay().plusSeconds(1),
              ),
            )
          },
        ),
      )
    }

    fun setupReportTestData() {
      val hasAppointment = baselineCourseCompletion().run {
        appointmentEventRepository.save(
          appointmentEventFixtureFactory.baselineAppointmentEvent().copy(
            triggerType = AppointmentEventTriggerType.ETE_COURSE_COMPLETION_RESOLUTION,
            triggeredBy = resolution!!.id.toString(),
            triggeredAt = RANGE_TEST_FROM_DATE.atLastSecondOfDay(),
            projectName = "project 2",
            date = LocalDate.of(2025, 5, 1),
            startTime = LocalTime.of(0, 0),
            endTime = LocalTime.of(1, 0),
          ),
        )

        copy(
          receivedAt = RANGE_TEST_FROM_DATE.atFirstSecondOfDay(),
          firstName = "has",
          lastName = "appointment",
          dateOfBirth = LocalDate.of(2020, 1, 1),
          region = "region one",
          pdu = pduEntityRepository.findByName("Cardiff and Vale")!!,
          office = "office one",
          email = "email one",
          courseName = "course one",
          courseType = "course type one",
          provider = "provider one",
          completionDateTime = LocalDate.of(2025, 6, 1).atFirstSecondOfDay(),
          status = EteCourseCompletionEventStatus.PASSED,
          totalTimeMinutes = 10,
          expectedTimeMinutes = 20,
          attempts = 2,
          resolution = resolution!!.copy(
            createdAt = RANGE_TEST_FROM_DATE.atLastSecondOfDay(),
            createdByUsername = "user1",
            resolution = EteCourseCompletionResolution.CREDIT_TIME,
            minutesCredited = 101,
            deliusAppointmentCreated = true,
          ),
        )
      }

      val hasNoAppointment = baselineCourseCompletion().run {
        copy(
          receivedAt = RANGE_TEST_TO_DATE.atFirstSecondOfDay(),
          firstName = "has no",
          lastName = "appointment",
          dateOfBirth = LocalDate.of(2020, 1, 2),
          region = "region two",
          pdu = pduEntityRepository.findByName("Cwm Taf Morgannwg")!!,
          office = "office two",
          email = "email two",
          courseName = "course two",
          courseType = "course type two",
          provider = "provider two",
          completionDateTime = LocalDate.of(2025, 6, 2).atFirstSecondOfDay(),
          status = EteCourseCompletionEventStatus.PASSED,
          totalTimeMinutes = 30,
          expectedTimeMinutes = 40,
          attempts = null,
          resolution = resolution!!.copy(
            createdAt = RANGE_TEST_TO_DATE.atLastSecondOfDay(),
            createdByUsername = "user2",
            resolution = EteCourseCompletionResolution.COURSE_ALREADY_COMPLETED_WITHIN_THRESHOLD,
            minutesCredited = null,
            deliusAppointmentCreated = null,
          ),
        )
      }

      eteCourseCompletionEventEntityRepository.saveAll(
        listOf(
          hasAppointment,
          hasNoAppointment,
        ),

      )
    }

    private fun baselineCourseCompletion(): EteCourseCompletionEventEntity {
      val completion = EteCourseCompletionEventEntity.valid(ctx)
      val resolution = EteCourseCompletionEventResolutionEntity.valid(ctx).copy(
        eteCourseCompletionEvent = completion,
        crn = CRN,
      )
      completion.resolution = resolution

      return completion
    }
  }
}
