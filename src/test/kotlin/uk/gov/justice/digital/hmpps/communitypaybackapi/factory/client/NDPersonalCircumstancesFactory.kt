package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPersonalCircumstances
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomFutureOffsetDateTime
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomPastOffsetDateTime

fun NDPersonalCircumstances.Companion.valid() = NDPersonalCircumstances(
  type = NDCodeDescription.valid(),
  subType = NDCodeDescription.valid(),
  startDate = randomPastOffsetDateTime(),
  endDate = randomFutureOffsetDateTime(),
  verified = Boolean.random(),
  notes = String.random(length = 100),
)

fun NDPersonalCircumstances.Companion.valid(typeCode: String, subTypeCode: String?) = NDPersonalCircumstances.valid().copy(
  type = NDCodeDescription.valid().copy(code = typeCode),
  subType = subTypeCode?.let { NDCodeDescription.valid().copy(code = it) },
)
