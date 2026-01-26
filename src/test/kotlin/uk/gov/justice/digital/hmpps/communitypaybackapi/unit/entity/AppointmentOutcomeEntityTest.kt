package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomOffsetDateTime
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import java.util.UUID

class AppointmentOutcomeEntityTest {

  @Nested
  inner class IsLogicallyIdentical {

    @Test
    fun `If identical return true, ignoring certain fields`() {
      val old = AppointmentEventEntity.valid()
      val new = old.copy(
        id = UUID.randomUUID(),
        createdAt = randomOffsetDateTime(),
      )

      assertThat(old.isLogicallyIdentical(new)).isTrue()
    }

    @Test
    fun `If contact outcome has changed, return false`() {
      val old = AppointmentEventEntity.valid()
      val new = old.copy(
        contactOutcome = ContactOutcomeEntity.valid(),
      )

      assertThat(old.isLogicallyIdentical(new)).isFalse()
    }

    @Test
    fun `If a primitive field has changed, return false`() {
      val old = AppointmentEventEntity.valid().copy(
        hiVisWorn = true,
      )
      val new = old.copy(
        hiVisWorn = false,
      )

      assertThat(old.isLogicallyIdentical(new)).isFalse()
    }
  }
}
