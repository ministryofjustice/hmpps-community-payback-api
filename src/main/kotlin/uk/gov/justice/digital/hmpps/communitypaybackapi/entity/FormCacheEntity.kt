package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.time.OffsetDateTime

@Entity
@Table(name = "form_cache")
@IdClass(FormCacheId::class)
data class FormCacheEntity(
  @Id
  val formId: String,

  @Id
  val formType: String,

  val formData: String,

  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),

  @UpdateTimestamp
  val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FormCacheEntity) return false
    return formId == other.formId && formType == other.formType
  }

  override fun hashCode(): Int = 31 * formId.hashCode() + formType.hashCode()

  override fun toString(): String = "FormCacheEntity(formId='$formId', formType='$formType')"

  companion object
}

data class FormCacheId(
  val formId: String = "",
  val formType: String = "",
) : Serializable {
  companion object {
    private const val serialVersionUID: Long = 1L
  }
}
