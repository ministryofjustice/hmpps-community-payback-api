package uk.gov.justice.digital.hmpps.communitypayback.config

data class AuthConfig(
    val authBaseUrl: String? = System.getProperty("authBaseUrl", null),
    val connectSidCookie: String? = System.getProperty("connectSidCookieValue", "my-auth-cookie-value")
)
