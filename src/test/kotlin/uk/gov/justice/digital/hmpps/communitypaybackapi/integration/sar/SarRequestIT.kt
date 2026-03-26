package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
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
 * The tests will call the SAR endpoint, render the data using the custom template, and compare these two outputs to the expected values
 * defined in /src/test/resource/sar
 *
 * We've currently disabled the database schema checks which are intended to catch database changes to act as a prompt for the user to consider SAR
 * report updates on data model changes. These will need enabling closer to project go-live and once the formal SAR change control process
 * has begun
 *
 * To quickly regenerate the expected SAR JSON and HTML, run the following:
 *
 *    SAR_GENERATE_ACTUAL=true ./gradlew test --tests "uk.gov.justice.digital.hmpps.communitypaybackapi.integration.sar.SarRequestIT"
 *
 * And then copy the contents generated in `/src/test/resources` into `/src/test/resources/sar`, renaming as required. Check the diff on
 * these new files carefully to ensure they are as expected. Then remove the generated files
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
  lateinit var courseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  /**
   * Delegates to a test defined in the superclass to check the api response content
   * against /src/test/resources/expected-api-response.json
   */
  @Nested
  inner class ApiDataTest : SarApiDataTest {

    @BeforeEach
    fun clearTestData() {
      val findAll = courseCompletionEventEntityRepository.findAll()
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
    SarRequestAppointmentEventIT.FixtureFactory(ctx).clearTestData()
    SarRequestCourseCompletionEventIT.FixtureFactory(ctx).clearTestData()
  }

  private fun setupTestData() {
    SarRequestAppointmentEventIT.FixtureFactory(ctx).setupReportTestData()
    SarRequestCourseCompletionEventIT.FixtureFactory(ctx).setupReportTestData()
  }
}
