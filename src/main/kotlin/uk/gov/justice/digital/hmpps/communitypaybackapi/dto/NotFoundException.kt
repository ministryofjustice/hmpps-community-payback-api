package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

class NotFoundException(message: String) : RuntimeException(message) {
  constructor(entityType: String, id: String) : this("$entityType not found for ID '$id'")
}
