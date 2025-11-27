package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionIdDto
import java.time.LocalDate

@Service
@ConditionalOnProperty(name = ["community-payback.auto-seed.enabled"], havingValue = "true")
class AutoSeedService(
  val environmentService: EnvironmentService,
  val sessionService: SessionService,
) {

  private companion object {
    const val SUPERVISOR_SESSION_PROJECT_CODE = "N56123456"
    val SUPERVISOR_SESSION_PROJECT_DAY: LocalDate = LocalDate.of(2026, 3, 1)
    const val SUPERVISOR_CODE = "N56A108"
  }

  @PostConstruct
  fun onStartup() {
    environmentService.ensureTestEnvironment("auto seed should not be enabled outside of test environments")

    ensureSupervisorAllocationExists()
  }

  private fun ensureSupervisorAllocationExists() {
    sessionService.allocateSupervisor(
      sessionId = SessionIdDto(
        projectCode = SUPERVISOR_SESSION_PROJECT_CODE,
        day = SUPERVISOR_SESSION_PROJECT_DAY,
      ),
      supervisorCode = SUPERVISOR_CODE,
      allocatedByUsername = "AutoSeedService",
    )
  }
}
