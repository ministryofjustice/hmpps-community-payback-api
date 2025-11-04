package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalDate

data class SessionIdDto(val projectCode: String, val day: LocalDate)
