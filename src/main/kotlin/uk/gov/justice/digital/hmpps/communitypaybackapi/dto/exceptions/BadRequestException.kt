package uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions

class BadRequestException(override val message: String) : RuntimeException(message)
