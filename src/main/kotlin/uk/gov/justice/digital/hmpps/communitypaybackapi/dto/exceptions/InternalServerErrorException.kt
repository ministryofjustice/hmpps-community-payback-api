package uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions

class InternalServerErrorException(override val message: String) : RuntimeException(message)
