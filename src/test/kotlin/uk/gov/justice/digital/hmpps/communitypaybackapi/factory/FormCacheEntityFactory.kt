package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntity

fun FormCacheEntity.Companion.valid() = FormCacheEntity(
  formId = String.random(),
  formType = String.random(),
  formData = String.random(),
)
