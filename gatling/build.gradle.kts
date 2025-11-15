import io.gatling.gradle.GatlingRunTask

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")

    id("io.gatling.gradle") version "3.14.9"
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

val fetchK8sClientCreds = tasks.register("fetchK8sClientCreds") {
    group = "gatling"
    description = "Fetches CLIENT_CREDS_CLIENT_ID and CLIENT_CREDS_CLIENT_SECRET from K8s secret hmpps-community-payback-ui-client-creds in hmpps-community-payback-<.env> namespace"

    doLast {
        val envName = (project.findProperty("envName") as String?) ?: "dev"
        val namespace = "hmpps-community-payback-$envName"
        val secretName = "hmpps-community-payback-ui-client-creds"

        fun checkCmd(cmd: String): Boolean {
          val proc = ProcessBuilder("bash", "-lc", "command -v $cmd >/dev/null 2>&1; echo $?")
            .redirectErrorStream(true)
            .start()
          val exit = proc.waitFor()
          val out = proc.inputStream.bufferedReader().readText().trim()
          val lastLine = out.lines().lastOrNull() ?: ""
          return exit == 0 && lastLine == "0"
        }

        if (!checkCmd("kubectl")) {
            logger.warn("[GATLING][Gradle] kubectl not found on PATH; skipping K8s credential fetch")
            return@doLast
        }
        if (!checkCmd("jq")) {
            logger.warn("[GATLING][Gradle] jq not found on PATH; skipping K8s credential fetch")
            return@doLast
        }

        val cmd = "kubectl -n $namespace get secrets $secretName -o json | jq -r '.data | to_entries[] | \"\\(.key)=\\(.value|@base64d)\"'"
        val process = ProcessBuilder("bash", "-lc", cmd)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            logger.warn("[GATLING][Gradle] kubectl/jq command failed (exit=$exitCode). Output: $output")
            return@doLast
        }

        val map = mutableMapOf<String, String>()
        output.lineSequence()
            .map { it.trim() }
            .filter { it.contains('=') }
            .forEach {
                val idx = it.indexOf('=')
                if (idx > 0) {
                    val k = it.substring(0, idx)
                    val v = it.substring(idx + 1)
                    if (k.isNotBlank()) map[k] = v
                }
            }

        fun firstNonNull(vararg keys: String): String? = keys.firstNotNullOfOrNull { map[it] }

        val clientId = firstNonNull("CLIENT_CREDS_CLIENT_ID")
        val clientSecret = firstNonNull("CLIENT_CREDS_CLIENT_SECRET")

        if (clientSecret.isNullOrBlank()) {
            logger.warn("[GATLING][Gradle] CLIENT_SECRET not found in secret output; proceeding without it")
        }

        // Store in project extra to be consumed by gatlingRun later
        val extra = project.extensions.extraProperties
        extra.set("clientIdFromK8s", clientId)
        if (!clientSecret.isNullOrBlank()) extra.set("clientSecretFromK8s", clientSecret)

        logger.lifecycle("[GATLING][Gradle] Loaded clientId=$clientId from namespace=$namespace secret=$secretName")
    }
}

tasks.register<Exec>("gatlingRunWithK8sCreds") {
  group = "gatling"
  description = "Fetch creds from K8s and run gatlingRun (pass -PenvName=dev and optionally -PsimulationFqn=<FQN>)"
  dependsOn(fetchK8sClientCreds)

  val simulationFqn = (project.findProperty("simulationFqn") as String?)
  val nothingFor = (project.findProperty("nothingFor") as String?)
  val atOnceUsers = (project.findProperty("atOnceUsers") as String?)
  val rampUsers = (project.findProperty("rampUsers") as String?)
  val rampUsersDuring = (project.findProperty("rampUsersDuring") as String?)
  val constantUsersPerSec = (project.findProperty("constantUsersPerSec") as String?)
  val constantUsersPerSecDuring = (project.findProperty("constantUsersPerSecDuring") as String?)

  environment("NOTHING_FOR", nothingFor ?: "5")
  environment("AT_ONCE_USERS", atOnceUsers ?: "10")
  environment("RAMP_USERS", rampUsers ?: "50")
  environment("RAMP_USERS_DURING", rampUsersDuring ?: "30")
  environment("CONSTANT_USERS_PER_SEC", constantUsersPerSec ?: "10.0")
  environment("CONSTANT_USERS_PER_SEC_DURING", constantUsersPerSecDuring ?: "60")

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

  doFirst {
    val extra = project.extensions.extraProperties
    if (extra.has("clientIdFromK8s")) {
      environment("CLIENT_ID", extra.get("clientIdFromK8s") as String)
    }
    if (extra.has("clientSecretFromK8s")) {
      environment("CLIENT_SECRET", extra.get("clientSecretFromK8s") as String)
    }
    println("[GATLING][Gradle] Launching nested gatlingRun with CLIENT_ID/CLIENT_SECRET .env vars if available")
  }
}


tasks.register<Exec>("gatlingRunCi") {
  group = "gatling"
  description = "Run un-attended in github ci)"

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

