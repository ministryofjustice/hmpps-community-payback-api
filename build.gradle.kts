plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.0.0"
  kotlin("plugin.spring") version "2.2.10"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  jacoco
  id("io.sentry.jvm.gradle") version "5.9.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
    }
  }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.5.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.12")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.5.0")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.33") {
    exclude(group = "io.swagger.core.v3")
  }
}

kotlin {
  jvmToolchain(21)
}

detekt {
  buildUponDefaultConfig = true
  allRules = false
  ignoreFailures = false
  config.setFrom(files("$rootDir/detekt.yml"))

  reports {
    html.required.set(true)
    xml.required.set(true)
    txt.required.set(false)
  }
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }

  named<Test>("test") {
    finalizedBy(named("jacocoTestReport"))
  }

  val excludedFromCodeCoverage = listOf(
    "**/uk/gov/justice/digital/hmpps/communitypaybackapi/CommunityPaybackApi*",
    "**/uk/gov/justice/digital/hmpps/communitypaybackapi/config/*",
  )

  named<JacocoReport>("jacocoTestReport") {
    dependsOn(named("test"))
    classDirectories.setFrom(
      classDirectories.files.map { file ->
        fileTree(file) { exclude(excludedFromCodeCoverage) }
      },
    )

    reports {
      xml.required.set(true)
      csv.required.set(false)
      html.required.set(true)
    }
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

tasks.bootRun {
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
