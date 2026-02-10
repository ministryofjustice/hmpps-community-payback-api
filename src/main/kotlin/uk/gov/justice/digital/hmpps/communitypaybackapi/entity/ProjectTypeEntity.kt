package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.UpdateTimestamp
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
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ProjectTypeEntity) return false
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "ProjectType(id=$id, code='$code', name='$name')"

  companion object
}

enum class ProjectTypeGroup {
  GROUP,
  INDIVIDUAL,
  INDUCTION,
  ;

  companion object {
    fun fromDto(projectTypeGroupDto: ProjectTypeGroupDto) = when (projectTypeGroupDto) {
      ProjectTypeGroupDto.GROUP -> GROUP
      ProjectTypeGroupDto.INDIVIDUAL -> INDIVIDUAL
      ProjectTypeGroupDto.INDUCTION -> INDUCTION
    }
  }
}
