package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntity
import java.util.UUID
import kotlin.String

fun FormCacheEntity.Companion.valid() = FormCacheEntity(
  formId = UUID.randomUUID().toString(),
  formType = String.random(5),
  formData = String.random(),
)
