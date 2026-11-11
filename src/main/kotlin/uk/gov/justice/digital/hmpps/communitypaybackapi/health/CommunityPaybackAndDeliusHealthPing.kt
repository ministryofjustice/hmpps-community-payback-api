package uk.gov.justice.digital.hmpps.communitypaybackapi.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component("communityPaybackAndDelius")
@ConditionalOnEnabledHealthIndicator("communityPaybackAndDelius")
class CommunityPaybackAndDeliusHealthPing(@Qualifier("communityPaybackAndDeliusWebClient") webClient: WebClient) : HealthPingCheck(webClient)
