package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.OffsetDateTime

@Validated
@Component
@ConfigurationProperties("course.completions.visibility")
class CourseCompletionVisibilityConfig {
  var showAll: Boolean = true
  var environments: List<String> = listOf("prod")

  fun appliesTo(activeProfiles: Array<String>): Boolean = !showAll && environments.any { configured -> activeProfiles.any { it.equals(configured, ignoreCase = true) } }

  @field:Valid
  var offices: List<Office> = emptyList()

  class Office {
    @field:NotBlank
    var name: String = ""

    @field:NotNull
    var from: OffsetDateTime? = null
  }
}
