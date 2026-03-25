package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.proxy.HibernateProxy
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventStatusDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ete_course_completion_events")
data class EteCourseCompletionEventEntity(
  @Id
  val id: UUID,

  @Version
  var version: Long = 1,

  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
  /**
   * The region entered in community campus. Note that for filtering on region the
   * provider code on the associated pdu will be used instead.
   */
  val region: String,
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn("community_campus_pdu_id")
  val pdu: CommunityCampusPduEntity,
  val office: String,
  val email: String,

  val courseName: String,
  val courseType: String,
  val provider: String,

  @Column(name = "completion_date_time")
  val completionDateTime: OffsetDateTime,

  @Enumerated(EnumType.STRING)
  val status: EteCourseCompletionEventStatus,

  val totalTimeMinutes: Long,

  val expectedTimeMinutes: Long,

  val attempts: Int?,

  val externalReference: String,

  @OneToOne(mappedBy = "eteCourseCompletionEvent", cascade = [CascadeType.ALL])
  var resolution: EteCourseCompletionEventResolutionEntity? = null,

  val receivedAt: OffsetDateTime,

  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),

  @UpdateTimestamp
  val updatedAt: OffsetDateTime = OffsetDateTime.now(),
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
    other as EteCourseCompletionEventEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "EteCourseEventEntity(id=$id)"

  companion object
}

enum class EteCourseCompletionEventStatus(
  val messageType: EducationCourseCompletionStatus,
  val dtoType: EteCourseCompletionEventStatusDto,
) {
  PASSED(
    messageType = EducationCourseCompletionStatus.Completed,
    dtoType = EteCourseCompletionEventStatusDto.Passed,
  ),
  FAILED(
    messageType = EducationCourseCompletionStatus.Failed,
    dtoType = EteCourseCompletionEventStatusDto.Failed,
  ),
  ;

  companion object {
    fun fromMessage(messageType: EducationCourseCompletionStatus) = entries.first { it.messageType == messageType }
  }
}
