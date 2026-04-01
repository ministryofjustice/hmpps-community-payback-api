package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.proxy.HibernateProxy
import java.time.OffsetDateTime
import java.util.UUID

@Immutable
@Entity
@Table(name = "enforcement_actions")
data class EnforcementActionEntity(

  @Id
  val id: UUID,
  val code: String,
  val name: String,
  val respondByDateRequired: Boolean,
  @CreationTimestamp
  val createdAt: OffsetDateTime? = null,
  @UpdateTimestamp
  val updatedAt: OffsetDateTime? = null,
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
    other as EnforcementActionEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "EnforcementAction(id=$id, code='$code', name='$name', respondByDateRequired=$respondByDateRequired)"

  companion object
}
