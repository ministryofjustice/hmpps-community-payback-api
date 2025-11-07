package uk.gov.justice.digital.hmpps.communitypayback.config

import io.github.cdimascio.dotenv.Dotenv

data class HttpApiRequestConfig(
  val protocol: String = Dotenv.load()["PROTOCOL"] ?: "http",
  val domain: String = Dotenv.load()["DOMAIN"] ?: "localhost",
  val port: Int = (Dotenv.load()["PORT"] ?: "8080").toInt(),
)
