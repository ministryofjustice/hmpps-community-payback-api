package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.proxy.HibernateProxy
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ete_course_completion_draft_resolutions")
data class EteCourseCompletionDraftResolutionEntity(
  @Id
  val id: UUID,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ete_course_completion_event_id", referencedColumnName = "id")
  val eteCourseCompletionEvent: EteCourseCompletionEventEntity,

  val crn: String?,

  val teamCode: String? = null,

  val projectCode: String? = null,

  val appointmentIdToUpdate: Long? = null,

  val createdAt: OffsetDateTime = OffsetDateTime.now(),
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
    other as EteCourseCompletionDraftResolutionEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "EteCourseCompletionDraftResolutionEntity(id=$id)"

  companion object
}
