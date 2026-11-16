package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.Types
import java.util.UUID

@Repository
class AdjustmentEntityUpsertRepository(private val jdbcClient: JdbcClient) {
  fun upsert(adjustment: AdjustmentEntity): UUID {
    // A retried creation can receive a new Delius ID for the same Community Payback reference.
    jdbcClient.sql(
      """
      UPDATE adjustments SET delius_adjustment_id = :deliusId
      WHERE id = :id AND delius_adjustment_id <> :deliusId
        AND NOT EXISTS (SELECT 1 FROM adjustments WHERE delius_adjustment_id = :deliusId)
      """.trimIndent(),
    )
      .param("id", adjustment.id)
      .param("deliusId", adjustment.deliusAdjustmentId)
      .update()

    return jdbcClient.sql(
      """
      INSERT INTO adjustments AS existing (
        id, delius_adjustment_id, crn, delius_event_number, adjustment_type,
        adjustment_date, minutes, adjustment_reason_id, appointment_id
      ) VALUES (
        :id, :deliusId, :crn, :eventNumber, :type, :date, :minutes, :reasonId, :appointmentId
      )
      ON CONFLICT (delius_adjustment_id) DO UPDATE SET
        crn = EXCLUDED.crn,
        delius_event_number = EXCLUDED.delius_event_number,
        adjustment_type = EXCLUDED.adjustment_type,
        adjustment_date = EXCLUDED.adjustment_date,
        minutes = EXCLUDED.minutes,
        adjustment_reason_id = EXCLUDED.adjustment_reason_id,
        appointment_id = COALESCE(EXCLUDED.appointment_id, existing.appointment_id),
        updated_at = CURRENT_TIMESTAMP
      RETURNING id
      """.trimIndent(),
    )
      .param("id", adjustment.id)
      .param("deliusId", adjustment.deliusAdjustmentId)
      .param("crn", adjustment.crn)
      .param("eventNumber", adjustment.deliusEventNumber)
      .param("type", adjustment.adjustmentType.name)
      .param("date", adjustment.adjustmentDate)
      .param("minutes", adjustment.minutes)
      .param("reasonId", adjustment.adjustmentReason?.id, Types.OTHER)
      .param("appointmentId", adjustment.appointment?.id, Types.OTHER)
      .query(UUID::class.java)
      .single()
  }
}
