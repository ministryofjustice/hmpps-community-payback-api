package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID
import kotlin.String

fun FormCacheEntity.Companion.valid() = FormCacheEntity(
  formId = UUID.randomUUID().toString(),
  formType = String.Companion.random(5),
  formData = String.random(),
)
