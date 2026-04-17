package uk.gov.justice.digital.hmpps.communitypayback.config

data class ScenarioConfig(
  val nothingFor: Long = 5L,
  val atOnceUsers: Int = 10,
  val rampUsers: Int = 50,
  val rampUsersDuring: Long = 60L,
  val constantUsersPerSec: Double = 5.0,
  val constantUsersPerSecDuring: Long = 200,
  val caseAdminUsername: String = "CPBTestCaseAdmin1",
  val supervisorUsername: String = "CPBTestSupervisor1",
  val providerCode: String = "N56",
) {

  companion object {
    fun fromEnv(): ScenarioConfig = ScenarioConfig(
      nothingFor = System.getenv("NOTHING_FOR")?.toLongOrNull() ?: 5L,
      atOnceUsers = System.getenv("AT_ONCE_USERS")?.toIntOrNull() ?: 10,
      rampUsers = System.getenv("RAMP_USERS")?.toIntOrNull() ?: 50,
      rampUsersDuring = System.getenv("RAMP_USERS_DURING")?.toLongOrNull() ?: 60L,
      constantUsersPerSec = System.getenv("CONSTANT_USERS_PER_SEC")?.toDoubleOrNull() ?: 5.0,
      constantUsersPerSecDuring = System.getenv("CONSTANT_USERS_PER_SEC_DURING")?.toLongOrNull() ?: 200L,
      caseAdminUsername = System.getenv("CASE_ADMIN_USERNAME") ?: "CPBTestCaseAdmin1",
      supervisorUsername = System.getenv("SUPERVISOR_USERNAME") ?: "CPBTestSupervisor1",
      providerCode = System.getenv("PROVIDER_CODE") ?: "N56",
    )
  }
}
