package uk.gov.justice.digital.hmpps.communitypayback.config

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Attempts to automatically obtain an OAuth2 access token using the Client Credentials grant.
 *
 * Configuration (system properties take precedence over env vars):
 * - authBaseUrl / AUTH_BASE_URL (e.g. https://sign-in-dev.hmpps.service.justice.gov.uk/auth)
 * - authTokenPath / AUTH_TOKEN_PATH (default: /oauth/token)
 * - clientId / CLIENT_ID
 * - clientSecret / CLIENT_SECRET
 * - grantType / GRANT_TYPE (default: client_credentials)
 * - scope / SCOPE (optional)
 */
object OAuthTokenProvider {
  private val httpClient: HttpClient = HttpClient.newHttpClient()

  fun fetchAccessTokenOrNull(): String? {
    val clientId = sysOrEnv("clientId", "CLIENT_ID")
    val clientSecret = sysOrEnv("clientSecret", "CLIENT_SECRET")
    val authBaseUrl = sysOrEnv("authBaseUrl", "AUTH_BASE_URL")
      ?: // sensible default for dev
      "https://sign-in-dev.hmpps.service.justice.gov.uk/auth"
    val tokenPath = sysOrEnv("authTokenPath", "AUTH_TOKEN_PATH") ?: "/oauth/token"
    val grantType = sysOrEnv("grantType", "GRANT_TYPE") ?: "client_credentials"
    val scope = sysOrEnv("scope", "SCOPE")

    if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank()) {
      // Not configured, bail out quietly
      return null
    }

    val url = buildString {
      append(authBaseUrl.trimEnd('/'))
      append(tokenPath)
      append("?grant_type=")
      append(encode(grantType))
      if (!scope.isNullOrBlank()) {
        append("&scope=")
        append(encode(scope))
      }
    }

    val basic = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray(StandardCharsets.UTF_8))

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
        System.err.println("[GATLING][Auth] Failed to obtain token: HTTP ${response.statusCode()} - ${response.body()}")
        null
      }
    } catch (ex: Exception) {
      System.err.println("[GATLING][Auth] Exception obtaining token: ${ex.message}")
      null
    }
  }

  private fun parseAccessToken(json: String): String? {
    // Minimal parsing to avoid adding dependencies
    // Looks for: "access_token":"..."
    val regex = Regex("\\\"access_token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
    val match = regex.find(json)
    return match?.groups?.get(1)?.value
  }

  private fun encode(value: String): String = java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)

  private fun sysOrEnv(sysKey: String, envKey: String): String? =
    System.getProperty(sysKey) ?: System.getenv(envKey)
}
