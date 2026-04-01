package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.proxy.HibernateProxy
import java.time.OffsetDateTime

@Entity
@Table(name = "form_cache")
@IdClass(FormCacheId::class)
data class FormCacheEntity(
  @Id
  val formId: String,

  @Id
  val formType: String,

  var formData: String,

  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),

  var updatedAt: OffsetDateTime = OffsetDateTime.now(),
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
    other as FormCacheEntity

    return formId == other.formId
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "FormCacheEntity(formId='$formId', formType='$formType')"

  companion object
}

@Embeddable
data class FormCacheId(
  val formId: String,
  val formType: String,
)
