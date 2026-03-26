package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atFirstSecondOfDay
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atLastSecondOfDay
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.persist
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
 * This class defines tests to ensure that the correct appointment event data
 * is returned from the API endpoint
 */
class SarRequestAppointmentEventIT : IntegrationTestBase() {

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
      .jsonPath("$.content.appointmentEvents[0].deliusEventNumber").isEqualTo(4)
      .jsonPath("$.content.appointmentEvents[1].deliusEventNumber").isEqualTo(3)
      .jsonPath("$.content.appointmentEvents[2].deliusEventNumber").isEqualTo(2)
      .jsonPath("$.content.appointmentEvents[3].deliusEventNumber").isEqualTo(1)
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
      .jsonPath("$.content.appointmentEvents[0].deliusEventNumber").isEqualTo(3)
      .jsonPath("$.content.appointmentEvents[1].deliusEventNumber").isEqualTo(2)
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
      .jsonPath("$.content.appointmentEvents[0].deliusEventNumber").isEqualTo(4)
      .jsonPath("$.content.appointmentEvents[1].deliusEventNumber").isEqualTo(3)
      .jsonPath("$.content.appointmentEvents[2].deliusEventNumber").isEqualTo(2)
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
      .jsonPath("$.content.appointmentEvents[0].deliusEventNumber").isEqualTo(3)
      .jsonPath("$.content.appointmentEvents[1].deliusEventNumber").isEqualTo(2)
      .jsonPath("$.content.appointmentEvents[2].deliusEventNumber").isEqualTo(1)
  }

  class FixtureFactory(
    private val ctx: ApplicationContext,
  ) {

    val appointmentEventEntityRepository = ctx.getBean<AppointmentEventEntityRepository>()

    fun clearTestData() {
      appointmentEventEntityRepository.deleteAll()
    }

    fun setupRangeTestData() {
      appointmentEventEntityRepository.saveAll(
        listOf(
          baselineAppointmentEvent().copy(
            triggeredAt = RANGE_TEST_FROM_DATE.minusDays(1).atLastSecondOfDay(),
            appointment = baselineAppointment().copy(deliusEventNumber = 1).persist(ctx),
            eventType = AppointmentEventType.CREATE,
          ),
          baselineAppointmentEvent().copy(
            triggeredAt = RANGE_TEST_FROM_DATE.atFirstSecondOfDay(),
            triggerType = AppointmentEventTriggerType.SCHEDULING,
            appointment = baselineAppointment().copy(deliusEventNumber = 2).persist(ctx),
          ),
          baselineAppointmentEvent().copy(
            triggeredAt = RANGE_TEST_TO_DATE.atLastSecondOfDay(),
            triggerType = AppointmentEventTriggerType.ETE_COURSE_COMPLETION_RESOLUTION,
            appointment = baselineAppointment().copy(deliusEventNumber = 3).persist(ctx),
          ),
          baselineAppointmentEvent().copy(
            triggeredAt = RANGE_TEST_TO_DATE.plusDays(1).atFirstSecondOfDay(),
            appointment = baselineAppointment().copy(deliusEventNumber = 4).persist(ctx),
          ),
        ),
      )
    }

    fun setupReportTestData() {
      appointmentEventEntityRepository.saveAll(
        listOf(
          baselineAppointmentEvent().copy(
            triggeredAt = RANGE_TEST_FROM_DATE.minusDays(1).atLastSecondOfDay(),
            appointment = baselineAppointment().copy(deliusEventNumber = 1).persist(ctx),
            eventType = AppointmentEventType.CREATE,
          ),
          baselineAppointmentEvent().copy(
            triggeredAt = RANGE_TEST_FROM_DATE.atFirstSecondOfDay(),
            triggerType = AppointmentEventTriggerType.SCHEDULING,
            appointment = baselineAppointment().copy(deliusEventNumber = 2).persist(ctx),
            eventType = AppointmentEventType.UPDATE,
            projectName = "Some other project name",
            date = LocalDate.of(2026, 3, 4),
            startTime = LocalTime.of(23, 0),
            endTime = LocalTime.of(23, 59),
            pickupLocationDescription = "Some other pickup location",
            pickupTime = LocalTime.of(22, 0),
            notes = "Some different notes",
            contactOutcome = null,
            minutesCredited = null,
            penaltyMinutes = null,
            hiVisWorn = null,
            workedIntensively = null,
            workQuality = null,
            behaviour = null,
            alertActive = null,
            sensitive = null,
          ),
        ),
      )
    }

    fun baselineAppointment() = AppointmentEntity.valid().copy(crn = CRN)

    fun baselineAppointmentEvent() = AppointmentEventEntity.valid(ctx).copy(
      triggerType = AppointmentEventTriggerType.USER,
      triggeredBy = "username1",
      eventType = AppointmentEventType.CREATE,
      appointment = AppointmentEntity.valid().copy(
        crn = CRN,
        deliusEventNumber = 1,
      ).persist(ctx),
      projectName = "The project name",
      date = LocalDate.of(2025, 1, 1),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(15, 0),
      pickupLocationDescription = "The pickup location",
      pickupTime = LocalTime.of(9, 35),
      notes = "The notes",
      hiVisWorn = false,
      workedIntensively = false,
      penaltyMinutes = 20L,
      minutesCredited = 50L,
      workQuality = WorkQuality.SATISFACTORY,
      behaviour = Behaviour.UNSATISFACTORY,
      alertActive = false,
      sensitive = true,
    )
  }
}
