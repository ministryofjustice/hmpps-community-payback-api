package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseAccessItem

fun NDCaseAccess.Companion.valid(vararg items: NDCaseAccessItem) = NDCaseAccess(items.toList())

fun NDCaseAccessItem.Companion.unrestricted(crn: String) = NDCaseAccessItem(
  crn = crn,
  userExcluded = false,
  userRestricted = false,
  exclusionMessage = null,
  restrictionMessage = null,
)

fun NDCaseAccessItem.Companion.excluded(crn: String) = NDCaseAccessItem(
  crn = crn,
  userExcluded = true,
  userRestricted = false,
  exclusionMessage = "Test exclusion",
  restrictionMessage = null,
)

fun NDCaseAccessItem.Companion.restricted(crn: String) = NDCaseAccessItem(
  crn = crn,
  userExcluded = false,
  userRestricted = true,
  exclusionMessage = null,
  restrictionMessage = "Test restriction",
)

fun NDCaseAccessItem.Companion.excludedAndRestricted(crn: String) = NDCaseAccessItem(
  crn = crn,
  userExcluded = true,
  userRestricted = true,
  exclusionMessage = "Test exclusion",
  restrictionMessage = "Test restriction",
)
