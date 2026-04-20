package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse

fun <T> PageResponse.Companion.empty() = PageResponse<T>(content = emptyList(), page = PageResponse.PageMeta(50, 0, 0, 0))
