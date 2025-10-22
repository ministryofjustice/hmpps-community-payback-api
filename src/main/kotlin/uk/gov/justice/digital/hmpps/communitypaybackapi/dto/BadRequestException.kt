package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

class BadRequestException(override val message: String) : RuntimeException(message)
