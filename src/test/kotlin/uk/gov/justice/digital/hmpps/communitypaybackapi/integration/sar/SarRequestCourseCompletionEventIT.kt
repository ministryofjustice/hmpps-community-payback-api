package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atFirstSecondOfDay
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atLastSecondOfDay
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
    val eteCourseCompletionEventEntityRepository = ctx.getBean<EteCourseCompletionEventEntityRepository>()
    val pduEntityRepository = ctx.getBean<CommunityCampusPduEntityRepository>()

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

      val all = eteCourseCompletionEventEntityRepository.findAll()
      println(all)
    }

    fun setupReportTestData() {
      val courseCompletion = baselineCourseCompletion().run {
        copy(
          receivedAt = LocalDate.of(2025, 6, 1).atFirstSecondOfDay(),
          firstName = "Rosemary",
          lastName = "Thompson",
          dateOfBirth = LocalDate.of(1976, 6, 1),
          region = "East of England",
          pdu = pduEntityRepository.findByName("North Essex")!!,
          office = "Chelmsford",
          email = "Rosemary.Thompson@example.com",
          courseName = "Effective Communication",
          courseType = "Certified",
          provider = "Moodle",
          completionDateTime = LocalDate.of(2025, 6, 1).atFirstSecondOfDay(),
          status = EteCourseCompletionEventStatus.PASSED,
          totalTimeMinutes = 60,
          expectedTimeMinutes = 60,
          attempts = 1,
          resolution = resolution!!.copy(
            createdAt = RANGE_TEST_FROM_DATE.atLastSecondOfDay(),
            createdByUsername = "isaac.mitchell",
            resolution = EteCourseCompletionResolution.CREDIT_TIME,
            minutesCredited = 60,
            deliusAppointmentCreated = true,
            crn = "X995728",
            notes = "Candidate completed course successfully.",
          ),
        )
      }

      val courseCompletion2 = baselineCourseCompletion().run {
        copy(
          receivedAt = LocalDate.of(2025, 6, 3).atFirstSecondOfDay(),
          firstName = "Rosemary",
          lastName = "Thompson",
          dateOfBirth = LocalDate.of(1976, 6, 1),
          region = "East of England",
          pdu = pduEntityRepository.findByName("North Essex")!!,
          office = "Chelmsford",
          email = "Rosemary.Thompson@example.com",
          courseName = "Building a Professional Brand on LinkedIn",
          courseType = "Accredited",
          provider = "Moodle",
          completionDateTime = LocalDate.of(2025, 6, 3).atFirstSecondOfDay(),
          status = EteCourseCompletionEventStatus.PASSED,
          totalTimeMinutes = 120,
          expectedTimeMinutes = 120,
          attempts = 1,
          resolution = resolution!!.copy(
            createdAt = RANGE_TEST_FROM_DATE.atLastSecondOfDay(),
            createdByUsername = "isaac.mitchell",
            resolution = EteCourseCompletionResolution.CREDIT_TIME,
            minutesCredited = 120,
            deliusAppointmentCreated = false,
            crn = "X995728",
          ),
        )
      }

      val courseCompletion3 = baselineCourseCompletion().run {
        copy(
          receivedAt = LocalDate.of(2025, 6, 4).atFirstSecondOfDay(),
          firstName = "Rosemary",
          lastName = "Thompson",
          dateOfBirth = LocalDate.of(1976, 6, 1),
          region = "East of England",
          pdu = pduEntityRepository.findByName("North Essex")!!,
          office = "Chelmsford",
          email = "Rosemary.Thompson@example.com",
          courseName = "Building your path: A Career in Construction",
          courseType = "Accredited",
          provider = "Alison",
          completionDateTime = LocalDate.of(2025, 6, 4).atFirstSecondOfDay(),
          status = EteCourseCompletionEventStatus.PASSED,
          totalTimeMinutes = 240,
          expectedTimeMinutes = 240,
          attempts = 1,
          resolution = resolution!!.copy(
            createdAt = RANGE_TEST_FROM_DATE.atLastSecondOfDay(),
            createdByUsername = "isaac.mitchell",
            resolution = EteCourseCompletionResolution.CREDIT_TIME,
            minutesCredited = 240,
            deliusAppointmentCreated = true,
            crn = "X995728",
          ),
        )
      }

      eteCourseCompletionEventEntityRepository.saveAll(
        listOf(
          courseCompletion,
          courseCompletion2,
          courseCompletion3,
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
