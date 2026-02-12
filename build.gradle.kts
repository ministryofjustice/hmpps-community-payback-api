import java.net.InetSocketAddress
import java.net.Socket

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.3"
  kotlin("plugin.spring") version "2.3.10"
  kotlin("plugin.jpa") version "2.3.10"
  id("dev.detekt") version "2.0.0-alpha.2"
  jacoco
  id("io.sentry.jvm.gradle") version "6.0.0"
}

configurations {
  testImplementation {
    exclude(group = "org.junit.vintage")
    exclude(group = "org.mockito")
    exclude(group = "org.mockito.kotlin")
  }
  all {
    exclude(group = "dev.detekt", module = "detekt-report-checkstyle")
  }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.0.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:6.0.1")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-hateoas")

  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.postgresql:postgresql")

  implementation("org.redisson:redisson-spring-boot-starter:4.1.0")

  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.0.0")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.37") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("org.testcontainers:testcontainers-localstack:2.0.3")
  testImplementation("org.testcontainers:testcontainers-postgresql:2.0.3")
  testImplementation("com.redis:testcontainers-redis:2.2.4")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.wiremock.integrations:wiremock-spring-boot:4.1.0")
  testImplementation("io.mockk:mockk:1.14.9")
  testImplementation("com.lemonappdev:konsist:0.17.3")
  testImplementation("org.zalando:logbook-spring-boot-starter:4.0.1")
}

kotlin {
  jvmToolchain(25)
}

detekt {
  buildUponDefaultConfig = true
  allRules = false
  ignoreFailures = false
  config.setFrom(files("$rootDir/detekt.yml"))
}

// detekt must use a specific kotlin version when running, this block ensures it's using the correct version
// this is variation on https://detekt.dev/docs/gettingstarted/gradle/#gradle-runtime-dependencies
configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion("2.3.0")
    }
  }
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }

  named<Test>("test") {
    finalizedBy(named("jacocoTestReport"))
  }

  val excludedFromCodeCoverage = listOf(
    "**/uk/gov/justice/digital/hmpps/communitypaybackapi/CommunityPaybackApi*",
    "**/uk/gov/justice/digital/hmpps/communitypaybackapi/config/*",
    "**/uk/gov/justice/digital/hmpps/communitypaybackapi/mock/*",
  )

  named<JacocoReport>("jacocoTestReport") {
    dependsOn(named("test"))
    classDirectories.setFrom(
      classDirectories.files.map { file ->
        fileTree(file) { exclude(excludedFromCodeCoverage) }
      },
    )
  }

  named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(named("test"))
    classDirectories.setFrom(
      classDirectories.files.map { file ->
        fileTree(file) { exclude(excludedFromCodeCoverage) }
      },
    )

    violationRules {
      rule {
        element = "BUNDLE"
        limit {
          counter = "LINE"
          value = "COVEREDRATIO"
          minimum = "0.80".toBigDecimal()
        }
      }
    }
  }

  named("check") {
    dependsOn(named("jacocoTestCoverageVerification"))
    dependsOn(named("detekt"))
  }
}

tasks.register("bootRunDebug") {
  group = "application"
  description = "Runs this project as a Spring Boot application with debug configuration"
  doFirst {
    tasks.bootRun.configure {
      jvmArgs("-Xmx512m", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=32323")
    }
  }
  finalizedBy("bootRun")
}

fun isPortInUse(port: Int): Boolean = try {
  Socket().use { socket ->
    socket.connect(InetSocketAddress("localhost", port), 1000)
    true
  }
} catch (e: Exception) {
  false
}

tasks.bootRun {
  val debugPort = 32323
  val maxRetries = 10
  val retryDelayMs = 1000L

  // Check if debug port is in use and wait if necessary
  var retries = 0
  while (isPortInUse(debugPort) && retries < maxRetries) {
    println("Debug port $debugPort is in use. Waiting for ${retryDelayMs}ms (attempt ${retries + 1}/$maxRetries)...")
    Thread.sleep(retryDelayMs)
    retries++
  }

  if (isPortInUse(debugPort)) {
    throw IllegalStateException("Debug port $debugPort is still in use after $maxRetries attempts. Please make sure the previous application instance is stopped.")
  }

  System.getenv()["BOOT_RUN_ENV_FILE"]?.let { envFilePath ->
    println("Reading env vars from file $envFilePath")
    file(envFilePath).readLines().forEach {
      if (it.isNotBlank() && !it.startsWith("#")) {
        val (key, value) = it.split("=", limit = 2)
        println("Setting env var $key")
        environment(key, value)
      }
    }
  }
}

sentry {
  includeSourceContext = false
  projectName = rootProject.name
}
