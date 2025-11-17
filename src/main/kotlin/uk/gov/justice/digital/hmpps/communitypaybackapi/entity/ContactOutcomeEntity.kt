package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime
import java.util.UUID

@Immutable
@Entity
@Table(name = "contact_outcomes")
data class ContactOutcomeEntity(

  @Id
  val id: UUID,
  @Column(unique = true)
  val code: String,
  val name: String,
  val enforceable: Boolean,
  val attended: Boolean,
  val availableToSupervisors: Boolean,
  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),
  @UpdateTimestamp
  val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ContactOutcomeEntity) return false
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "ContactOutcomeEntity(id=$id, code='$code', name='$name', enforceable=$enforceable)"

  companion object
}
