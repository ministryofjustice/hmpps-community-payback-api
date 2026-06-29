package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.proxy.HibernateProxy
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "office_upw_team_mappings")
data class OfficeUpwTeamMappingEntity(
  @Id
  val id: UUID,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "community_campus_pdu_id")
  val pdu: CommunityCampusPduEntity,

  val office: String,

  val teamCode: String?,

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
    other as OfficeUpwTeamMappingEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "OfficeUpwTeamMappingEntity(id=$id)"

  companion object
}
