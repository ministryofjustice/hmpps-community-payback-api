package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.proxy.HibernateProxy
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import java.time.OffsetDateTime
import java.util.UUID

@Immutable
@Entity
@Table(name = "project_types")
data class ProjectTypeEntity(
  @Id
  val id: UUID,
  val code: String,
  val name: String,
  @Enumerated(EnumType.STRING)
  val projectTypeGroup: ProjectTypeGroup?,
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
    other as ProjectTypeEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "ProjectType(id=$id, code='$code', name='$name')"

  companion object {
    const val GROUP_PLACEMENT_NATIONAL_PROJECT_CODE = "NP2"
  }
}

enum class ProjectTypeGroup(val travelTimeSupported: Boolean) {
  ETE(travelTimeSupported = false),
  GROUP(travelTimeSupported = true),
  INDIVIDUAL(travelTimeSupported = true),
  INDUCTION(travelTimeSupported = false),
  ;

  companion object {
    fun fromDto(projectTypeGroupDto: ProjectTypeGroupDto) = when (projectTypeGroupDto) {
      ProjectTypeGroupDto.ETE -> ETE
      ProjectTypeGroupDto.GROUP -> GROUP
      ProjectTypeGroupDto.INDIVIDUAL -> INDIVIDUAL
      ProjectTypeGroupDto.INDUCTION -> INDUCTION
    }
  }
}
