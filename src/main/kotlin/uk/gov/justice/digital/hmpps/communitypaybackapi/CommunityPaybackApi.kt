package uk.gov.justice.digital.hmpps.communitypaybackapi

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class CommunityPaybackApi

fun main(args: Array<String>) {
  SpringApplication.run(arrayOf(CommunityPaybackApi::class.java), args)
}
