package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.sql.DataSource

/**
 * The test framework used here is defined by https://github.com/ministryofjustice/hmpps-subject-access-request-lib
 *
 * The default tests will check the expected response is returned for Subject Access Requests (SARs), and also act as a
 * prompt to review the SAR returned whenever the entities or databases change.
 *
 * Once you are happy that the SAR response is sufficient following a database change do the following
 *
 * 1. Update the expected flyway schema version
 *
 * This is defined by `expected-flyway-schema-version` in `application-integrationtest.yml`
 *
 * 2. Regenerate the expected-jpa-schema file
 *
 * Run the following
 *
 *    SAR_GENERATE_ACTUAL=true ./gradlew test --tests "uk.gov.justice.digital.hmpps.communitypaybackapi.integration.SarRequestIT"
 *
 * And copy the contents of `/src/test/resources/entity-schema.json.log`  into `/src/test/resources/expected-jpa-schema.json`.
 *
 * Then delete the generated files '.log' files
 */
class SarRequestIT : IntegrationTestBase() {

  private companion object {
    const val CRN = "CRN12345"
    val RANGE_TEST_FROM_DATE: LocalDate = LocalDate.of(2025, 1, 2)
    val RANGE_TEST_TO_DATE: LocalDate = LocalDate.of(2025, 2, 15)
  }

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var appointmentEventEntityRepository: AppointmentEventEntityRepository

  @Nested
  inner class ApiDataTest : SarApiDataTest {

    // there is a test defined in the superclass that checks the entire message
    // content against /src/test/resources/subject-access-request/expected-api-response.json
    // see javadoc on this class for more info

    @BeforeEach
    fun clearTestData() {
      clearTestDataCommon()
    }

    @Test
    fun `filter on no dates`() {
      setupTestDataCommon()

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
      setupTestDataCommon()

      webTestClient.get().uri("/subject-access-request?crn=$CRN&fromDate=$RANGE_TEST_FROM_DATE&toDate=$RANGE_TEST_TO_DATE")
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
      setupTestDataCommon()

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
      setupTestDataCommon()

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

    @Test
    fun `no results returns 204`() {
      webTestClient.get().uri("/subject-access-request?crn=$CRN")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `PRN returns 209`() {
      webTestClient.get().uri("/subject-access-request?prn=XYZ")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isEqualTo(209)
    }

    override fun setupTestData() {
      setupTestDataCommon()
    }

    override fun getCrn() = CRN
    override fun getWebTestClientInstance() = webTestClient
    override fun getSarHelper() = sarIntegrationTestHelper
  }

  @Nested
  inner class ReportTest : SarReportTest {
    @BeforeEach
    fun clearTestData() {
      clearTestDataCommon()
    }

    override fun setupTestData() {
      setupTestDataCommon()
    }

    override fun getCrn() = CRN
    override fun getWebTestClientInstance() = webTestClient
    override fun getSarHelper() = sarIntegrationTestHelper
  }

  // flyway schema checks are disabled whilst we are iterating and finalising the data models
  @Disabled
  @Nested
  inner class FlywaySchemaTest : SarFlywaySchemaTest {
    override fun getSarHelper() = sarIntegrationTestHelper
    override fun getDataSourceInstance() = dataSource
  }

  // jpa entity checks are disabled whilst we are iterating and finalising the data models
  @Disabled
  @Nested
  inner class JpaEntitiesTest : SarJpaEntitiesTest {
    override fun getSarHelper() = sarIntegrationTestHelper
    override fun getEntityManagerInstance() = entityManager
  }

  private fun clearTestDataCommon() {
    appointmentEventEntityRepository.deleteAll()
  }

  private fun setupTestDataCommon() {
    val baselineAppointmentEvent = AppointmentEventEntity.valid(ctx).copy(
      crn = CRN,
      triggerType = AppointmentEventTriggerType.USER,
      triggeredBy = "username1",
      eventType = AppointmentEventType.CREATE,
      deliusEventNumber = 1,
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

    appointmentEventEntityRepository.saveAll(
      listOf(
        baselineAppointmentEvent.copy(
          id = UUID.randomUUID(),
          triggeredAt = RANGE_TEST_FROM_DATE.minusDays(1).atLastSecondOfDay(),
          deliusEventNumber = 1,
          eventType = AppointmentEventType.CREATE,
        ),
        baselineAppointmentEvent.copy(
          id = UUID.randomUUID(),
          triggeredAt = RANGE_TEST_FROM_DATE.atFirstSecondOfDay(),
          triggerType = AppointmentEventTriggerType.SCHEDULING,
          deliusEventNumber = 2,
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
        baselineAppointmentEvent.copy(
          id = UUID.randomUUID(),
          triggeredAt = RANGE_TEST_TO_DATE.atLastSecondOfDay(),
          triggerType = AppointmentEventTriggerType.ETE_COURSE_COMPLETION,
          deliusEventNumber = 3,
        ),
        baselineAppointmentEvent.copy(
          id = UUID.randomUUID(),
          triggeredAt = RANGE_TEST_TO_DATE.plusDays(1).atFirstSecondOfDay(),
          deliusEventNumber = 4,
        ),
      ),
    )
  }

  private fun LocalDate.atFirstSecondOfDay() = this.atTime(0, 0).atZone(ZoneId.systemDefault()).toOffsetDateTime()
  private fun LocalDate.atLastSecondOfDay() = this.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toOffsetDateTime()
}
