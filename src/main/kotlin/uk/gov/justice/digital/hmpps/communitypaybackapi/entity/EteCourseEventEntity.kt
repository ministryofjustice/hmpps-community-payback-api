package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.proxy.HibernateProxy
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ete_course_events")
data class EteCourseEventEntity(
  @Id
  val id: UUID,

  val crn: String,

  val courseName: String,

  val totalTimeMinutes: Long,

  val attempts: Int,

  @Enumerated(EnumType.STRING)
  val status: EteCourseEventStatus,

  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),

  @UpdateTimestamp
  val updatedAt: OffsetDateTime = OffsetDateTime.now(),
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
      if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as EteCourseEventEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

  override fun toString(): String = "EteCourseEventEntity(id=$id)"
}

enum class EteCourseEventStatus(
  val messageType: EducationCourseCompletionStatus,
) {
  COMPLETED(EducationCourseCompletionStatus.Completed),
  FAILED(EducationCourseCompletionStatus.Failed),
  ;

  companion object {
    fun fromMessage(messageType: EducationCourseCompletionStatus) = entries.first { it.messageType == messageType }
  }
}
