package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.reference.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ContactOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.EnforcementActionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.service.toDto
import java.util.UUID

class ReferenceMappersTest {

  @Nested
  inner class ProjectTypesMapper {

    @Test
    fun `should map empty ProjectTypes list correctly`() {
      val projectTypes = listOf<ProjectTypeEntity>()
      val projectTypesDto = projectTypes.toDto()

      assertThat(projectTypesDto.projectTypes).isEmpty()
    }

    @Test
    fun `should map ProjectTypes to DTO correctly`() {
      val projectTypes = listOf(
        ProjectTypeEntity(
          id = UUID.fromString("e68f2cd5-c6f2-4ed8-af66-cd9a46d5fe77"),
          name = "ETE - CFO",
          code = "ET3",
        ),
        ProjectTypeEntity(
          id = UUID.fromString("ea55e70e-c1ca-45b9-9001-18af7a907b25"),
          name = "Externally Supervised Placement",
          code = "ES",
        ),
        ProjectTypeEntity(
          id = UUID.fromString("b9391e9a-515a-4139-a956-20e0f0a129b9"),
          name = "Independent Working",
          code = "WH1",
        ),
      )

      val projectTypesDto = projectTypes.toDto()

      assertThat(projectTypesDto.projectTypes).hasSize(3)

      assertThat(projectTypesDto.projectTypes[0].id.toString()).isEqualTo("e68f2cd5-c6f2-4ed8-af66-cd9a46d5fe77")
      assertThat(projectTypesDto.projectTypes[0].name).isEqualTo("ETE - CFO")
      assertThat(projectTypesDto.projectTypes[0].code).isEqualTo("ET3")

      assertThat(projectTypesDto.projectTypes[1].id.toString()).isEqualTo("ea55e70e-c1ca-45b9-9001-18af7a907b25")
      assertThat(projectTypesDto.projectTypes[1].name).isEqualTo("Externally Supervised Placement")
      assertThat(projectTypesDto.projectTypes[1].code).isEqualTo("ES")

      assertThat(projectTypesDto.projectTypes[2].id.toString()).isEqualTo("b9391e9a-515a-4139-a956-20e0f0a129b9")
      assertThat(projectTypesDto.projectTypes[2].name).isEqualTo("Independent Working")
      assertThat(projectTypesDto.projectTypes[2].code).isEqualTo("WH1")
    }
  }

  @Nested
  inner class ProjectTypeMapper {
    @Test
    fun `should map ProjectType to DTO correctly`() {
      val projectType = ProjectTypeEntity(
        id = UUID.fromString("ea55e70e-c1ca-45b9-9001-18af7a907b25"),
        name = "Externally Supervised Placement",
        code = "ES",
      )

      assertThat(projectType.toDto()).isEqualTo(
        ProjectTypeDto(
          id = UUID.fromString("ea55e70e-c1ca-45b9-9001-18af7a907b25"),
          name = "Externally Supervised Placement",
          code = "ES",
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
          enforceable = false,
        ),
        ContactOutcomeEntity(
          id = UUID.fromString("f352472b-a277-4976-b8b4-224898d4a9b8"),
          name = "Attended - Failed to Comply",
          code = "AFTC",
          enforceable = true,
        ),
        ContactOutcomeEntity(
          id = UUID.fromString("5e8f3124-d794-43b1-b844-df0bb95814dc"),
          name = "Attended - Sent Home (behaviour)",
          code = "ATSH",
          enforceable = true,
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
        enforceable = false,
      )

      assertThat(contactOutcome.toDto()).isEqualTo(
        ContactOutcomeDto(
          id = UUID.fromString("b9391e9a-515a-4139-a956-20e0f0a129b9"),
          name = "Attended - Complied",
          code = "ATTC",
          enforceable = false,
        ),
      )
    }
  }

  @Nested
  inner class EnforcementActionsMapper {
    @Test
    fun `should map empty EnforcementActionsEntity list correctly`() {
      val enforcementActions = listOf<EnforcementActionEntity>()
      val result = enforcementActions.toDto()

      assertThat(result.enforcementActions).isEmpty()
    }

    @Test
    fun `should map EnforcementActionEntity to DTO correctly`() {
      val enforcementActions = listOf(
        EnforcementActionEntity(
          id = UUID.fromString("070cfb0a-6fc2-44cb-994f-25ec4839ef60"),
          name = "Refer to Offender Manager",
          code = "ROM",
        ),
        EnforcementActionEntity(
          id = UUID.fromString("068dbac5-fe96-4d84-b621-84f3af83ac28"),
          name = "Breach / Recall Initiated",
          code = "IBR",
        ),
      )

      val result = enforcementActions.toDto()

      assertThat(result.enforcementActions).hasSize(2)

      assertThat(result.enforcementActions[0].id).isEqualTo(UUID.fromString("070cfb0a-6fc2-44cb-994f-25ec4839ef60"))
      assertThat(result.enforcementActions[0].name).isEqualTo("Refer to Offender Manager")
      assertThat(result.enforcementActions[0].code).isEqualTo("ROM")

      assertThat(result.enforcementActions[1].id).isEqualTo(UUID.fromString("068dbac5-fe96-4d84-b621-84f3af83ac28"))
      assertThat(result.enforcementActions[1].name).isEqualTo("Breach / Recall Initiated")
      assertThat(result.enforcementActions[1].code).isEqualTo("IBR")
    }
  }

  @Nested
  inner class EnforcementActionMapper {
    @Test
    fun `should map EnforcementAction to DTO correctly`() {
      val enforcementAction = EnforcementActionEntity(
        id = UUID.fromString("070cfb0a-6fc2-44cb-994f-25ec4839ef60"),
        name = "Refer to Offender Manager",
        code = "ROM",
      )

      assertThat(enforcementAction.toDto()).isEqualTo(
        EnforcementActionDto(
          id = UUID.fromString("070cfb0a-6fc2-44cb-994f-25ec4839ef60"),
          name = "Refer to Offender Manager",
          code = "ROM",
        ),
      )
    }
  }
}
