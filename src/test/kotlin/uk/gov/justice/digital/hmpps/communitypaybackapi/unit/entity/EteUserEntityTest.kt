package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.entity

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteUser

class EteUserEntityTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `should fail validation with invalid email format`() {
    val user = EteUser(
      crn = "X123456",
      email = "not-an-email",
    )

    val violations = validator.validate(user)

    assertThat(violations).isNotEmpty
    assertThat(violations.first().message).contains("must be a well-formed email address")
  }

  @Test
  fun `should pass validation with valid email format`() {
    val user = EteUser(
      crn = "X123456",
      email = "valid.user@justice.gov.uk",
    )

    val violations = validator.validate(user)

    assertThat(violations).isEmpty()
  }
}
