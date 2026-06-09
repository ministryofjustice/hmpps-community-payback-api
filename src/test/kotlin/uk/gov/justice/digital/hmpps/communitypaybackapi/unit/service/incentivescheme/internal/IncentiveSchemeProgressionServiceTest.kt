package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.incentivescheme.internal

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.incentivescheme.validAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.incentivescheme.validAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.IncentiveSchemeStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeProgressionService
import java.time.Duration

@ExtendWith(MockKExtension::class)
class IncentiveSchemeProgressionServiceTest {
  val config = IncentiveSchemeConfig(0.25, 0.5)

  @InjectMockKs
  lateinit var service: IncentiveSchemeProgressionService

  @Nested
  inner class GetProgress {
    @Test
    fun `Returns zero hours and eligible status when no events are provided`() {
      val requirement = NDUnpaidWorkRequirement.valid()
      val events = emptyList<IncentiveSchemeEvent>()

      val result = service.getProgress(requirement, events)

      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ZERO)
      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.ELIGIBLE)
    }

    @Test
    fun `Returns zero hours when qualifying time would be negative due to adjustments increasing the requirement`() {
      val requirement = NDUnpaidWorkRequirement.valid()
      val events = listOf(
        IncentiveSchemeEvent.validAdjustment(duration = Duration.ofHours(-10)),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `Returns total time worked so far when all events are qualifying`() {
      val requirement = NDUnpaidWorkRequirement.valid()
      val events = listOf(
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAdjustment(duration = Duration.ofMinutes(45)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAdjustment(duration = Duration.ofMinutes(30)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAdjustment(duration = Duration.ofMinutes(45)),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(26))
    }

    @Test
    fun `Returns total time worked up until disqualifying event`() {
      val requirement = NDUnpaidWorkRequirement.valid()
      val events = listOf(
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8), isDisqualifying = true),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(8))
      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.DISQUALIFIED)
    }

    @Test
    fun `Ignores events that don't qualify`() {
      val requirement = NDUnpaidWorkRequirement.valid()
      val events = listOf(
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8), isQualifying = false),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8), isQualifying = false),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(8))
    }

    @Test
    fun `Returns eligible status when qualifying time worked does not reach the threshold`() {
      val requirementProgess = NDRequirementProgress.valid().copy(requiredMinutes = 3600)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgess)

      val events = listOf(
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(6)),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.ELIGIBLE)
    }

    @Test
    fun `Returns qualifying status when qualifying time worked has reached the threshold`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 3600)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val events = listOf(
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
      )

      val result = service.getProgress(requirement, events)

      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.QUALIFYING)
    }
  }

  @Nested
  inner class GetTotalTimeWorked {
    @Test
    fun `Returns the sum of all event durations`() {
      val events = listOf(
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAdjustment(duration = Duration.ofMinutes(45)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAdjustment(duration = Duration.ofMinutes(30)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAdjustment(duration = Duration.ofMinutes(45)),
      )

      val result = service.getTotalTimeWorked(events)

      assertThat(result).isEqualTo(Duration.ofHours(26))
    }
  }
}
