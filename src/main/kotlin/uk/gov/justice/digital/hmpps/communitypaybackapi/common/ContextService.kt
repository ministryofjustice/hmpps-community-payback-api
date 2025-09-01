package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class ContextService {
  fun getUserName(): String? = SecurityContextHolder.getContext().authentication.name
}
