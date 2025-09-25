package uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto

class NotFoundException(entityType: String, id: String) : RuntimeException("$entityType not found for ID '$id'")
