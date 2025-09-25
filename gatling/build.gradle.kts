import io.gatling.gradle.GatlingRunTask

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.allopen") version "2.1.0"

    id("io.gatling.gradle") version "3.13.3"
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
    gatling(platform("org.http4k:http4k-bom:5.12.0.0"))
    gatling("org.http4k:http4k-core")
    gatling("org.http4k:http4k-server-undertow")
    gatling("org.http4k:http4k-client-apache")
    gatling("io.github.cdimascio:dotenv-java:3.0.0")
}
