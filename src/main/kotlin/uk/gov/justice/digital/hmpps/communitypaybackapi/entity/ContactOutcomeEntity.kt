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
import org.hibernate.proxy.HibernateProxy
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
  @Suppress("USELESS_IS_CHECK")
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    val oEffectiveClass =
      if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
    val thisEffectiveClass =
      this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass ?: this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as ContactOutcomeEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "ContactOutcomeEntity(id=$id, code='$code', name='$name', enforceable=$enforceable)"

  companion object {
    const val CODE_ATTENDED_COMPLIED = "ATTC"

    fun unknown(code: String): ContactOutcomeEntity = ContactOutcomeEntity(
      id = UUID(0L, 0L),
      code = code,
      name = "Unknown ($code)",
      enforceable = false,
      attended = false,
      groups = emptyList(),
      createdAt = OffsetDateTime.now(),
      updatedAt = OffsetDateTime.now(),
    )
  }
}

enum class ContactOutcomeGroup {
  AVAILABLE_TO_ADMIN,
  AVAILABLE_FOR_ETE,
  ;

  companion object {
    fun fromDto(dto: ContactOutcomeGroupDto) = when (dto) {
      ContactOutcomeGroupDto.AVAILABLE_TO_ADMIN -> AVAILABLE_TO_ADMIN
      ContactOutcomeGroupDto.AVAILABLE_FOR_ETE -> AVAILABLE_FOR_ETE
    }
  }
}
