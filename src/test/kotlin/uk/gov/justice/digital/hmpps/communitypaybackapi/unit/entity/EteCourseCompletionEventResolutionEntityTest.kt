package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomOffsetDateTime
import java.util.UUID

class EteCourseCompletionEventResolutionEntityTest {

  @Nested
  inner class IsLogicallyIdentical {

    @Test
    fun `If linked event is different, return false`() {
      val old = EteCourseCompletionEventResolutionEntity.valid()
      val new = old.copy(
        eteCourseCompletionEvent = EteCourseCompletionEventEntity.valid(),
      )

      assertThat(old.isLogicallyIdentical(new)).isFalse()
    }

    @Test
    fun `If contact outcome is different, return false`() {
      val old = EteCourseCompletionEventResolutionEntity.valid()
      val new = old.copy(
        contactOutcome = ContactOutcomeEntity.valid(),
      )

      assertThat(old.isLogicallyIdentical(new)).isFalse()
    }

    @Test
    fun `If identical ignoring certain fields returns true, when creating an appointment`() {
      val old = EteCourseCompletionEventResolutionEntity.valid().copy(
        deliusAppointmentCreated = true,
      )
      val new = old.copy(
        id = UUID.randomUUID(),
        createdAt = randomOffsetDateTime(),
        createdByUsername = String.random(),
        deliusAppointmentId = Long.random(),
      )

      assertThat(old.isLogicallyIdentical(new)).isTrue()
    }

    @Test
    fun `If identical ignoring certain fields returns true, when updating an appointment`() {
      val old = EteCourseCompletionEventResolutionEntity.valid().copy(
        deliusAppointmentCreated = false,
      )
      val new = old.copy(
        id = UUID.randomUUID(),
        createdAt = randomOffsetDateTime(),
        createdByUsername = String.random(),
      )

      assertThat(old.isLogicallyIdentical(new)).isTrue()
    }

    @Test
    fun `If appointment id differs returns true, when creating an appointment`() {
      val old = EteCourseCompletionEventResolutionEntity.valid().copy(
        deliusAppointmentCreated = true,
      )
      val new = old.copy(
        deliusAppointmentId = Long.random(),
      )

      assertThat(old.isLogicallyIdentical(new)).isTrue()
    }

    @Test
    fun `If appointment id differs returns false, when updating an appointment`() {
      val old = EteCourseCompletionEventResolutionEntity.valid().copy(
        deliusAppointmentCreated = false,
      )
      val new = old.copy(
        deliusAppointmentId = Long.random(),
      )

      assertThat(old.isLogicallyIdentical(new)).isFalse()
    }

    @Test
    fun `If a primitive field has changed, return false`() {
      val old = EteCourseCompletionEventResolutionEntity.valid()
      val new = old.copy(
        projectCode = String.random(),
      )

      assertThat(old.isLogicallyIdentical(new)).isFalse()
    }
  }
}
