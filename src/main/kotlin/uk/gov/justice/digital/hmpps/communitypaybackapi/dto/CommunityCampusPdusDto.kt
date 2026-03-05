package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.util.UUID

data class CommunityCampusPdusDto(
  val pdus: List<CommunityCampusPduDto>,
)

data class CommunityCampusPduDto(
  val id: UUID,
  val name: String,
  val providerCode: String,
)
