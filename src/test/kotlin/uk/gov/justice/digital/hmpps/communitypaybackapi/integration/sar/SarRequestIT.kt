package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import java.time.LocalDate
import javax.sql.DataSource

/**
 * This test file implements the default SAR test framework defined by https://github.com/ministryofjustice/hmpps-subject-access-request-lib
 *
 * There are tests for specific data types defined in standalone classes to keep the size of test classes manageable
 *
 * # After making a content change or upstream formatting changes:
 *
 * Either fix the expected SAR response files in `/src/test/resources/sar` by hand, or if the change is related to
 * upstream formatting you can grab a copy of the generated files using
 *
 *    SAR_GENERATE_ACTUAL=true ./gradlew test --tests "uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar.SarRequestIT"
 *
 * And copy the contents generated in `/src/test/resources` into `/src/test/resources/sar` (renaming as required).
 *
 * # After making a database change:
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
 *    SAR_GENERATE_ACTUAL=true ./gradlew test --tests "uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar.SarRequestIT"
 *
 * And copy the contents of `/src/test/resources/entity-schema.json.log`  into `/src/test/resources/expected-jpa-schema.json`.
 *
 * Then delete the generated files '.log' files
 */
class SarRequestIT : IntegrationTestBase() {

  companion object {
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

  /**
   * Delegates to a test defined in the superclass to check the api response content
   * against /src/test/resources/expected-api-response.json
   */
  @Nested
  inner class ApiDataTest : SarApiDataTest {

    @BeforeEach
    fun clearTestData() {
      this@SarRequestIT.clearTestData()
    }

    override fun setupTestData() {
      this@SarRequestIT.setupTestData()
    }

    override fun getCrn() = CRN
    override fun getWebTestClientInstance() = webTestClient
    override fun getSarHelper() = sarIntegrationTestHelper
  }

  /**
   * Delegates to a test defined in the superclass to check the rendered template content
   * against /src/test/resources/expected-report.html
   */
  @Nested
  inner class ReportTest : SarReportTest {
    @BeforeEach
    fun clearTestData() {
      this@SarRequestIT.clearTestData()
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
      this@SarRequestIT.setupTestData()
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

  private fun clearTestData() {
    SarRequestAppointmentEventFixtureFactory(ctx, appointmentEventEntityRepository).clearTestData()
  }

  private fun setupTestData() {
    SarRequestAppointmentEventFixtureFactory(ctx, appointmentEventEntityRepository).setupTestData()
  }
}
