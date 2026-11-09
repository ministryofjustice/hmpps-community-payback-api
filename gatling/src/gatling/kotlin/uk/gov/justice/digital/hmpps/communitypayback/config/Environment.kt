package uk.gov.justice.digital.hmpps.communitypayback.config

import io.github.cdimascio.dotenv.Dotenv
import java.io.File

object Environment {
  private val dotenv = Dotenv.configure()
    .directory(if (File("gatling").isDirectory) "gatling" else ".")
    .ignoreIfMissing()
    .load()

  operator fun get(name: String): String? = dotenv[name]?.takeIf { it.isNotBlank() }
}
