package uk.gov.justice.digital.hmpps.communitypayback.config

import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.Base64

object OAuthTokenProvider {
  private val httpClient: HttpClient = HttpClient.newHttpClient()
  private val logger = LoggerFactory.getLogger(OAuthTokenProvider::class.java)

  fun fetchAccessToken(isSupervisor: Boolean = false): String? {
    val clientIdEnv = if (isSupervisor) "SUPERVISOR_CLIENT_ID" else "CLIENT_ID"
    val clientSecretEnv = if (isSupervisor) "SUPERVISOR_CLIENT_SECRET" else "CLIENT_SECRET"

    val clientId = System.getenv(clientIdEnv)?.trim() ?: ""
    val clientSecret = System.getenv(clientSecretEnv)?.trim() ?: ""
    val authBaseUrl = System.getenv("AUTH_BASE_URL") ?: "https://sign-in-dev.hmpps.service.justice.gov.uk/auth"
    val encodedGrantType = URLEncoder.encode("client_credentials", StandardCharsets.UTF_8)

    if (clientId.isBlank() || clientSecret.isBlank()) {
      if (isSupervisor) {
        // Fallback to standard client if supervisor client is not configured
        return fetchAccessToken(false)
      }
      throw IllegalStateException("Client credentials not configured - $clientIdEnv and $clientSecretEnv must be provided")
    }

    val url = "$authBaseUrl/oauth/token?grant_type=$encodedGrantType"
    val basic = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray(StandardCharsets.UTF_8))

    logger.debug("[GATLING][Auth] Fetching access token from $url, with Basic auth $basic for client $clientIdEnv")

    val request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .POST(HttpRequest.BodyPublishers.noBody())
      .header("Content-Type", "application/json")
      .header("Authorization", "Basic $basic")
      .build()

    return try {
      val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
      if (response.statusCode() in 200..299) {
        parseAccessToken(response.body())
      } else {
        throw IllegalStateException("[GATLING][Auth] Failed to obtain token: HTTP ${response.statusCode()} - ${response.body()}")
      }
    } catch (ex: Exception) {
      throw IllegalStateException("[GATLING][Auth] Exception obtaining token: ${ex.message}")
    }
  }

  private fun parseAccessToken(json: String): String? {
    val regex = Regex("\\\"access_token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
    val match = regex.find(json)
    return match?.groups?.get(1)?.value
  }
}
