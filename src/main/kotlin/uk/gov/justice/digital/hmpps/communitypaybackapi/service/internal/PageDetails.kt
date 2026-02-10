package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import org.springframework.data.domain.Pageable

data class PageDetails(val page: Int, val size: Int, val sort: List<String>)

fun Pageable.toQueryTriplet(): PageDetails {
  val sortValues = if (sort.isSorted) {
    sort.map { "${it.property},${it.direction.name.lowercase()}" }.toList()
  } else {
    emptyList()
  }

  return PageDetails(pageNumber, pageSize, sortValues)
}
