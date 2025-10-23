package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

class ConflictException(override val message: String) : RuntimeException(message)
