package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.reference.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.EnforcementAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.EnforcementActions
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectTypes
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ContactOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.EnforcementActionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.service.toDto
import java.util.UUID

class ReferenceMappersTest {

  @Nested
  inner class ProjectTypesMapper {

    @Test
    fun `should map empty ProjectTypes list correctly`() {
      val projectTypes = ProjectTypes(emptyList())
      val projectTypesDto = projectTypes.toDto()

      assertThat(projectTypesDto.projectTypes).isEmpty()
    }

    @Test
    fun `should map ProjectTypes to DTO correctly`() {
      val projectTypes = ProjectTypes(
        listOf(
          ProjectType(
            id = 1234,
            name = "Community Garden Maintenance",
          ),
          ProjectType(
            id = 5678,
            name = "Park Cleanup",
          ),
          ProjectType(
            id = 9012,
            name = "Library Assistance",
          ),
        ),
      )

      val projectTypesDto = projectTypes.toDto()

      assertThat(projectTypesDto.projectTypes).hasSize(3)

      assertThat(projectTypesDto.projectTypes[0].id).isEqualTo(1234)
      assertThat(projectTypesDto.projectTypes[0].name).isEqualTo("Community Garden Maintenance")

      assertThat(projectTypesDto.projectTypes[1].id).isEqualTo(5678)
      assertThat(projectTypesDto.projectTypes[1].name).isEqualTo("Park Cleanup")

      assertThat(projectTypesDto.projectTypes[2].id).isEqualTo(9012)
      assertThat(projectTypesDto.projectTypes[2].name).isEqualTo("Library Assistance")
    }
  }

  @Nested
  inner class ProjectTypeMapper {
    @Test
    fun `should map ProjectType to DTO correctly`() {
      val projectType = ProjectType(
        id = 1234,
        name = "Community Garden Maintenance",
      )

      assertThat(projectType.toDto()).isEqualTo(
        ProjectTypeDto(
          id = 1234,
          name = "Community Garden Maintenance",
        ),
      )
    }
  }

  @Nested
  inner class ContactOutcomesMapper {

    @Test
    fun `should map empty ContactOutcomeEntity list correctly`() {
      val contactOutcomes = listOf<ContactOutcomeEntity>()
      val contactOutcomesDto = contactOutcomes.toDto()

      assertThat(contactOutcomesDto.contactOutcomes).isEmpty()
    }

    @Test
    fun `should map ContactOutcomeEntity list to DTO correctly`() {
      val contactOutcomes = listOf(
        ContactOutcomeEntity(
          id = UUID.fromString("b9391e9a-515a-4139-a956-20e0f0a129b9"),
          name = "Attended - Complied",
          code = "ATTC",
        ),
        ContactOutcomeEntity(
          id = UUID.fromString("f352472b-a277-4976-b8b4-224898d4a9b8"),
          name = "Attended - Failed to Comply",
          code = "AFTC",
        ),
        ContactOutcomeEntity(
          id = UUID.fromString("5e8f3124-d794-43b1-b844-df0bb95814dc"),
          name = "Attended - Sent Home (behaviour)",
          code = "ATSH",
        ),
      )

      val contactOutcomesDto = contactOutcomes.toDto()

      assertThat(contactOutcomesDto.contactOutcomes).hasSize(3)

      assertThat(contactOutcomesDto.contactOutcomes[0].id).isEqualTo(UUID.fromString("b9391e9a-515a-4139-a956-20e0f0a129b9"))
      assertThat(contactOutcomesDto.contactOutcomes[0].name).isEqualTo("Attended - Complied")
      assertThat(contactOutcomesDto.contactOutcomes[0].code).isEqualTo("ATTC")

      assertThat(contactOutcomesDto.contactOutcomes[1].id).isEqualTo(UUID.fromString("f352472b-a277-4976-b8b4-224898d4a9b8"))
      assertThat(contactOutcomesDto.contactOutcomes[1].name).isEqualTo("Attended - Failed to Comply")
      assertThat(contactOutcomesDto.contactOutcomes[1].code).isEqualTo("AFTC")

      assertThat(contactOutcomesDto.contactOutcomes[2].id).isEqualTo(UUID.fromString("5e8f3124-d794-43b1-b844-df0bb95814dc"))
      assertThat(contactOutcomesDto.contactOutcomes[2].name).isEqualTo("Attended - Sent Home (behaviour)")
      assertThat(contactOutcomesDto.contactOutcomes[2].code).isEqualTo("ATSH")
    }
  }

  @Nested
  inner class ContactOutcomeMapper {
    @Test
    fun `should map ContactOutcome to DTO correctly`() {
      val contactOutcome = ContactOutcomeEntity(
        id = UUID.fromString("b9391e9a-515a-4139-a956-20e0f0a129b9"),
        name = "Attended - Complied",
        code = "ATTC",
      )

      assertThat(contactOutcome.toDto()).isEqualTo(
        ContactOutcomeDto(
          id = UUID.fromString("b9391e9a-515a-4139-a956-20e0f0a129b9"),
          name = "Attended - Complied",
          code = "ATTC",
        ),
      )
    }
  }

  @Nested
  inner class EnforcementActionsMapper {
    @Test
    fun `should map empty EnforcementActions list correctly`() {
      val enforcementActions = EnforcementActions(emptyList())
      val result = enforcementActions.toDto()

      assertThat(result.enforcementActions).isEmpty()
    }

    @Test
    fun `should map EnforcementActions to DTO correctly`() {
      val enforcementActions = EnforcementActions(
        listOf(
          EnforcementAction(
            id = 2,
            name = "Breach / Recall Initiated",
          ),
          EnforcementAction(
            id = 24,
            name = "Breach Confirmation Sent",
          ),
        ),
      )

      val result = enforcementActions.toDto()

      assertThat(result.enforcementActions).hasSize(2)

      assertThat(result.enforcementActions[0].id).isEqualTo(2)
      assertThat(result.enforcementActions[0].name).isEqualTo("Breach / Recall Initiated")

      assertThat(result.enforcementActions[1].id).isEqualTo(24)
      assertThat(result.enforcementActions[1].name).isEqualTo("Breach Confirmation Sent")
    }
  }

  @Nested
  inner class EnforcementActionMapper {
    @Test
    fun `should map EnforcementAction to DTO correctly`() {
      val enforcementAction = EnforcementAction(
        id = 2,
        name = "Breach / Recall Initiated",
      )

      assertThat(enforcementAction.toDto()).isEqualTo(
        EnforcementActionDto(
          id = 2,
          name = "Breach / Recall Initiated",
        ),
      )
    }
  }
}
