package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.CourseCompletionVisibilityConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.passed
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DatabasePurgeUtils
import java.time.OffsetDateTime

@TestPropertySource(
  properties = [
    "course.completions.visibility.show-all=false",
    "course.completions.visibility.offices[0].name=Office A",
    "course.completions.visibility.offices[0].from=2026-10-01T00:00:00Z",
    "course.completions.visibility.offices[1].name=Office B",
    "course.completions.visibility.offices[1].from=2026-11-01T00:00:00Z",
  ],
)
class CourseCompletionVisibilityIT : IntegrationTestBase() {
  @Autowired
  lateinit var repository: EteCourseCompletionEventEntityRepository

  @Autowired
  lateinit var visibility: CourseCompletionVisibilityConfig

  @Autowired
  lateinit var databasePurgeUtils: DatabasePurgeUtils

  private val october = OffsetDateTime.parse("2026-10-01T00:00:00Z")
  private val november = OffsetDateTime.parse("2026-11-01T00:00:00Z")
  private lateinit var events: List<EteCourseCompletionEventEntity>

  @BeforeEach
  fun setUp() {
    databasePurgeUtils.deleteAllEteData()
    events = repository.saveAll(
      listOf(
        EteCourseCompletionEventEntity.passed(ctx).copy(office = "Office A", completionDateTime = october.minusSeconds(1)),
        EteCourseCompletionEventEntity.passed(ctx).copy(office = "Office A", completionDateTime = october),
        EteCourseCompletionEventEntity.passed(ctx).copy(office = "Office B", completionDateTime = november.minusSeconds(1)),
        EteCourseCompletionEventEntity.passed(ctx).copy(office = "Office B", completionDateTime = november),
        EteCourseCompletionEventEntity.passed(ctx).copy(office = "Office C", completionDateTime = november.plusDays(1)),
      ),
    )
  }

  @Test
  fun `configured rules apply to the endpoint before pagination and counting`() {
    webTestClient.get()
      .uri("/admin/providers/${events.first().pdu.providerCode}/course-completions?size=1&sort=completionDateTime,asc")
      .addAdminUiAuthHeader()
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.page.totalElements").isEqualTo(2)
      .jsonPath("$.page.totalPages").isEqualTo(2)
      .jsonPath("$.content.length()").isEqualTo(1)
      .jsonPath("$.content[0].id").isEqualTo(events[1].id.toString())
  }

  @Test
  fun `office search intersects configured rules`() {
    getEvents("Office B")
      .jsonPath("$.page.totalElements").isEqualTo(1)
      .jsonPath("$.content.length()").isEqualTo(1)
      .jsonPath("$.content[0].id").isEqualTo(events[3].id.toString())

    getEvents("Office C")
      .jsonPath("$.page.totalElements").isEqualTo(0)
      .jsonPath("$.content.length()").isEqualTo(0)
  }

  @Test
  fun `show all bypasses rules and still respects requested offices`() {
    val originalShowAll = visibility.showAll
    try {
      visibility.showAll = true
      getEvents()
        .jsonPath("$.page.totalElements").isEqualTo(5)
        .jsonPath("$.content.length()").isEqualTo(5)

      getEvents("Office A")
        .jsonPath("$.page.totalElements").isEqualTo(2)
        .jsonPath("$.content.length()").isEqualTo(2)
        .jsonPath("$.content[0].id").isEqualTo(events[0].id.toString())
        .jsonPath("$.content[1].id").isEqualTo(events[1].id.toString())
    } finally {
      visibility.showAll = originalShowAll
    }
  }

  @Test
  fun `restricted mode with no offices shows no completions`() {
    val originalOffices = visibility.offices
    try {
      visibility.offices = emptyList()
      getEvents()
        .jsonPath("$.page.totalElements").isEqualTo(0)
        .jsonPath("$.content.length()").isEqualTo(0)
    } finally {
      visibility.offices = originalOffices
    }
  }

  private fun getEvents(office: String? = null) = webTestClient.get()
    .uri { builder ->
      builder.path("/admin/providers/${events.first().pdu.providerCode}/course-completions")
        .queryParam("sort", "completionDateTime,asc")
        .apply { office?.let { queryParam("office", it) } }
        .build()
    }
    .addAdminUiAuthHeader()
    .exchange()
    .expectStatus().isOk
    .expectBody()
}
