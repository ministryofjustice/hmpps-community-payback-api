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
import org.hibernate.proxy.HibernateProxy
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
  var decisionDescription: String? = null,
) {
  @Suppress("USELESS_IS_CHECK")
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    val oEffectiveClass =
      if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
    val thisEffectiveClass =
      this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass ?: this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as AppointmentTaskEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

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
