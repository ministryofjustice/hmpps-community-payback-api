package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "appointment_tasks")
data class AppointmentTaskEntity(
  @Id
  val id: UUID,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn("appointment_id")
  val appointment: AppointmentEntity,
  @Enumerated(EnumType.STRING)
  val taskType: AppointmentTaskType,
  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),
  @Enumerated(EnumType.STRING)
  var taskStatus: AppointmentTaskStatus,
  var decisionMadeByUsername: String? = null,
  var decisionMadeAt: OffsetDateTime? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AppointmentTaskEntity) return false
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "AppointmentTaskEntity(id=$id, taskType=$taskType, taskStatus=$taskStatus)"

  companion object
}

enum class AppointmentTaskType {
  ADJUSTMENT_TRAVEL_TIME,
}

enum class AppointmentTaskStatus {
  PENDING,
  COMPLETE,
}
