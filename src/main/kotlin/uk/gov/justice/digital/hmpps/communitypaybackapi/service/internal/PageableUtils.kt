package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import org.springframework.data.domain.Pageable

fun Pageable.toHttpParams(): Map<String, String> = buildMap {
  put("page", if (isPaged) pageNumber.toString() else "0")
  put("size", if (isPaged) pageSize.toString() else Integer.MAX_VALUE.toString())
  if (sort.isSorted) {
    sort.forEach { order ->
      put("sort", "${order.property},${order.direction.name.lowercase()}")
    }
  }
}
