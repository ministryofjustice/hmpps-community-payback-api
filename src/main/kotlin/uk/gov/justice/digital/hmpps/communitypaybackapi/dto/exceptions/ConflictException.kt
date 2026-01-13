package uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions

class ConflictException(override val message: String) : RuntimeException(message)
