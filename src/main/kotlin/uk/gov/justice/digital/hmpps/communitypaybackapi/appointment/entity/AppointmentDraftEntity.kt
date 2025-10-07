package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table
  (name = "appointment_drafts")
data class AppointmentDraftEntity(
  @Id
  val id: UUID,

  @Column(name = "appointment_delius_id", unique = true, nullable = false)
  val appointmentDeliusId: Long,

  @Column(name = "data", nullable = false, columnDefinition = "jsonb")
  val data: String,

  @Column(name = "delius_last_updated_at")
  val deliusLastUpdatedAt: OffsetDateTime? = null,

  @Column(name = "created_at")
  val createdAt: OffsetDateTime = OffsetDateTime.now(),

  @Column(name = "updated_at")
  val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AppointmentDraftEntity) return false
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "AppointmentDraftEntity(id=$id, appointmentDeliusId='$appointmentDeliusId')"

  companion object
}
