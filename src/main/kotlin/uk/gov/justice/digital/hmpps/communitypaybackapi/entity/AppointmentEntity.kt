package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.proxy.HibernateProxy
import java.time.LocalDate
import java.util.UUID

/**
 * Tracks all appointments that community payback has either updated or created
 *
 * This entity only includes immutable aspects of an appointments, because users
 * can still make modifications in NDelius, which would make this data stale
 *
 * The primary purpose of this table is to maintain the mapping from the
 * community payback appointment IDs to the NDelius appointment ID
 *
 * At some point in the future we will ideally only be using our IDs when
 * communicating with community-payback-and-delius. At this point all
 * appointments in NDelius would need a community payback ID assigning
 */
@Entity
@Table(name = "appointments")
data class AppointmentEntity(
  /**
   * The key identifier for this appointment in community payback
   *
   * This is also persisted in NDelius as the 'external reference'
   */
  @Id
  val id: UUID,
  /**
   * The key identifier for this appointment in NDelius
   */
  val deliusId: Long,
  val crn: String,
  val deliusEventNumber: Long,
  val createdByCommunityPayback: Boolean,
  /**
   * If date is updated directly in NDelius (which is allowed in ND after an outcome is set),
   * this value may be stale. Carefully consider the context of it's usage, and update from
   * NDelius is absolute accuracy is required.
   */
  var date: LocalDate,
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
    other as AppointmentEntity

    return id == other.id
  }

  @Suppress("USELESS_IS_CHECK")
  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "Appointments(id=$id)"

  companion object {
    fun generateId(): UUID = UUID.randomUUID()
  }
}
