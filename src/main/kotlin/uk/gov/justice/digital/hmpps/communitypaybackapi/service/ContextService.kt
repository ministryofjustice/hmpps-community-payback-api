package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class ContextService {
  fun getUserName(): String = SecurityContextHolder.getContext().authentication!!.name
    ?: error("Username should be included in the client token")
}
