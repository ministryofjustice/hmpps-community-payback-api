package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import uk.gov.justice.digital.hmpps.communitypaybackapi.oneoffjobs.JobIsEnabled

class OneOffJobsIT : IntegrationTestBase() {
  @TestConfiguration
  class TestOneOffJobsConfig(
    private val jobRepository: JobRepository,
  ) {
    var enabledTestJobRan: Boolean = false
    var disabledTestJobRan: Boolean = false

    @Bean
    @Conditional(JobIsEnabled::class)
    fun enabledTestJob(): Job {
      val step = StepBuilder("step1", jobRepository)
        .tasklet { _, _ ->
          enabledTestJobRan = true
          RepeatStatus.FINISHED
        }.build()

      return JobBuilder("enabledTestJob", jobRepository).start(step).build()
    }

    @Bean
    @Conditional(JobIsEnabled::class)
    fun disabledTestJob(): Job {
      val step = StepBuilder("step1", jobRepository)
        .tasklet { _, _ ->
          disabledTestJobRan = true
          RepeatStatus.FINISHED
        }.build()

      return JobBuilder("disabledTestJob", jobRepository).start(step).build()
    }
  }

  @Autowired
  private lateinit var testOneOffJobsConfig: TestOneOffJobsConfig

  @Test
  fun `enabled jobs run successfully`() {
    assertThat(testOneOffJobsConfig.enabledTestJobRan).isTrue
  }

  @Test
  fun `disabled jobs are not run`() {
    assertThat(testOneOffJobsConfig.disabledTestJobRan).isFalse
  }
}
