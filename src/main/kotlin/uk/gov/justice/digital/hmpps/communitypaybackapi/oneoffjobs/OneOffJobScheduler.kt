package uk.gov.justice.digital.hmpps.communitypaybackapi.oneoffjobs

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.launch.JobRestartException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.ONE_OFF_JOBS_CONFIG_NAMESPACE

@Service
class OneOffJobScheduler(
  private val jobs: List<Job>,
  private val jobOperator: JobOperator,
  @param:Value("\${${ONE_OFF_JOBS_CONFIG_NAMESPACE}.enabled:true}") private val enabled: Boolean,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostConstruct
  fun init() {
    log.info("One-off jobs are {}.", if (enabled) "enabled" else "NOT enabled")
    if (enabled) {
      scheduleOneOffJobs()
    }
  }

  private fun scheduleOneOffJobs() {
    log.info("Scheduling new one-off jobs...")
    log.info("Found {} total job(s).", jobs.count())

    for (job in jobs) {
      try {
        val jobExecution = jobOperator.start(job, JobParameters())
        log.info("Job {} started at {}", job.name, jobExecution)
      } catch (_: JobExecutionAlreadyRunningException) {
        log.warn("Job {} already running", job.name)
      } catch (_: JobRestartException) {
        log.warn("Job {} cannot be restarted", job.name)
      } catch (_: JobInstanceAlreadyCompleteException) {
        log.warn("Job {} already completed successfully", job.name)
      }
    }
  }
}
