package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

class NotFoundException(entityType: String, id: String) : RuntimeException("$entityType not found for ID '$id'")
