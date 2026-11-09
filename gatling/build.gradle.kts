import io.gatling.gradle.GatlingRunTask

plugins {
  kotlin("jvm")
  kotlin("plugin.allopen")
  id("org.jlleitschuh.gradle.ktlint")
  id("io.gatling.gradle") version "3.16.0"
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
  gatlingImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.4")
  gatlingImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.22.4")
}

tasks.register<GatlingRunTask>("gatlingRunCi") {
  group = "gatling"
  description = "Run unattended using the same simulation and load settings as the CI workflow"
  dependsOn(tasks.named("gatlingClasses"))

  nonInteractive = true
  simulationClassName = providers.gradleProperty("simulationFqn").orNull?.takeIf { it.isNotBlank() }
  runAllSimulations = simulationClassName == null

  val envName = providers.gradleProperty("envName").getOrElse("dev")
  val settings = mapOf(
    "NOTHING_FOR" to ("nothingFor" to "5"),
    "AT_ONCE_USERS" to ("atOnceUsers" to "10"),
    "RAMP_USERS" to ("rampUsers" to "50"),
    "RAMP_USERS_DURING" to ("rampUsersDuring" to "30"),
    "CONSTANT_USERS_PER_SEC" to ("constantUsersPerSec" to "10.0"),
    "CONSTANT_USERS_PER_SEC_DURING" to ("constantUsersPerSecDuring" to "60"),
    "API_URL" to ("API_URL" to "https://community-payback-api-$envName.hmpps.service.justice.gov.uk"),
  )
  environment = settings.mapValues { (name, setting) ->
    providers.gradleProperty(setting.first).orElse(providers.environmentVariable(name)).getOrElse(setting.second)
  } + listOf("CLIENT_ID", "CLIENT_SECRET", "SUPERVISOR_CLIENT_ID", "SUPERVISOR_CLIENT_SECRET", "AUTH_BASE_URL")
    .mapNotNull { name ->
      providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orNull
        ?.takeIf { it.isNotBlank() }?.let { name to it }
    }.toMap()
}
