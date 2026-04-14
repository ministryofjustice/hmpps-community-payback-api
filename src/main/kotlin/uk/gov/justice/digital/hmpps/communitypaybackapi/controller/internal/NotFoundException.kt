package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal

/**
 * Use of this exception is limited to controllers because they are the only
 * code that has the context for whether an entity has been addressed in the
 * URL
 */
class NotFoundException(message: String) : RuntimeException(message) {
  constructor(entityType: String, id: Any) : this("$entityType not found for ID '$id'")
}

fun notFound(entityType: String, id: Any): Nothing = throw NotFoundException(entityType, id)
