package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Supervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

class SupervisorMappersTest {

  @Nested
  inner class SupervisorToSupervisorDto {

    @Test
    fun success() {
      val result = Supervisor(
        code = "SC1",
        isUnpaidWorkTeamMember = true,
      ).toDto()

      assertThat(result.code).isEqualTo("SC1")
      assertThat(result.isUnpaidWorkTeamMember).isTrue()
    }
  }
}
