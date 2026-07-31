package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import org.springframework.security.concurrent.DelegatingSecurityContextCallable
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

fun <T : Any> runWithCallerContext(
  securityContext: SecurityContext,
  requestAttributes: RequestAttributes?,
  action: () -> T,
): T = DelegatingSecurityContextCallable<T>(
  {
    RequestContextHolder.setRequestAttributes(requestAttributes)
    try {
      action()
    } finally {
      RequestContextHolder.resetRequestAttributes()
    }
  },
  securityContext,
).call()
