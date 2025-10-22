package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "appointment_drafts")
data class AppointmentDraftEntity(

  @Id
  val id: UUID,

  @Column(name = "appointment_delius_id", unique = true, nullable = false)
  val appointmentDeliusId: Long,

  @Column(name = "crn", nullable = false)
  val crn: String,

  @Column(name = "project_name", nullable = false)
  val projectName: String,

  @Column(name = "project_code", nullable = false)
  val projectCode: String,

  @Column(name = "project_type_id", nullable = false)
  val projectTypeId: UUID,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_type_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
  val projectTypeEntity: ProjectTypeEntity? = null,

  @Column(name = "supervising_team_code")
  val supervisingTeamCode: String? = null,

  @Column(name = "appointment_date", nullable = false)
  val appointmentDate: LocalDate,

  @Column(name = "start_time", nullable = false)
  val startTime: LocalTime,

  @Column(name = "end_time", nullable = false)
  val endTime: LocalTime,

  @Column(name = "hi_vis_worn")
  val hiVisWorn: Boolean? = null,

  @Column(name = "worked_intensively")
  val workedIntensively: Boolean? = null,

  @Column(name = "penalty_time")
  val penaltyTime: LocalTime? = null,

  @Enumerated(EnumType.STRING)
  @Column(name = "work_quality")
  val workQuality: WorkQuality? = null,

  @Enumerated(EnumType.STRING)
  @Column(name = "behaviour")
  val behaviour: Behaviour? = null,

  @Column(name = "supervisor_officer_code")
  val supervisorOfficerCode: String? = null,

  @Column(name = "contact_outcome_id")
  val contactOutcomeId: UUID? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contact_outcome_id", referencedColumnName = "id", insertable = false, updatable = false)
  val contactOutcomeEntity: ContactOutcomeEntity? = null,

  @Column(name = "enforcement_action_id")
  val enforcementActionId: UUID? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enforcement_action_id", referencedColumnName = "id", insertable = false, updatable = false)
  val enforcementActionEntity: EnforcementActionEntity? = null,

  @Column(name = "respond_by")
  val respondBy: LocalDate? = null,

  @Column(name = "notes")
  val notes: String? = null,

  @Column(name = "delius_last_updated_at", nullable = false)
  val deliusLastUpdatedAt: OffsetDateTime,

  @CreationTimestamp
  @Column(name = "created_at")
  val createdAt: OffsetDateTime = OffsetDateTime.now(),

  @UpdateTimestamp
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
