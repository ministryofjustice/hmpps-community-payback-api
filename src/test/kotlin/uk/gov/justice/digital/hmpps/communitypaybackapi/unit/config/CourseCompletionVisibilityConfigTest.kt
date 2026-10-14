package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.CourseCompletionVisibilityConfig

class CourseCompletionVisibilityConfigTest {
  @Test
  fun `restrictions target only prod by default`() {
    val config = CourseCompletionVisibilityConfig().apply { showAll = false }
    assertThat(config.appliesTo(arrayOf("prod"))).isTrue()
    listOf("dev", "test", "preprod", "localdev", "integrationtest").forEach {
      assertThat(config.appliesTo(arrayOf(it))).isFalse()
    }
    assertThat(config.appliesTo(emptyArray())).isFalse()
  }

  @Test
  fun `configured environments match any active profile ignoring case`() {
    val config = CourseCompletionVisibilityConfig().apply {
      showAll = false
      environments = listOf("preprod", "test")
    }
    assertThat(config.appliesTo(arrayOf("metrics", "PREPROD"))).isTrue()
    assertThat(config.appliesTo(arrayOf("test"))).isTrue()
    assertThat(config.appliesTo(arrayOf("prod"))).isFalse()
  }

  @Test
  fun `show all overrides a matching environment`() {
    assertThat(CourseCompletionVisibilityConfig().appliesTo(arrayOf("prod"))).isFalse()
  }

  @Test
  fun `empty environments disable restrictions`() {
    val config = CourseCompletionVisibilityConfig().apply {
      showAll = false
      environments = emptyList()
    }
    assertThat(config.appliesTo(arrayOf("prod"))).isFalse()
  }
}
