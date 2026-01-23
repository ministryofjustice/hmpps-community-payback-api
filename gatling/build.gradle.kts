import io.gatling.gradle.GatlingRunTask

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")

    id("io.gatling.gradle") version "3.14.9.5"
}

gatling {
    enterprise.closureOf<Any> {
        // Enterprise Cloud (https://cloud.gatling.io/) configuration reference: https://docs.gatling.io/reference/integrations/build-tools/gradle-plugin/
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
}

dependencies {
    gatling("io.github.cdimascio:dotenv-java:3.2.0")
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
  val envName = (project.findProperty("envName") as String?)

  val auth = "https://sign-in-$envName.hmpps.service.justice.gov.uk/auth"
  val domain = "https://community-payback-api-$envName.hmpps.service.justice.gov.uk/"

  environment("NOTHING_FOR", nothingFor ?: "5")
  environment("AT_ONCE_USERS", atOnceUsers ?: "10")
  environment("RAMP_USERS", rampUsers ?: "50")
  environment("RAMP_USERS_DURING", rampUsersDuring ?: "30")
  environment("CONSTANT_USERS_PER_SEC", constantUsersPerSec ?: "10.0")
  environment("CONSTANT_USERS_PER_SEC_DURING", constantUsersPerSecDuring ?: "60")

  environment("AUTH_BASE_URL", auth)
  environment("DOMAIN", domain)
  environment("CLIENT_ID", clientId ?: "")
  environment("CLIENT_SECRET", clientSecret ?: "")

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

