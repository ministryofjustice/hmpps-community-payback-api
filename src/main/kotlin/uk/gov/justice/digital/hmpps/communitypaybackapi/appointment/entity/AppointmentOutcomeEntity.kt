package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ProjectTypeEntity
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
  val startTime: LocalTime,
  val endTime: LocalTime,

  @Column(name = "contact_outcome_id")
  val contactOutcomeId: UUID,

  @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @JoinColumn(name = "contact_outcome_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
  val contactOutcomeEntity: ContactOutcomeEntity? = null,

  @Column(name = "enforcement_action_id")
  val enforcementActionId: UUID? = null,

  @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @JoinColumn(name = "enforcement_action_id", referencedColumnName = "id", nullable = true, insertable = false, updatable = false)
  val enforcementActionEntity: EnforcementActionEntity? = null,

  @Column(name = "project_type_id")
  val projectTypeId: UUID? = null,

  @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @JoinColumn(name = "project_type_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
  val projectTypeEntity: ProjectTypeEntity? = null,

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

  val respondBy: LocalDate? = null,
  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),
  @UpdateTimestamp
  val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AppointmentOutcomeEntity) return false
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "AppointmentOutcomeEntity(id=$id, appointmentDeliusId='$appointmentDeliusId')"
}

enum class WorkQuality {
  EXCELLENT,
  GOOD,
  NOT_APPLICABLE,
  POOR,
  SATISFACTORY,
  UNSATISFACTORY,
  ;

  companion object
}

enum class Behaviour {
  EXCELLENT,
  GOOD,
  NOT_APPLICABLE,
  POOR,
  SATISFACTORY,
  UNSATISFACTORY,
  ;

  companion object
}
