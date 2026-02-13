package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementActionsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject

class CommonReferencesIT : IntegrationTestBase() {

  @Autowired
  lateinit var projectTypeEntityRepository: ProjectTypeEntityRepository

  lateinit var contactOutcomesEntityRepository: ContactOutcomeEntityRepository

  @Nested
  @DisplayName("GET /common/references/project-types")
  inner class ProjectTypesEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/common/references/project-types")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/common/references/project-types")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/common/references/project-types")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with project types`() {
      val seededProjectTypes = projectTypeEntityRepository.findAll()

      val projectTypes = webTestClient.get()
        .uri("/common/references/project-types")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProjectTypesDto>()

      assertThat(projectTypes.projectTypes).hasSize(seededProjectTypes.size)

      projectTypes.projectTypes.forEach {
        assertThat(seededProjectTypes).anyMatch { seeded -> seeded.code == it.code }
        assertThat(seededProjectTypes).anyMatch { seeded -> seeded.name == it.name }
        assertThat(seededProjectTypes).anyMatch { seeded -> seeded.id == it.id }
      }
    }
  }

  @Nested
  @DisplayName("GET /common/references/contact-outcomes")
  inner class ContactOutcomesEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/common/references/contact-outcomes")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/common/references/contact-outcomes")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/common/references/contact-outcomes")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with contact outcomes`() {
      val contactOutcomes = webTestClient.get()
        .uri("/common/references/contact-outcomes")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ContactOutcomesDto>()

      assertThat(contactOutcomes.contactOutcomes).hasSize(21)
      assertThat(contactOutcomes.contactOutcomes[0].id).isNotNull
      assertThat(contactOutcomes.contactOutcomes[0].name).isEqualTo("Acceptable Absence - Court/Legal")
      assertThat(contactOutcomes.contactOutcomes[0].code).isEqualTo("AACL")
      assertThat(contactOutcomes.contactOutcomes[1].id).isNotNull
      assertThat(contactOutcomes.contactOutcomes[1].name).isEqualTo("Acceptable Absence - Employment")
      assertThat(contactOutcomes.contactOutcomes[1].code).isEqualTo("AAEM")
      assertThat(contactOutcomes.contactOutcomes[2].id).isNotNull
      assertThat(contactOutcomes.contactOutcomes[2].name).isEqualTo("Acceptable Absence - Family/ Childcare")
      assertThat(contactOutcomes.contactOutcomes[2].code).isEqualTo("AAFC")
    }

    @Test
    fun `should filter on provided group`() {
      val contactOutcomes = webTestClient.get()
        .uri("/common/references/contact-outcomes?group=AVAILABLE_TO_ADMIN")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ContactOutcomesDto>()

      assertThat(contactOutcomes.contactOutcomes).hasSize(10)
    }
  }

  @Nested
  @DisplayName("GET /common/references/enforcement-actions")
  inner class EnforcementActionsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/common/references/enforcement-actions")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/common/references/enforcement-actions")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/common/references/enforcement-actions")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with enforcement actions`() {
      val enforementActions = webTestClient.get()
        .uri("/common/references/enforcement-actions")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<EnforcementActionsDto>()

      assertThat(enforementActions.enforcementActions).hasSize(19)
      assertThat(enforementActions.enforcementActions[0].id).isNotNull
      assertThat(enforementActions.enforcementActions[0].name).isEqualTo("Breach / Recall Initiated")
      assertThat(enforementActions.enforcementActions[0].code).isEqualTo("IBR")
      assertThat(enforementActions.enforcementActions[1].id).isNotNull
      assertThat(enforementActions.enforcementActions[1].name).isEqualTo("Breach Confirmation Sent")
      assertThat(enforementActions.enforcementActions[1].code).isEqualTo("EA10")
      assertThat(enforementActions.enforcementActions[2].id).isNotNull
      assertThat(enforementActions.enforcementActions[2].name).isEqualTo("Breach Letter Sent")
      assertThat(enforementActions.enforcementActions[2].code).isEqualTo("EA08")
    }
  }
}
