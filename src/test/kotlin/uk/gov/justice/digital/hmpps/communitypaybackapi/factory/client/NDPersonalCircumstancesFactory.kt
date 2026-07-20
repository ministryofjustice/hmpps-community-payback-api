package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPersonalCircumstances

fun NDPersonalCircumstances.Companion.valid() = NDPersonalCircumstances(
  type = NDCodeDescription.valid(),
  subType = NDCodeDescription.valid(),
)

fun NDPersonalCircumstances.Companion.valid(typeCode: String, subTypeCode: String?) = NDPersonalCircumstances(
  type = NDCodeDescription.valid().copy(code = typeCode),
  subType = subTypeCode?.let { NDCodeDescription.valid().copy(code = it) },
)
