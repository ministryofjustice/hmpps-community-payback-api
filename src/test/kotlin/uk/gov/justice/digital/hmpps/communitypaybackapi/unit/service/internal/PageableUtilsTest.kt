package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.toHttpParams

class PageableUtilsTest {

  @Test
  fun `toHttpParams should return map with page and size when no sort`() {
    val pageable = PageRequest.of(0, 10)

    val result = pageable.toHttpParams()

    assertThat(result).containsEntry("page", "0")
    assertThat(result).containsEntry("size", "10")
    assertThat(result).hasSize(2)
  }

  @Test
  fun `toHttpParams should return map with single sort ascending`() {
    val pageable = PageRequest.of(1, 20, Sort.by(Sort.Direction.ASC, "name"))

    val result = pageable.toHttpParams()

    assertThat(result).containsEntry("page", "1")
    assertThat(result).containsEntry("size", "20")
    assertThat(result).containsEntry("sort", "name,asc")
    assertThat(result).hasSize(3)
  }

  @Test
  fun `toHttpParams should return map with single sort descending`() {
    val pageable = PageRequest.of(2, 15, Sort.by(Sort.Direction.DESC, "createdDate"))

    val result = pageable.toHttpParams()

    assertThat(result).containsEntry("page", "2")
    assertThat(result).containsEntry("size", "15")
    assertThat(result).containsEntry("sort", "createdDate,desc")
    assertThat(result).hasSize(3)
  }

  @Test
  fun `toHttpParams should return map with last sort when multiple sorts`() {
    val pageable = PageRequest.of(
      3,
      25,
      Sort.by(Sort.Direction.DESC, "priority")
        .and(Sort.by(Sort.Direction.ASC, "name")),
    )

    val result = pageable.toHttpParams()

    assertThat(result).containsEntry("page", "3")
    assertThat(result).containsEntry("size", "25")
    assertThat(result).containsEntry("sort", "name,asc")
    assertThat(result).hasSize(3)
  }

  @Test
  fun `toHttpParams should handle unsorted pageable`() {
    val pageable = PageRequest.of(5, 50, Sort.unsorted())

    val result = pageable.toHttpParams()

    assertThat(result).containsEntry("page", "5")
    assertThat(result).containsEntry("size", "50")
    assertThat(result).hasSize(2)
  }

  @Test
  fun `toHttpParams should convert page number correctly`() {
    val pageable = PageRequest.of(10, 100)

    val result = pageable.toHttpParams()

    assertThat(result["page"]).isEqualTo("10")
    assertThat(result["size"]).isEqualTo("100")
  }

  @Test
  fun `toHttpParams should convert direction to lowercase`() {
    val pageableAsc = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "field"))
    val pageableDesc = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "field"))

    val resultAsc = pageableAsc.toHttpParams()
    val resultDesc = pageableDesc.toHttpParams()

    assertThat(resultAsc["sort"]).isEqualTo("field,asc")
    assertThat(resultDesc["sort"]).isEqualTo("field,desc")
  }
}
