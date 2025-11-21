package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.proxy.HibernateProxy
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects

@Entity
@Table(name = "session_supervisors")
@IdClass(SessionSupervisorId::class)
data class SessionSupervisorEntity(
  @Id
  val projectCode: String,

  @Id
  val day: LocalDate,

  var supervisorCode: String,

  var allocatedByUsername: String,

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
      if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as SessionSupervisorEntity

    return projectCode == other.projectCode && day == other.day
  }

  override fun hashCode(): Int = Objects.hash(projectCode, day)

  override fun toString(): String = "SessionSupervisorEntity(projectCode='$projectCode', day='$day', supervisorCode='$supervisorCode')"

  companion object
}

@Embeddable
data class SessionSupervisorId(
  val projectCode: String,
  val day: LocalDate,
)
