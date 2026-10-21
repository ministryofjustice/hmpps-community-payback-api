package uk.gov.justice.digital.hmpps.communitypaybackapi.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component("arns")
class ArnsHealthPing(@Qualifier("arnsWebClient") webClient: WebClient) : HealthPingCheck(webClient)
