package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.oneoffjobs

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.launch.JobRestartException
import uk.gov.justice.digital.hmpps.communitypaybackapi.oneoffjobs.OneOffJobScheduler

@ExtendWith(MockKExtension::class)
class OneOffJobSchedulerTest {
  @MockK
  lateinit var jobOperator: JobOperator

  @BeforeEach
  fun beforeEach() {
    every { jobOperator.start(any<Job>(), any<JobParameters>()) } returns mockk()
  }

  @Test
  fun `init does nothing when one-off jobs are disabled`() {
    val jobs = listOf<Job>(
      mockk {
        every { name } returns "job1"
      },
      mockk {
        every { name } returns "job2"
      },
      mockk {
        every { name } returns "job3"
      },
    )

    OneOffJobScheduler(jobs, jobOperator, false).init()

    verify(exactly = 0) { jobOperator.start(any<Job>(), any<JobParameters>()) }
  }

  @Test
  fun `init starts all configured jobs when enabled`() {
    val jobs = listOf<Job>(
      mockk {
        every { name } returns "job1"
      },
      mockk {
        every { name } returns "job2"
      },
      mockk {
        every { name } returns "job3"
      },
    )

    OneOffJobScheduler(jobs, jobOperator, true).init()

    verify(exactly = 1) {
      jobOperator.start(jobs[0], JobParameters())
      jobOperator.start(jobs[1], JobParameters())
      jobOperator.start(jobs[2], JobParameters())
    }
  }

  @Test
  fun `init completes successfully even if jobs fail`() {
    val jobs = listOf<Job>(
      mockk {
        every { name } returns "job1"
      },
      mockk {
        every { name } returns "job2"
      },
      mockk {
        every { name } returns "job3"
      },
    )

    every { jobOperator.start(match { it.name == "job1" }, JobParameters()) } throws JobExecutionAlreadyRunningException("")
    every { jobOperator.start(match { it.name == "job2" }, JobParameters()) } throws JobRestartException("")
    every { jobOperator.start(match { it.name == "job3" }, JobParameters()) } throws JobInstanceAlreadyCompleteException("")

    assertDoesNotThrow { OneOffJobScheduler(jobs, jobOperator, true).init() }
  }
}
