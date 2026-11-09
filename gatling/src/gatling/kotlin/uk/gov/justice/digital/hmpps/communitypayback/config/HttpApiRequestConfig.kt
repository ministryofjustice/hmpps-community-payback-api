package uk.gov.justice.digital.hmpps.communitypayback.config

data class HttpApiRequestConfig(
  val apiUrl: String = Environment["API_URL"] ?: "http://localhost:8080",
)
