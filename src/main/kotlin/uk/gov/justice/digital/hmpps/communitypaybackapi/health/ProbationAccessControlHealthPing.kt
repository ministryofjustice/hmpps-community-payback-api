package uk.gov.justice.digital.hmpps.communitypaybackapi.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component("probationAccessControl")
class ProbationAccessControlHealthPing(@Qualifier("probationAccessControlWebClient") webClient: WebClient) : HealthPingCheck(webClient)
