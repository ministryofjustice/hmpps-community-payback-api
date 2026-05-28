package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry

@Configuration
@EnableRetry
class RetryConfiguration
