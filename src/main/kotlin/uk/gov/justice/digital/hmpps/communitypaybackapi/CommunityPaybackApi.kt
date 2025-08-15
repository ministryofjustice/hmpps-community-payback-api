package uk.gov.justice.digital.hmpps.communitypaybackapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CommunityPaybackApi

fun main(args: Array<String>) {
  runApplication<CommunityPaybackApi>(*args)
}
