package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.proxy.HibernateProxy
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ete_course_completion_event_resolutions")
data class EteCourseCompletionEventResolutionEntity(
  @Id
  val id: UUID,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ete_course_completion_event_id", referencedColumnName = "id")
  val eteCourseCompletionEvent: EteCourseCompletionEventEntity,

  @Enumerated(EnumType.STRING)
  val resolution: EteCourseCompletionResolution,

  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),
  val createdByUsername: String,

  /**
   * The following fields are set when resolution is [EteCourseCompletionResolution.CREDIT_TIME]
   */
  val crn: String?,
  val deliusEventNumber: Long?,
  val deliusAppointmentId: Long?,
  val deliusAppointmentCreated: Boolean?,
  val projectCode: String?,
  val minutesCredited: Long?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contact_outcome_id", referencedColumnName = "id")
  val contactOutcome: ContactOutcomeEntity?,
) {
  @PreUpdate
  fun preUpdate(): Unit = throw UnsupportedOperationException("This entity can't be updated")

  @Suppress("USELESS_IS_CHECK")
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    val oEffectiveClass =
      if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
    val thisEffectiveClass =
      this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass ?: this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as EteCourseCompletionEventEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "EteCourseCompletionEventResolutionEntity(id=$id)"

  companion object
}

enum class EteCourseCompletionResolution {
  CREDIT_TIME,
}
