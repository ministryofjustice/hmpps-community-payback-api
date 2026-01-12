package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import com.fasterxml.jackson.annotation.JsonProperty

data class SqsMessage(
  @JsonProperty("Message") val message: String,
)
