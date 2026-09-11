package uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem

class ValidationRule<T, TContext : ValidationContext<T>>(
  private val assume: List<(value: T, ctx: TContext) -> Boolean?>,
  private val expect: (value: T, ctx: TContext) -> Boolean?,
  private val type: Type,
  private val field: String,
  private val code: String,
  private val getData: (value: T, ctx: TContext) -> Map<String, Any>,
) {
  fun evaluate(value: T, ctx: TContext): Result {
    if (this.assume.any { it(value, ctx) == false }) return Result.Inconclusive
    if (this.expect(value, ctx) != false) return Result.Success

    val result = ValidationResultItem(
      field = field,
      code = code,
      data = getData(value, ctx),
    )

    return when (type) {
      Type.ERROR -> Result.Error(result)
      Type.WARNING -> Result.Warning(result)
    }
  }

  enum class Type {
    WARNING,
    ERROR,
  }

  sealed interface Result {
    object Inconclusive : Result
    object Success : Result
    data class Error(val item: ValidationResultItem) : Result
    data class Warning(val item: ValidationResultItem) : Result
  }
}
