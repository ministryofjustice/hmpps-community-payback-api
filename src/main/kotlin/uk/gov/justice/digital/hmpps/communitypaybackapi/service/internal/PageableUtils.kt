package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import org.springframework.data.domain.Pageable
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

fun Pageable.toHttpParams(): Map<String, String> = buildMap {
  put("page", if (isPaged) pageNumber.toString() else "0")
  put("size", if (isPaged) pageSize.toString() else Integer.MAX_VALUE.toString())
  if (sort.isSorted) {
    sort.forEach { order ->
      put("sort", "${order.property},${order.direction.name.lowercase()}")
    }
  }
}

fun Pageable.toMultiValueHttpParams(): MultiValueMap<String, String> {
  val map: MultiValueMap<String, String> = LinkedMultiValueMap()

  map.add("page", if (isPaged) pageNumber.toString() else "0")
  map.add("size", if (isPaged) pageSize.toString() else Integer.MAX_VALUE.toString())
  if (sort.isSorted) {
    sort.forEach { order ->
      map.add("sort", "${order.property},${order.direction.name.lowercase()}")
    }
  }

  return map
}
