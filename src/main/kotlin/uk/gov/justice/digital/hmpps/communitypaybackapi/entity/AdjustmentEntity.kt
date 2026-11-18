package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "adjustments")
data class AdjustmentEntity(
  @Id
  val id: UUID,
  val deliusId: Long,
  val crn: String,
  val deliusEventNumber: Int,
  @Enumerated(EnumType.STRING)
  val type: AdjustmentType,
  val minutes: Int,
  val date: LocalDate,
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn("reason_id", nullable = false)
  val reason: AdjustmentReasonEntity,
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn("appointment_id")
  val appointment: AppointmentEntity?,
) {
  @CreationTimestamp
  @Column(updatable = false)
  val createdAt: OffsetDateTime = OffsetDateTime.now()

  @UpdateTimestamp
  val updatedAt: OffsetDateTime = OffsetDateTime.now()
}

enum class AdjustmentType {
  POSITIVE,
  NEGATIVE,
}
