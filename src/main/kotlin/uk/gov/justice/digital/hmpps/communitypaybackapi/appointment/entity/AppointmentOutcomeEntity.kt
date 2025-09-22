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
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntity
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

  companion object
}

enum class WorkQuality(val dtoType: AppointmentWorkQualityDto) {
  EXCELLENT(AppointmentWorkQualityDto.EXCELLENT),
  GOOD(AppointmentWorkQualityDto.GOOD),
  NOT_APPLICABLE(AppointmentWorkQualityDto.NOT_APPLICABLE),
  POOR(AppointmentWorkQualityDto.POOR),
  SATISFACTORY(AppointmentWorkQualityDto.SATISFACTORY),
  UNSATISFACTORY(AppointmentWorkQualityDto.UNSATISFACTORY),
  ;

  companion object
}

enum class Behaviour(val dtoType: AppointmentBehaviourDto) {
  EXCELLENT(AppointmentBehaviourDto.EXCELLENT),
  GOOD(AppointmentBehaviourDto.GOOD),
  NOT_APPLICABLE(AppointmentBehaviourDto.NOT_APPLICABLE),
  POOR(AppointmentBehaviourDto.POOR),
  SATISFACTORY(AppointmentBehaviourDto.SATISFACTORY),
  UNSATISFACTORY(AppointmentBehaviourDto.UNSATISFACTORY),
  ;

  companion object
}
