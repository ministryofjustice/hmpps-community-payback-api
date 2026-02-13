package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.UpdateTimestamp
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeGroupDto
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
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "contact_outcomes_groups")
  @Column(name = "contact_outcome_group")
  @Enumerated(EnumType.STRING)
  val groups: List<ContactOutcomeGroup>,
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

  companion object {
    const val ATTENDED_COMPLIED_OUTCOME_CODE = "ATTC"
  }
}

enum class ContactOutcomeGroup {
  AVAILABLE_TO_ADMIN,
  ;

  companion object {
    fun fromDto(dto: ContactOutcomeGroupDto) = when (dto) {
      ContactOutcomeGroupDto.AVAILABLE_TO_ADMIN -> AVAILABLE_TO_ADMIN
    }
  }
}
