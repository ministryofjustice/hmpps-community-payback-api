package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime
import java.util.UUID

@Immutable
@Entity
@Table(name = "enforcement_actions")
data class EnforcementActionEntity(

  @Id
  @GeneratedValue
  val id: UUID,
  val code: String,
  val name: String,
  @CreationTimestamp
  val createdAt: OffsetDateTime? = null,
  @UpdateTimestamp
  val updatedAt: OffsetDateTime? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is EnforcementActionEntity) return false
    if (id == other.id) return true
    return false
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "EnforcementAction(id=$id, code='$code', name='$name')"

  companion object
}
