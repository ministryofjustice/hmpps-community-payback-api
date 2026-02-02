package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["community-payback.auto-seed.enabled"], havingValue = "true")
class AutoSeedService(
  val environmentService: EnvironmentService,
) {

  @PostConstruct
  fun onStartup() {
    if (environmentService.isNotATestEnvironment()) {
      error("auto seed should not be enabled outside of test environments")
    }

    // currently autoseeding does nothing.
  }
}
