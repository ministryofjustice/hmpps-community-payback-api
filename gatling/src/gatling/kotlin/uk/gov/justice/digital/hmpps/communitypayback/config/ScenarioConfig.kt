import io.github.cdimascio.dotenv.Dotenv

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
      val dotenv = Dotenv.load()

      return ScenarioConfig(
        nothingFor = dotenv["NOTHING_FOR"]?.toLongOrNull() ?: 5L,
        atOnceUsers = dotenv["AT_ONCE_USERS"]?.toIntOrNull() ?: 10,
        rampUsers = dotenv["RAMP_USERS"]?.toIntOrNull() ?: 50,
        rampUsersDuring = dotenv["RAMP_USERS_DURING"]?.toLongOrNull() ?: 30L,
        constantUsersPerSec = dotenv["CONSTANT_USERS_PER_SEC"]?.toDoubleOrNull() ?: 10.0,
        constantUsersPerSecDuring = dotenv["CONSTANT_USERS_PER_SEC_DURING"]?.toLongOrNull() ?: 60L
      )
    }
  }
}