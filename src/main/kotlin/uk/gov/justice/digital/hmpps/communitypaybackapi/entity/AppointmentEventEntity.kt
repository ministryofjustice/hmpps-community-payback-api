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
import org.apache.commons.lang3.builder.CompareToBuilder.reflectionCompare
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "appointment_events")
data class AppointmentEventEntity(
  @Id
  val id: UUID,
  @Enumerated(EnumType.STRING)
  val eventType: AppointmentEventType,
  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),
  val triggeredAt: OffsetDateTime,
  @Enumerated(EnumType.STRING)
  val triggerType: AppointmentEventTriggerType? = null,
  val triggeredBy: String? = null,

  val triggeredSchedulingAt: OffsetDateTime? = null,
  val triggeredSchedulingId: UUID? = null,

  val crn: String,
  val deliusEventNumber: Int,
  val communityPaybackAppointmentId: UUID?,
  val deliusAppointmentId: Long,
  val priorDeliusVersion: UUID?,
  val deliusAllocationId: Long?,

  val projectCode: String,
  val projectName: String,

  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val pickupLocationCode: String?,
  val pickupTime: LocalTime?,
  val supervisorOfficerCode: String?,
  val notes: String?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contact_outcome_id", referencedColumnName = "id")
  val contactOutcome: ContactOutcomeEntity?,
  val hiVisWorn: Boolean?,
  val workedIntensively: Boolean?,
  val penaltyMinutes: Long?,
  val minutesCredited: Long?,
  @Enumerated(EnumType.STRING)
  val workQuality: WorkQuality?,
  @Enumerated(EnumType.STRING)
  val behaviour: Behaviour?,
  val alertActive: Boolean?,
  val sensitive: Boolean?,
) {
  @PreUpdate
  fun preUpdate(): Unit = throw UnsupportedOperationException("This entity can't be updated")

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AppointmentEventEntity) return false
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "AppointmentEventEntity(id=$id, eventType=$eventType, deliusAppointmentId='$deliusAppointmentId')"

  /**
   * Used when determining if an update has already been applied
   *
   * This function should be updated if a new JPA relationship is added to this entity,
   * adding an explicit comparison and excluding it from the call to [reflectionCompare].
   * For an example see contactOutcome
   */
  fun isLogicallyIdentical(other: AppointmentEventEntity): Boolean {
    if (this.contactOutcome?.id != other.contactOutcome?.id) return false

    val excludeFields = listOf(
      "id",
      "createdAt",
      "contactOutcome",
    )

    return reflectionCompare(
      this,
      other,
      excludeFields,
    ) == 0
  }

  companion object
}

enum class AppointmentEventTriggerType {
  USER,
  SCHEDULING,
  ETE_COURSE_COMPLETION,
}

enum class AppointmentEventType {
  UPDATE,
  CREATE,
}
