package uk.gov.justice.digital.hmpps.communitypayback.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Base64

object OAuthTokenProvider {
  private val httpClient: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
  private val logger = LoggerFactory.getLogger(OAuthTokenProvider::class.java)

  fun fetchAccessToken(isSupervisor: Boolean = false): String {
    val clientIdEnv = if (isSupervisor) "SUPERVISOR_CLIENT_ID" else "CLIENT_ID"
    val clientSecretEnv = if (isSupervisor) "SUPERVISOR_CLIENT_SECRET" else "CLIENT_SECRET"

    val clientId = Environment[clientIdEnv]?.trim() ?: ""
    val clientSecret = Environment[clientSecretEnv]?.trim() ?: ""
    val authBaseUrl = Environment["AUTH_BASE_URL"] ?: "https://sign-in-dev.hmpps.service.justice.gov.uk/auth"
    val encodedGrantType = URLEncoder.encode("client_credentials", StandardCharsets.UTF_8)

    if (clientId.isBlank() || clientSecret.isBlank()) {
      if (isSupervisor) {
        // Fallback to standard client if supervisor client is not configured
        return fetchAccessToken(false)
      }
      throw IllegalStateException("Client credentials not configured - $clientIdEnv and $clientSecretEnv must be provided")
    }

    val url = "${authBaseUrl.trimEnd('/')}/oauth/token"
    val basic = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray(StandardCharsets.UTF_8))

    logger.debug("[GATLING][Auth] Fetching access token from {} using {}", url, clientIdEnv)

    val request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofSeconds(30))
      .POST(HttpRequest.BodyPublishers.ofString("grant_type=$encodedGrantType"))
      .header("Content-Type", "application/x-www-form-urlencoded")
      .header("Authorization", "Basic $basic")
      .build()

    return try {
      val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
      if (response.statusCode() in 200..299) {
        parseAccessToken(response.body())
      } else {
        throw IllegalStateException("[GATLING][Auth] Failed to obtain token: HTTP ${response.statusCode()}")
      }
    } catch (ex: Exception) {
      throw IllegalStateException("[GATLING][Auth] Exception obtaining token", ex)
    }
  }

  private fun parseAccessToken(json: String): String {
    val token = jacksonObjectMapper().readTree(json).path("access_token").asText("")
    check(token.isNotBlank()) { "[GATLING][Auth] Token response did not contain an access_token" }
    return token
  }
}
