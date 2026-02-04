package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import javax.sql.DataSource

/**
 * These tests are defined by https://github.com/ministryofjustice/hmpps-subject-access-request-lib
 *
 * They will check the expected response is returned for Subject Access Requests (SARs), and also act as a
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
 * `SAR_GENERATE_ACTUAL=true ./gradlew test --tests "uk.gov.justice.digital.hmpps.communitypaybackapi.integration.SubjectAccessRequestIT"`
 *
 * And copy the contents of `/src/test/resources/entity-schema.json.log`  into `/src/test/resources/expected-jpa-schema.json`.
 *
 * Then delete the generated files '.log' files
 */
class SarRequestIT : IntegrationTestBase() {

  private companion object {
    const val CRN = "CRN12345"
  }

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var dataSource: DataSource

  @Nested
  inner class ApiDataTest : uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest {

    override fun setupTestData() {
      // for the initial implementation we return an empty string
    }

    override fun getCrn() = CRN
    override fun getWebTestClientInstance() = webTestClient
    override fun getSarHelper() = sarIntegrationTestHelper
  }

  @Nested
  inner class ReportTest : SarReportTest {
    override fun setupTestData() {
      // for the initial implementation we return an empty string
    }

    override fun getCrn() = CRN
    override fun getWebTestClientInstance() = webTestClient
    override fun getSarHelper() = sarIntegrationTestHelper
  }

  @Nested
  inner class FlywaySchemaTest : uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest {
    override fun getSarHelper() = sarIntegrationTestHelper
    override fun getDataSourceInstance() = dataSource
  }

  @Nested
  inner class JpaEntitiesTest : uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest {
    override fun getSarHelper() = sarIntegrationTestHelper
    override fun getEntityManagerInstance() = entityManager
  }
}
