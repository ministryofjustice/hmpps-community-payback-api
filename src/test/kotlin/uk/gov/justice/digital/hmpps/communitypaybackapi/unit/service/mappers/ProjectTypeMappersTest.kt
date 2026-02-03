package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDProjectTypeCodes

class ProjectTypeMappersTest {

  @Nested
  inner class ToNdProjectTypes {

    @Test
    fun `group mappings`() {
      assertThat(ProjectTypeGroupDto.GROUP.toNDProjectTypeCodes()).containsExactlyInAnyOrder(
        "PL",
        "NP1",
        "NP2",
      )
    }

    @Test
    fun `individual mappings`() {
      assertThat(ProjectTypeGroupDto.INDIVIDUAL.toNDProjectTypeCodes()).containsExactlyInAnyOrder(
        "ES",
        "ICP",
        "PIP2",
        "PSP",
      )
    }
  }
}
