package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.incentivesscheme.internal

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.incentivesscheme.validAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.incentivesscheme.validAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.IncentivesSchemeStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeProgressionService
import java.time.Duration

@ExtendWith(MockKExtension::class)
class IncentivesSchemeProgressionServiceTest {
  val config = IncentivesSchemeConfig(0.25, 0.5)

  @InjectMockKs
  lateinit var service: IncentivesSchemeProgressionService

  @Nested
  inner class GetProgress {
    @Test
    fun `Returns zero hours and eligible status when no events are provided`() {
      val requirement = NDUnpaidWorkRequirement.valid()
      val events = emptyList<IncentivesSchemeEvent>()

      val result = service.getProgress(requirement, events)

      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ZERO)
      assertThat(result.status).isEqualTo(IncentivesSchemeStatus.ELIGIBLE)
    }

    @Test
    fun `Returns zero hours when qualifying time would be negative due to adjustments increasing the requirement`() {
      val requirement = NDUnpaidWorkRequirement.valid()
      val events = listOf(
        IncentivesSchemeEvent.validAdjustment(duration = Duration.ofHours(-10)),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `Returns total time worked so far when all events are qualifying`() {
      val requirement = NDUnpaidWorkRequirement.valid()
      val events = listOf(
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAdjustment(duration = Duration.ofMinutes(45)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAdjustment(duration = Duration.ofMinutes(30)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAdjustment(duration = Duration.ofMinutes(45)),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(26))
    }

    @Test
    fun `Returns total time worked up until disqualifying event`() {
      val requirement = NDUnpaidWorkRequirement.valid()
      val events = listOf(
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8), isDisqualifying = true),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(8))
      assertThat(result.status).isEqualTo(IncentivesSchemeStatus.DISQUALIFIED)
    }

    @Test
    fun `Ignores events that don't qualify`() {
      val requirement = NDUnpaidWorkRequirement.valid()
      val events = listOf(
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8), isQualifying = false),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8), isQualifying = false),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(8))
    }

    @Test
    fun `Returns eligible status when qualifying time worked does not reach the threshold`() {
      val requirementProgess = NDRequirementProgress.valid().copy(requiredMinutes = 3600)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgess)

      val events = listOf(
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(6)),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.status).isEqualTo(IncentivesSchemeStatus.ELIGIBLE)
    }

    @Test
    fun `Returns qualifying status when qualifying time worked has reached the threshold`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 3600)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val events = listOf(
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.status).isEqualTo(IncentivesSchemeStatus.QUALIFYING)
    }
  }

  @Nested
  inner class GetTotalTimeWorked {
    @Test
    fun `Returns the sum of all event durations`() {
      val events = listOf(
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAdjustment(duration = Duration.ofMinutes(45)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAdjustment(duration = Duration.ofMinutes(30)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAdjustment(duration = Duration.ofMinutes(45)),
      )

      val result = service.getTotalTimeWorked(events)

      assertThat(result).isEqualTo(Duration.ofHours(26))
    }
  }
}
