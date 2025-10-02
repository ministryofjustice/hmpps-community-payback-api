package uk.gov.justice.digital.hmpps.communitypayback.config

data class HttpApiRequestConfig(
  val protocol: String = System.getProperty("protocol", "http"),
  val domain: String = System.getProperty("domain", "localhost"),
  val port: Int = System.getProperty("port", "8080").toInt(),
  // JWT can be provided via system property or env var; optional
  val jwt: String? = System.getProperty("jwt")
    ?: System.getenv("JWT")
    ?: System.getenv("HMPPS_AUTH_TOKEN"),
  val acceptHeader: String = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
  val acceptLanguageHeader: String = "en-US,en;q=0.5",
  val acceptEncodingHeader: String = "gzip, deflate",
  val userAgentHeader: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0",
)
