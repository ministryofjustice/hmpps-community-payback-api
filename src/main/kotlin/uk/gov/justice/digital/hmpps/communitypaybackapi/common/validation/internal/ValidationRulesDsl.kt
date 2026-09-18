package uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.internal.ValidationRule.Type

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ValidationRulesDsl

@ValidationRulesDsl
class ValidationRuleBuilder<T, TContext : ValidationContext<T>> {
  private var assume: MutableList<((value: T, ctx: TContext) -> Boolean?)> = mutableListOf()
  private var expect: ((value: T, ctx: TContext) -> Boolean?)? = null
  private var type: Type? = null
  private var field: String? = null
  private var code: String? = null
  private var data: MutableMap<String, (value: T, ctx: TContext) -> Any> = mutableMapOf()

  private var expectConfigurationCount: Int = 0
  private var otherwiseConfigurationCount: Int = 0
  private var typeAndCodeConfigurationCount: Int = 0
  private var fieldConfigurationCount: Int = 0

  fun assume(assume: @ValidationRulesDsl (value: T, ctx: TContext) -> Boolean?) {
    this.assume.add(assume)
  }

  fun assume(assume: @ValidationRulesDsl (value: T) -> Boolean?) {
    this.assume.add({ value, _ -> assume(value) })
  }

  fun expect(expect: @ValidationRulesDsl (value: T, ctx: TContext) -> Boolean?) {
    this.expectConfigurationCount++
    this.expect = expect
  }

  fun expect(expect: @ValidationRulesDsl (value: T) -> Boolean?) {
    this.expectConfigurationCount++
    this.expect = { value, _ -> expect(value) }
  }

  fun otherwise(init: @ValidationRulesDsl ValidationOtherwiseBuilder.() -> Unit) {
    this.otherwiseConfigurationCount++
    ValidationOtherwiseBuilder().apply(init)
  }

  fun build(): ValidationRule<T, TContext> {
    checkNotNull(expect) { "The validation rule needs an `expect` block" }
    check(expectConfigurationCount == 1) { "The validation rule only allows exactly one `expect` block" }
    check(otherwiseConfigurationCount > 0) { "The validation rule needs an `otherwise` block" }
    check(otherwiseConfigurationCount == 1) { "The validation rule only allows exactly one `otherwise` block" }
    checkNotNull(field) { "The validation rule needs to set the field in the `otherwise` block" }
    check(fieldConfigurationCount == 1) { "The validation rule only allows setting the field once" }
    checkNotNull(type) { "The validation rule needs to set the rule type and code using `isError` or `isWarning` in the `otherwise` block" }
    checkNotNull(code) { "The validation rule needs to set the rule type and code using `isError` or `isWarning` in the `otherwise` block" }
    check(typeAndCodeConfigurationCount == 1) { "The validation rule only allows setting the rule type and code once" }

    return ValidationRule(
      assume = assume,
      expect = expect!!,
      type = type!!,
      field = field!!,
      code = code!!,
      getData = { value, ctx -> data.mapValues { it.value(value, ctx) } },
    )
  }

  @ValidationRulesDsl
  inner class ValidationOtherwiseBuilder {
    infix fun isError(code: String) {
      this@ValidationRuleBuilder.typeAndCodeConfigurationCount++
      this@ValidationRuleBuilder.type = Type.ERROR
      this@ValidationRuleBuilder.code = code
    }

    infix fun isWarning(code: String) {
      this@ValidationRuleBuilder.typeAndCodeConfigurationCount++
      this@ValidationRuleBuilder.type = Type.WARNING
      this@ValidationRuleBuilder.code = code
    }

    var field: String?
      get() {
        return this@ValidationRuleBuilder.field
      }
      set(value) {
        this@ValidationRuleBuilder.fieldConfigurationCount++
        this@ValidationRuleBuilder.field = value
      }

    fun data(init: @ValidationRulesDsl ValidationExtraDataBuilder.() -> Unit) {
      this@ValidationRuleBuilder.ValidationExtraDataBuilder().apply(init)
    }
  }

  @ValidationRulesDsl
  inner class ValidationExtraDataBuilder {
    infix fun String.to(init: @ValidationRulesDsl (value: T, ctx: TContext) -> Any) {
      this@ValidationRuleBuilder.data[this] = init
    }

    infix fun String.to(init: @ValidationRulesDsl (value: T) -> Any) {
      this@ValidationRuleBuilder.data[this] = { value, _ -> init(value) }
    }

    infix fun String.to(value: Any) {
      this@ValidationRuleBuilder.data[this] = { _, _ -> value }
    }
  }
}
