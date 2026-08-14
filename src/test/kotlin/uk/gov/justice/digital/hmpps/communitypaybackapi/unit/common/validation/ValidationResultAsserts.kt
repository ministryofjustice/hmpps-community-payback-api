package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation

import org.assertj.core.api.InstanceOfAssertFactories
import org.assertj.core.api.ObjectAssert
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem

fun ObjectAssert<ValidationResult>.hasWarning(expectedWarning: ValidationResultItem): ObjectAssert<ValidationResult> = this.describedAs("Validation result contains warning: %s", expectedWarning)
  .propertyIsTrue { it.hasWarnings }
  .propertyContainsItem(expectedWarning) { it.warnings }

fun ObjectAssert<ValidationResult>.hasWarnings(expectedWarnings: List<ValidationResultItem>): ObjectAssert<ValidationResult> = this.describedAs("Validation result has warnings: %s", expectedWarnings)
  .propertyIsTrue { it.hasWarnings }
  .propertyHasContents(expectedWarnings) { it.warnings }

fun ObjectAssert<ValidationResult>.hasNoWarnings(): ObjectAssert<ValidationResult> = this.describedAs("Validation result has no warnings")
  .propertyIsFalse { it.hasWarnings }
  .propertyIsEmpty { it.warnings }

fun ObjectAssert<ValidationResult>.hasError(expectedError: ValidationResultItem): ObjectAssert<ValidationResult> = this.describedAs("Validation result has error: %s", expectedError)
  .propertyIsFalse { it.isSuccess }
  .propertyIsTrue { it.hasErrors }
  .propertyContainsItem(expectedError) { it.errors }

fun ObjectAssert<ValidationResult>.hasErrors(expectedErrors: List<ValidationResultItem>): ObjectAssert<ValidationResult> = this.describedAs("Validation result has errors: %s", expectedErrors)
  .propertyIsFalse { it.isSuccess }
  .propertyIsTrue { it.hasErrors }
  .propertyHasContents(expectedErrors) { it.errors }

fun ObjectAssert<ValidationResult>.hasNoErrors(): ObjectAssert<ValidationResult> = this.describedAs("Validation result has no errors")
  .propertyIsTrue { it.isSuccess }
  .propertyIsFalse { it.hasErrors }
  .propertyIsEmpty { it.errors }

private fun ObjectAssert<ValidationResult>.propertyIsTrue(property: (ValidationResult) -> Boolean): ObjectAssert<ValidationResult> = this.also {
  this.extracting(property, InstanceOfAssertFactories.BOOLEAN).isTrue
}

private fun ObjectAssert<ValidationResult>.propertyIsFalse(property: (ValidationResult) -> Boolean): ObjectAssert<ValidationResult> = this.also {
  this.extracting(property, InstanceOfAssertFactories.BOOLEAN).isFalse
}

private fun ObjectAssert<ValidationResult>.propertyHasContents(expectedItems: List<ValidationResultItem>, property: (ValidationResult) -> List<ValidationResultItem>): ObjectAssert<ValidationResult> = this.also {
  this.extracting(property, InstanceOfAssertFactories.LIST).containsExactlyInAnyOrderElementsOf(expectedItems)
}

private fun ObjectAssert<ValidationResult>.propertyContainsItem(expectedItem: ValidationResultItem, property: (ValidationResult) -> List<ValidationResultItem>): ObjectAssert<ValidationResult> = this.also {
  this.extracting(property, InstanceOfAssertFactories.LIST).contains(expectedItem)
}

private fun ObjectAssert<ValidationResult>.propertyIsEmpty(property: (ValidationResult) -> List<ValidationResultItem>): ObjectAssert<ValidationResult> = this.also {
  this.extracting(property, InstanceOfAssertFactories.LIST).isEmpty()
}
