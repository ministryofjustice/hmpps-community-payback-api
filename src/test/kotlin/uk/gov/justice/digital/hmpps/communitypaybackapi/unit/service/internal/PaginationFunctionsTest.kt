package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.PageDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.toQueryTriplet

class PaginationFunctionsTest {

  @Test
  fun `toQueryTriplet should return query parameters with page and size when no sort`() {
    val pageable = PageRequest.of(0, 10)

    val result = pageable.toQueryTriplet()

    assertThat(result.page).isEqualTo(0)
    assertThat(result.size).isEqualTo(10)
    assertThat(result.sort).isEmpty()
  }

  @Test
  fun `toQueryTriplet should return query parameters with single sort ascending`() {
    val pageable = PageRequest.of(1, 20, Sort.by(Sort.Direction.ASC, "name"))

    val result = pageable.toQueryTriplet()

    assertThat(result.page).isEqualTo(1)
    assertThat(result.size).isEqualTo(20)
    assertThat(result.sort).isEqualTo(listOf("name,asc"))
  }

  @Test
  fun `toQueryTriplet should return query parameters with single sort descending`() {
    val pageable = PageRequest.of(2, 15, Sort.by(Sort.Direction.DESC, "createdDate"))

    val result = pageable.toQueryTriplet()

    assertThat(result.page).isEqualTo(2)
    assertThat(result.size).isEqualTo(15)
    assertThat(result.sort).isEqualTo(listOf("createdDate,desc"))
  }

  @Test
  fun `toQueryTriplet should return query parameters with multiple sorts`() {
    val pageable = PageRequest.of(
      3,
      25,
      Sort.by(Sort.Direction.DESC, "priority")
        .and(Sort.by(Sort.Direction.ASC, "name")),
    )

    val result = pageable.toQueryTriplet()

    assertThat(result.page).isEqualTo(3)
    assertThat(result.size).isEqualTo(25)
    assertThat(result.sort).isEqualTo(listOf("priority,desc", "name,asc"))
  }

  @Test
  fun `toQueryTriplet should handle unsorted pageable`() {
    val pageable = PageRequest.of(5, 50, Sort.unsorted())

    val result = pageable.toQueryTriplet()

    assertThat(result.page).isEqualTo(5)
    assertThat(result.size).isEqualTo(50)
    assertThat(result.sort).isEmpty()
  }

  @Test
  fun `QueryParameters data class should hold correct values`() {
    val pageDetails = PageDetails(
      page = 1,
      size = 100,
      sort = listOf("field1,asc", "field2,desc"),
    )

    assertThat(pageDetails.page).isEqualTo(1)
    assertThat(pageDetails.size).isEqualTo(100)
    assertThat(pageDetails.sort).hasSize(2)
    assertThat(pageDetails.sort[0]).isEqualTo("field1,asc")
    assertThat(pageDetails.sort[1]).isEqualTo("field2,desc")
  }
}
