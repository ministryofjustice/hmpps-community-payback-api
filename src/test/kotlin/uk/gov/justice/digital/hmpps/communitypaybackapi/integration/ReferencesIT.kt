package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ContactOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.EnforcementActionsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ProjectTypesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ProjectTypeEntityRepository

class ReferencesIT : IntegrationTestBase() {

  @Autowired
  lateinit var projectTypeEntityRepository: ProjectTypeEntityRepository

  @Nested
  @DisplayName("GET /references/project-types")
  inner class ProjectTypesEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/references/project-types")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/references/project-types")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/references/project-types")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with project types`() {
      val seededProjectTypes = projectTypeEntityRepository.findAll()

      val projectTypes = webTestClient.get()
        .uri("/references/project-types")
        .addUiAuthHeader()
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
  @DisplayName("GET /references/contact-outcomes")
  inner class ContactOutcomesEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/references/contact-outcomes")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/references/contact-outcomes")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/references/contact-outcomes")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with contact outcomes`() {
      val contactOutcomes = webTestClient.get()
        .uri("/references/contact-outcomes")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ContactOutcomesDto>()

      assertThat(contactOutcomes.contactOutcomes).hasSize(22)
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
  }

  @Nested
  @DisplayName("GET /references/enforcement-actions")
  inner class EnforcementActionsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/references/enforcement-actions")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/references/enforcement-actions")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/references/enforcement-actions")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with enforcement actions`() {
      val enforementActions = webTestClient.get()
        .uri("/references/enforcement-actions")
        .addUiAuthHeader()
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
