package uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto

class BadRequestException(override val message: String) : RuntimeException(message)
