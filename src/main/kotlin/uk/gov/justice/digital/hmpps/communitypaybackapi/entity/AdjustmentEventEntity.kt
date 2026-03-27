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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "adjustment_events")
data class AdjustmentEventEntity(
  @Id
  val id: UUID,
  @Enumerated(EnumType.STRING)
  val eventType: AdjustmentEventType,
  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),
  val triggeredAt: OffsetDateTime,
  @Enumerated(EnumType.STRING)
  val triggerType: AdjustmentEventTriggerType,
  val triggeredBy: String,
  val deliusAdjustmentId: Long,
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn("appointment_id")
  val appointment: AppointmentEntity,
  @Enumerated(EnumType.STRING)
  val adjustmentType: AdjustmentEventAdjustmentType,
  val adjustmentMinutes: Int,
  val adjustmentDate: LocalDate,
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn("adjustment_reason_id")
  val adjustmentReason: AdjustmentReasonEntity,

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
    other as AdjustmentEventEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "AdjustmentEvent(id=$id)"

  companion object
}

enum class AdjustmentEventType {
  CREATE,
}

enum class AdjustmentEventTriggerType {
  APPOINTMENT_TASK,
}

enum class AdjustmentEventAdjustmentType {
  NEGATIVE,
  POSITIVE,
}
