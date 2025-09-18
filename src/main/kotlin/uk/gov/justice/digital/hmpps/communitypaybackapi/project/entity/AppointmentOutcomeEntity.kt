package uk.gov.justice.digital.hmpps.communitypaybackapi.project.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
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
  val projectTypeDeliusId: Long,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val contactOutcomeDeliusId: Long,
  val supervisorTeamDeliusId: Long,
  val supervisorOfficerDeliusId: Long,
  val notes: String? = null,
  val hiVisWorn: Boolean? = null,
  val workedIntensively: Boolean? = null,
  val penaltyMinutes: Long? = null,

  @Enumerated(EnumType.STRING)
  val workQuality: WorkQuality? = null,

  @Enumerated(EnumType.STRING)
  val behaviour: Behaviour? = null,

  val enforcementActionDeliusId: Long? = null,
  val respondBy: LocalDate? = null,
  val createdAt: OffsetDateTime = OffsetDateTime.now(),
  val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AppointmentOutcomeEntity) return false
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}

enum class WorkQuality {
  EXCELLENT,
  GOOD,
  NOT_APPLICABLE,
  POOR,
  SATISFACTORY,
  UNSATISFACTORY,
}

enum class Behaviour {
  EXCELLENT,
  GOOD,
  NOT_APPLICABLE,
  POOR,
  SATISFACTORY,
  UNSATISFACTORY,
}
