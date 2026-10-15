package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository
import org.springframework.batch.core.repository.JobRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Configuration

const val ONE_OFF_JOBS_CONFIG_NAMESPACE = "community-payback.one-off-jobs"

@Configuration
@EnableJdbcJobRepository
@ConditionalOnBooleanProperty(value = ["$ONE_OFF_JOBS_CONFIG_NAMESPACE.enabled"], havingValue = true, matchIfMissing = true)
class OneOffJobsConfiguration(
  @Suppress("unused") private val jobRepository: JobRepository,
)
