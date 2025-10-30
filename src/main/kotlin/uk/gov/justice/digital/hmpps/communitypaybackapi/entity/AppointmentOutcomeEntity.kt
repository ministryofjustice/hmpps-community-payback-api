package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "appointment_outcomes")
data class AppointmentOutcomeEntity(
  @Id
  val id: UUID,
  val appointmentDeliusId: Long,
  val deliusVersionToUpdate: UUID,
  val startTime: LocalTime,
  val endTime: LocalTime,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contact_outcome_id", referencedColumnName = "id")
  val contactOutcome: ContactOutcomeEntity,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enforcement_action_id", referencedColumnName = "id")
  val enforcementAction: EnforcementActionEntity?,

  val supervisorOfficerCode: String,
  val notes: String?,
  val hiVisWorn: Boolean?,
  val workedIntensively: Boolean?,
  val penaltyMinutes: Long?,

  @Enumerated(EnumType.STRING)
  val workQuality: WorkQuality?,

  @Enumerated(EnumType.STRING)
  val behaviour: Behaviour?,

  val respondBy: LocalDate?,

  val alertActive: Boolean?,
  val sensitive: Boolean?,

  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
  @PreUpdate
  fun preUpdate(): Unit = throw UnsupportedOperationException()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AppointmentOutcomeEntity) return false
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "AppointmentOutcomeEntity(id=$id, appointmentDeliusId='$appointmentDeliusId')"

  companion object
}
