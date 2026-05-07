plugins {
  kotlin("jvm")
  kotlin("plugin.allopen")
  id("org.jlleitschuh.gradle.ktlint")
  id("io.gatling.gradle") version "3.15.0.1"
}

gatling {
  enterprise.closureOf<Any> {
    // Enterprise Cloud (https://cloud.gatling.io/) configuration reference: https://docs.gatling.io/reference/integrations/build-tools/gradle-plugin/
  }
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
  mavenCentral()
}

dependencies {
  gatlingImplementation("io.github.cdimascio:dotenv-java:3.2.0")
  gatlingImplementation(project(":"))
  gatlingImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.3")
  gatlingImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.3")
}

tasks.register<Exec>("gatlingRunCi") {
  group = "gatling"
  description = "Run un-attended in github ci"

  val simulationFqn = (project.findProperty("simulationFqn") as String?)
  val nothingFor = (project.findProperty("nothingFor") as String?)
  val atOnceUsers = (project.findProperty("atOnceUsers") as String?)
  val rampUsers = (project.findProperty("rampUsers") as String?)
  val rampUsersDuring = (project.findProperty("rampUsersDuring") as String?)
  val constantUsersPerSec = (project.findProperty("constantUsersPerSec") as String?)
  val constantUsersPerSecDuring = (project.findProperty("constantUsersPerSecDuring") as String?)
  val clientId = (project.findProperty("CLIENT_ID") as String?)
  val clientSecret = (project.findProperty("CLIENT_SECRET") as String?)
  val supervisorClientId = (project.findProperty("SUPERVISOR_CLIENT_ID") as String?)
  val supervisorClientSecret = (project.findProperty("SUPERVISOR_CLIENT_SECRET") as String?)
  val envName = (project.findProperty("envName") as String?)

  val apiUrl = "https://community-payback-api-$envName.hmpps.service.justice.gov.uk"

  environment("NOTHING_FOR", nothingFor ?: "5")
  environment("AT_ONCE_USERS", atOnceUsers ?: "10")
  environment("RAMP_USERS", rampUsers ?: "50")
  environment("RAMP_USERS_DURING", rampUsersDuring ?: "30")
  environment("CONSTANT_USERS_PER_SEC", constantUsersPerSec ?: "10.0")
  environment("CONSTANT_USERS_PER_SEC_DURING", constantUsersPerSecDuring ?: "60")

  environment("API_URL", apiUrl)
  environment("CLIENT_ID", clientId ?: "")
  environment("CLIENT_SECRET", clientSecret ?: "")
  environment("SUPERVISOR_CLIENT_ID", supervisorClientId ?: "")
  environment("SUPERVISOR_CLIENT_SECRET", supervisorClientSecret ?: "")

  val args = mutableListOf("gatlingRun")
  if (!simulationFqn.isNullOrBlank()) {
    args += listOf("--simulation", simulationFqn)
  } else {
    args += listOf("--all")
  }

  workingDir = project.rootDir
  val wrapper = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "gradlew.bat" else "./gradlew"
  println("[GATLING][Gradle] $wrapper ${args.joinToString(" ")}")
  commandLine(wrapper, *args.toTypedArray())
}
