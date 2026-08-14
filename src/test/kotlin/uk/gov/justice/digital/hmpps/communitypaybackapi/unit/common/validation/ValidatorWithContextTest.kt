package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.daysUntil
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidatorWithContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.time.LocalDate

class ValidatorWithContextTest {

  class TestValidationContext : ValidationContext<TestData> {
    var maxAttendees: Int = 0
  }

  class TestService {
    @Suppress("unused", "detekt:FunctionOnlyReturningConstant")
    fun getMaxAttendees(startDate: LocalDate, endDate: LocalDate): Int = 42
  }

  class TestValidator(
    private val testService: TestService,
  ) : ValidatorWithContext<TestData, TestValidationContext>() {

    override fun configureContext(value: TestData, ctx: TestValidationContext): TestValidationContext {
      ctx.maxAttendees = testService.getMaxAttendees(value.startDate, value.endDate)

      return ctx
    }

    override fun configureRules() {
      rule {
        expect { value -> value.notes?.let { it.length <= 4000 } }
        otherwise {
          this isError "NOTES_TOO_LONG"
          field = "notes"
          data {
            "length" to { value -> value.notes!!.length }
            "maxLength" to 4000
          }
        }
      }

      rule {
        expect { value -> value.startDate.isBefore(value.endDate) }
        otherwise {
          this isError "START_DATE_AFTER_END_DATE"
          field = "startDate"
          data {
            "startDate" to { value -> value.startDate }
            "endDate" to { value -> value.endDate }
          }
        }
      }

      rule {
        expect { value, ctx -> value.numberOfAttendees <= ctx.maxAttendees }
        otherwise {
          this isWarning "TOO_MANY_ATTENDEES"
          field = "numberOfAttendees"
          data {
            "requestedAttendees" to { value -> value.numberOfAttendees }
            "maxAttendees" to { _, ctx -> ctx.maxAttendees }
          }
        }
      }

      rule {
        assume { value -> value.isWeeklyEvent }
        assume { value -> value.startDate.isAfter(LocalDate.of(2020, 1, 1)) }
        expect { value -> value.startDate.plusDays(7).isAfter(value.endDate) }
        otherwise {
          this isWarning "OVERLAPPING_EVENTS"
          field = "endDate"
          data {
            "numberOfDays" to { value -> daysUntil(value.startDate, value.endDate) }
            "threshold" to 7L
          }
        }
      }
    }
  }

  @Nested
  inner class Validate {
    @Test
    fun `should report successful validation when all rules are satisfied`() {
      val validator = TestValidator(TestService())
      val value = TestData.valid()
      val ctx = TestValidationContext()

      val result = validator.validate(value, ctx)

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `should report warnings when warning rules are not satisfied`() {
      val validator = TestValidator(TestService())
      val value = TestData.valid().copy(isWeeklyEvent = true)
        .copy(startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(14), numberOfAttendees = 55)
      val ctx = TestValidationContext()
      val expectedWarnings = listOf(
        ValidationResultItem(
          field = "numberOfAttendees",
          code = "TOO_MANY_ATTENDEES",
          data = mapOf("requestedAttendees" to 55, "maxAttendees" to 42),
        ),
        ValidationResultItem(
          field = "endDate",
          code = "OVERLAPPING_EVENTS",
          data = mapOf("numberOfDays" to 14L, "threshold" to 7L),
        ),
      )

      val result = validator.validate(value, ctx)

      assertThat(result).hasNoErrors()
      assertThat(result).hasWarnings(expectedWarnings)
    }

    @Test
    fun `should report errors when error rules are not satisfied`() {
      val validator = TestValidator(TestService())
      val value = TestData.valid()
        .copy(startDate = LocalDate.now().plusDays(1), endDate = LocalDate.now(), notes = String.random(4001))
      val ctx = TestValidationContext()
      val expectedErrors = listOf(
        ValidationResultItem(
          field = "notes",
          code = "NOTES_TOO_LONG",
          data = mapOf("length" to 4001, "maxLength" to 4000),
        ),
        ValidationResultItem(
          field = "startDate",
          code = "START_DATE_AFTER_END_DATE",
          data = mapOf("startDate" to value.startDate, "endDate" to value.endDate),
        ),
      )

      val result = validator.validate(value, ctx)

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `should ignore rules when assumptions are not satisfied`() {
      val validator = TestValidator(TestService())
      val value = TestData.valid()
        .copy(startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(14), isWeeklyEvent = false)
      val ctx = TestValidationContext()

      val result = validator.validate(value, ctx)

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class ConfigureRules {
    @Test
    fun `throws an IllegalStateException if a rule is missing an expect block`() {
      val exception = assertThrows<IllegalStateException> {
        object : ValidatorWithContext<TestData, TestValidationContext>() {
          override fun configureContext(value: TestData, ctx: TestValidationContext): TestValidationContext = null!!

          override fun configureRules() {
            rule {
              otherwise {
                this isError "MISSING_EXPECT"
                field = String.random()
              }
            }
          }
        }
      }

      assertThat(exception).hasMessage("The validation rule needs an `expect` block")
    }

    @Test
    fun `throws an IllegalStateException if a rule is missing an otherwise block`() {
      val exception = assertThrows<IllegalStateException> {
        object : ValidatorWithContext<TestData, TestValidationContext>() {
          override fun configureContext(value: TestData, ctx: TestValidationContext): TestValidationContext = null!!

          override fun configureRules() {
            rule {
              expect { value -> value.notes != null }
            }
          }
        }
      }

      assertThat(exception).hasMessage("The validation rule needs an `otherwise` block")
    }

    @Test
    fun `throws an IllegalStateException if an otherwise block is missing the field`() {
      val exception = assertThrows<IllegalStateException> {
        object : ValidatorWithContext<TestData, TestValidationContext>() {
          override fun configureContext(value: TestData, ctx: TestValidationContext): TestValidationContext = null!!

          override fun configureRules() {
            rule {
              expect { value -> value.notes != null }
              otherwise {
                this isError "field is missing"
                data {
                  "foo" to "bar"
                }
              }
            }
          }
        }
      }

      assertThat(exception).hasMessage("The validation rule needs to set the field in the `otherwise` block")
    }

    @Test
    fun `throws an IllegalStateException if an otherwise block is missing the type and code`() {
      val exception = assertThrows<IllegalStateException> {
        object : ValidatorWithContext<TestData, TestValidationContext>() {
          override fun configureContext(value: TestData, ctx: TestValidationContext): TestValidationContext = null!!

          override fun configureRules() {
            rule {
              expect { value -> value.notes != null }
              otherwise {
                field = "notes"
                data {
                  "foo" to "bar"
                }
              }
            }
          }
        }
      }

      assertThat(exception).hasMessage("The validation rule needs to set the rule type and code using `isError` or `isWarning` in the `otherwise` block")
    }

    @Test
    fun `does not throw an exception if an otherwise block is missing the optional data block`() {
      assertDoesNotThrow {
        object : ValidatorWithContext<TestData, TestValidationContext>() {
          override fun configureContext(value: TestData, ctx: TestValidationContext): TestValidationContext = null!!

          override fun configureRules() {
            rule {
              expect { value -> value.notes != null }
              otherwise {
                this isError "extra data is missing"
                field = "notes"
              }
            }
          }
        }
      }
    }
  }
}
