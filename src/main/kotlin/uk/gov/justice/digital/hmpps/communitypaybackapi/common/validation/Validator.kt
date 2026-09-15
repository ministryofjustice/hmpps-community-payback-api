package uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.internal.ValidationRule
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.internal.ValidationRuleBuilder

abstract class ValidatorWithContext<T : Any, TContext : ValidationContext<T>> {
  private val rules = mutableListOf<ValidationRule<T, TContext>>()

  init {
    configureRules()
  }

  protected abstract fun configureRules()
  protected abstract fun configureContext(value: T, ctx: TContext): TContext

  protected fun rule(init: ValidationRuleBuilder<T, TContext>.() -> Unit) {
    this.rules.add(ValidationRuleBuilder<T, TContext>().apply(init).build())
  }

  fun validate(value: T, ctx: TContext): ValidationResult {
    val errors = mutableListOf<ValidationResultItem>()
    val warnings = mutableListOf<ValidationResultItem>()

    val ctx = configureContext(value, ctx)

    for (rule in rules) {
      when (val result = rule.evaluate(value, ctx)) {
        is ValidationRule.Result.Inconclusive, ValidationRule.Result.Success -> {}
        is ValidationRule.Result.Error -> errors.add(result.item)
        is ValidationRule.Result.Warning -> warnings.add(result.item)
      }
    }

    return ValidationResult(errors, warnings)
  }
}

abstract class Validator<T : Any> : ValidatorWithContext<T, ValidationContext.EmptyContext>() {
  final override fun configureContext(value: T, ctx: ValidationContext.EmptyContext) = ValidationContext.EmptyContext

  fun validate(value: T) = validate(value, ValidationContext.EmptyContext)
}
