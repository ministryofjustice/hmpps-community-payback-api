package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.proxy.HibernateProxy
import java.util.UUID

@Immutable
@Entity
@Table(name = "adjustment_reasons")
data class AdjustmentReasonEntity(
  @Id
  val id: UUID,
  val deliusCode: String,
  val name: String,
  val maxMinutesAllowed: Int,
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
    other as AdjustmentReasonEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "AdjustmentReason(id=$id, code='$deliusCode', name='$name'"

  companion object
}
