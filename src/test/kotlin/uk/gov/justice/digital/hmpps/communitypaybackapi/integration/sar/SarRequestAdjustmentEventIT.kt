package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atFirstSecondOfDay
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atLastSecondOfDay
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.persist
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar.SarRequestIT.Companion.CRN
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar.SarRequestIT.Companion.RANGE_TEST_FROM_DATE
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar.SarRequestIT.Companion.RANGE_TEST_TO_DATE
import java.time.LocalDate

/**
 * Top level SAR tests are defined in [SarRequestIT]
 *
 * This class defines tests to ensure that the correct adjustment event data
 * is returned from the API endpoint
 */
class SarRequestAdjustmentEventIT : IntegrationTestBase() {

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
      .jsonPath("$.content.appointments[0].adjustmentEvents[0].deliusEventNumber").isEqualTo(1)
      .jsonPath("$.content.appointments[1].adjustmentEvents[0].deliusEventNumber").isEqualTo(2)
      .jsonPath("$.content.appointments[2].adjustmentEvents[0].deliusEventNumber").isEqualTo(3)
      .jsonPath("$.content.appointments[3].adjustmentEvents[0].deliusEventNumber").isEqualTo(4)
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
      .jsonPath("$.content.appointments[0].adjustmentEvents[0].deliusEventNumber").isEqualTo(2)
      .jsonPath("$.content.appointments[1].adjustmentEvents[0].deliusEventNumber").isEqualTo(3)
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
      .jsonPath("$.content.appointments[0].adjustmentEvents[0].deliusEventNumber").isEqualTo(2)
      .jsonPath("$.content.appointments[1].adjustmentEvents[0].deliusEventNumber").isEqualTo(3)
      .jsonPath("$.content.appointments[2].adjustmentEvents[0].deliusEventNumber").isEqualTo(4)
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
      .jsonPath("$.content.appointments[0].adjustmentEvents[0].deliusEventNumber").isEqualTo(1)
      .jsonPath("$.content.appointments[1].adjustmentEvents[0].deliusEventNumber").isEqualTo(2)
      .jsonPath("$.content.appointments[2].adjustmentEvents[0].deliusEventNumber").isEqualTo(3)
  }

  class FixtureFactory(
    private val ctx: ApplicationContext,
  ) {

    val adjustmentEventEntityRepository = ctx.getBean<AdjustmentEventEntityRepository>()

    fun clearTestData() {
      adjustmentEventEntityRepository.deleteAll()
    }

    fun setupRangeTestData() {
      adjustmentEventEntityRepository.saveAll(
        listOf(
          baselineAdjustmentEvent().copy(
            triggeredAt = RANGE_TEST_FROM_DATE.minusDays(1).atLastSecondOfDay(),
            appointment = baselineAppointment().copy(
              deliusEventNumber = 1,
              date = RANGE_TEST_FROM_DATE.minusDays(1),
            ).persist(ctx),
          ),
          baselineAdjustmentEvent().copy(
            triggeredAt = RANGE_TEST_FROM_DATE.atFirstSecondOfDay(),
            appointment = baselineAppointment().copy(
              deliusEventNumber = 2,
              date = RANGE_TEST_FROM_DATE,
            ).persist(ctx),
          ),
          baselineAdjustmentEvent().copy(
            triggeredAt = RANGE_TEST_TO_DATE.atLastSecondOfDay(),
            appointment = baselineAppointment().copy(
              deliusEventNumber = 3,
              date = RANGE_TEST_TO_DATE,
            ).persist(ctx),
          ),
          baselineAdjustmentEvent().copy(
            triggeredAt = RANGE_TEST_TO_DATE.plusDays(1).atFirstSecondOfDay(),
            appointment = baselineAppointment().copy(
              deliusEventNumber = 4,
              date = RANGE_TEST_TO_DATE.plusDays(1),
            ).persist(ctx),
          ),
        ),
      )
    }

    fun setupReportTestData(
      appointment1: AppointmentEntity,
      appointment2: AppointmentEntity,
    ) {
      adjustmentEventEntityRepository.saveAll(
        listOf(
          baselineAdjustmentEvent().copy(
            triggeredAt = RANGE_TEST_FROM_DATE.minusDays(1).atLastSecondOfDay(),
            appointment = appointment1,
            adjustmentType = AdjustmentEventAdjustmentType.POSITIVE,
            adjustmentMinutes = 20,
            adjustmentDate = LocalDate.of(2026, 3, 2),
          ),
          baselineAdjustmentEvent().copy(
            triggeredAt = RANGE_TEST_TO_DATE.atFirstSecondOfDay(),
            appointment = appointment2,
            adjustmentType = AdjustmentEventAdjustmentType.NEGATIVE,
            adjustmentMinutes = 30,
            adjustmentDate = LocalDate.of(2026, 3, 4),
          ),
        ),
      )
    }

    fun baselineAppointment() = AppointmentEntity.valid().copy(
      crn = CRN,
      deliusEventNumber = 1,
      date = LocalDate.of(2026, 3, 4),
      createdByCommunityPayback = true,
    )

    fun baselineAdjustmentEvent() = AdjustmentEventEntity.valid(ctx).copy(
      triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
      triggeredBy = "username1",
      eventType = AdjustmentEventType.CREATE,
      appointment = baselineAppointment().persist(ctx),
      adjustmentType = AdjustmentEventAdjustmentType.POSITIVE,
      adjustmentMinutes = 20,
      adjustmentDate = LocalDate.of(2026, 3, 4),
    )
  }
}
