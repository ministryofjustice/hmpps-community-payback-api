package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

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
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentType
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "adjustments")
class AdjustmentEntity(
  @Id
  val id: UUID,
  var deliusAdjustmentId: Long,
  var crn: String,
  var deliusEventNumber: Int,
  @Enumerated(EnumType.STRING)
  var adjustmentType: NDAdjustmentType,
  var adjustmentDate: LocalDate,
  var reasonCode: String,
  var reasonName: String,
  var minutes: Int,
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "adjustment_reason_id")
  var adjustmentReason: AdjustmentReasonEntity? = null,
  @CreationTimestamp
  val createdAt: OffsetDateTime? = null,
  @UpdateTimestamp
  val updatedAt: OffsetDateTime? = null,
)
