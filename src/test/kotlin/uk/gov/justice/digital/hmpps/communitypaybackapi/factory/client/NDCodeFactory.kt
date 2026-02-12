package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDCode.Companion.valid() = NDCode(code = String.random(5))
