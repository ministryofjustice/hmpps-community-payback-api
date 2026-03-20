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
import org.apache.commons.lang3.builder.CompareToBuilder.reflectionCompare
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

  val createdAt: OffsetDateTime = OffsetDateTime.now(),
  val createdByUsername: String,

  val crn: String?,
  /**
   * The following fields are set when resolution is [EteCourseCompletionResolution.CREDIT_TIME]
   */
  val deliusEventNumber: Long? = null,
  val deliusAppointmentId: Long? = null,
  /**
   * true = project created
   * false = project updated
   * null = project not created or updated
   */
  val deliusAppointmentCreated: Boolean? = null,
  val projectCode: String? = null,
  val minutesCredited: Long? = null,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contact_outcome_id", referencedColumnName = "id")
  val contactOutcome: ContactOutcomeEntity? = null,
) {
  /**
   * Used when determining if a resolution has already been applied
   *
   * This function should be updated if a new JPA relationship is added to this entity,
   * adding an explicit comparison and excluding it from the call to [reflectionCompare].
   * For an example see contactOutcome
   */
  fun isLogicallyIdentical(other: EteCourseCompletionEventResolutionEntity): Boolean {
    if (this.eteCourseCompletionEvent.id != other.eteCourseCompletionEvent.id) return false
    if (this.contactOutcome?.id != other.contactOutcome?.id) return false

    val excludeFields = buildList {
      add("id")
      add("createdAt")
      add("createdByUsername")
      if (other.deliusAppointmentCreated == true) {
        add("deliusAppointmentId")
      }
      // ignore because we check relationships manually
      add("eteCourseCompletionEvent")
      add("contactOutcome")
    }

    return reflectionCompare(
      this,
      other,
      excludeFields,
    ) == 0
  }

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
  COURSE_ALREADY_COMPLETED_WITHIN_THRESHOLD,
}
