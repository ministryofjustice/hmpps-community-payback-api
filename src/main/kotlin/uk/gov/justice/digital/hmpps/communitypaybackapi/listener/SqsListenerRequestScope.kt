package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.NDeliusRollbackService

/**
 * Request scope is required to support capturing spring events for
 * potential rollback, managed by the [NDeliusRollbackService]
 */
@Service
class SqsListenerRequestScope {

  fun withRequestScope(
    action: () -> Unit,
  ) {
    try {
      RequestContextHolder.setRequestAttributes(SimpleRequestAttributes())
      action.invoke()
    } finally {
      RequestContextHolder.resetRequestAttributes()
    }
  }
}

private class SimpleRequestAttributes : RequestAttributes {
  private val requestAttributeMap = mutableMapOf<String, Any>()

  override fun getAttribute(name: String, scope: Int): Any? = if (scope.isScopeRequest()) {
    this.requestAttributeMap[name]
  } else {
    null
  }

  override fun setAttribute(name: String, value: Any, scope: Int) {
    if (scope.isScopeRequest()) {
      this.requestAttributeMap[name] = value
    }
  }

  override fun removeAttribute(name: String, scope: Int) {
    if (scope.isScopeRequest()) {
      this.requestAttributeMap.remove(name)
    }
  }

  override fun getAttributeNames(scope: Int): Array<String> {
    if (scope.isScopeRequest()) {
      return this.requestAttributeMap.keys.toTypedArray<String>()
    }
    return emptyArray()
  }

  override fun registerDestructionCallback(name: String, callback: Runnable, scope: Int): Unit = throw UnsupportedOperationException()

  override fun resolveReference(key: String): Any? = throw UnsupportedOperationException()

  override fun getSessionId(): String = throw UnsupportedOperationException()

  override fun getSessionMutex(): Any = throw UnsupportedOperationException()

  private fun Int.isScopeRequest() = this == RequestAttributes.SCOPE_REQUEST
}
