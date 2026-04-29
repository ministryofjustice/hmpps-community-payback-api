package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProbationAccessControlClient

@Service
class CaseVisibilityService(
  private val contextService: ContextService,
  private val probationAccessControlClient: ProbationAccessControlClient,
) {
  fun isLimitedForCurrentUser(crns: List<String>): Map<String, Boolean> {
    if (crns.isEmpty()) return emptyMap()

    val caseAccess = probationAccessControlClient.getAccessControlForCrns(contextService.getUserName(), crns)
    return crns.associateWith { crn ->
      val case = caseAccess.access.firstOrNull { it.crn == crn }

      case != null && (case.userRestricted || case.userExcluded)
    }
  }
}
