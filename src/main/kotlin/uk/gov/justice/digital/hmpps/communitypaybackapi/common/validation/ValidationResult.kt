package uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation

data class ValidationResult(
  val errors: List<ValidationResultItem>,
  val warnings: List<ValidationResultItem>,
) {
  val isSuccess: Boolean
    get() = errors.isEmpty()

  val hasErrors: Boolean
    get() = errors.isNotEmpty()

  val hasWarnings: Boolean
    get() = warnings.isNotEmpty()

  companion object {
    fun success() = ValidationResult(emptyList(), emptyList())
  }
}

data class ValidationResultItem(val field: String, val code: String, val data: Map<String, Any>)
