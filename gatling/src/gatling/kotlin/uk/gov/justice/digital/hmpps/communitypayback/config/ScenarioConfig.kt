package uk.gov.justice.digital.hmpps.communitypayback.config

data class ScenarioConfig(
  val nothingFor: Long = 5L,
  val atOnceUsers: Int = 10,
  val rampUsers: Int = 50,
  val rampUsersDuring: Long = 30L,
  val constantUsersPerSec: Double = 10.0,
  val constantUsersPerSecDuring: Long = 60L,
) {

  companion object {
    fun fromEnv(): ScenarioConfig {
      val nothingFor = System.getenv("NOTHING_FOR")?.toLongOrNull() ?: 5L
      val atOnceUsers = System.getenv("AT_ONCE_USERS")?.toIntOrNull() ?: 10
      val rampUsers = System.getenv("RAMP_USERS")?.toIntOrNull() ?: 50
      val rampUsersDuring = System.getenv("RAMP_USERS_DURING")?.toLongOrNull() ?: 30L
      val constantUsersPerSec = System.getenv("CONSTANT_USERS_PER_SEC")?.toDoubleOrNull() ?: 10.0
      val constantUsersPerSecDuring = System.getenv("CONSTANT_USERS_PER_SEC_DURING")?.toLongOrNull() ?: 60L

      return ScenarioConfig(
        nothingFor = System.getenv("NOTHING_FOR")?.toLongOrNull() ?: 5L,
        atOnceUsers = System.getenv("AT_ONCE_USERS")?.toIntOrNull() ?: 10,
        rampUsers = System.getenv("RAMP_USERS")?.toIntOrNull() ?: 50,
        rampUsersDuring = System.getenv("RAMP_USERS_DURING")?.toLongOrNull() ?: 30L,
        constantUsersPerSec = System.getenv("CONSTANT_USERS_PER_SEC")?.toDoubleOrNull() ?: 10.0,
        constantUsersPerSecDuring = System.getenv("CONSTANT_USERS_PER_SEC_DURING")?.toLongOrNull() ?: 60L
      )
    }
  }
}