package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import org.springframework.data.domain.Pageable

fun Pageable.toHttpParams(): Map<String, String> = buildMap {
  put("page", pageNumber.toString())
  put("size", pageSize.toString())
  if (sort.isSorted) {
    sort.forEach { order ->
      put("sort", "${order.property},${order.direction.name.lowercase()}")
    }
  }
}
