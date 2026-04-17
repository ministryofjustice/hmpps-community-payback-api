package uk.gov.justice.digital.hmpps.communitypayback.config

import io.github.cdimascio.dotenv.Dotenv

data class HttpApiRequestConfig(
  val apiUrl: String = (Dotenv.load()["API_URL"] ?: "http"),
)
