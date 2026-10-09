package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.simple.JdbcClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService
import java.time.LocalDate
import java.util.UUID

class AdjustmentPersistenceIT : IntegrationTestBase() {
  @Autowired
  lateinit var service: AdjustmentService

  @Autowired
  lateinit var jdbcClient: JdbcClient

  @Test
  fun `uses the reference as the UUID identity and updates changed values`() {
    val adjustment = NDAdjustment.valid().copy(id = 900001L)
    retrieveAdjustments("X123456", 1, listOf(adjustment))
    val original = findAdjustment(adjustment.reference!!)
    assertThat(original["id"]).isEqualTo(adjustment.reference)
    assertThat(original["minutes"]).isEqualTo(adjustment.minutes)

    jdbcClient.sql("UPDATE adjustments SET updated_at = '2000-01-01' WHERE id = :id")
      .param("id", adjustment.reference).update()
    val beforeUpdate = findAdjustment(adjustment.reference!!)
    val updated = adjustment.copy(
      id = 900004L,
      type = NDAdjustmentType.NEGATIVE,
      date = LocalDate.of(2026, 1, 1),
      reason = NDNameCode("Updated reason", "UPDATED"),
      minutes = adjustment.minutes + 1,
    )
    retrieveAdjustments("X654321", 2, listOf(updated))
    val stored = findAdjustment(adjustment.reference!!)
    assertThat(stored["crn"]).isEqualTo("X654321")
    assertThat(stored["delius_event_number"]).isEqualTo(2)
    assertThat(stored["id"]).isEqualTo(updated.reference)
    assertThat(stored["delius_adjustment_id"]).isEqualTo(updated.id)
    assertThat(stored["adjustment_type"]).isEqualTo(updated.type.name)
    assertThat(stored["adjustment_date"].toString()).isEqualTo(updated.date.toString())
    assertThat(stored["reason_code"]).isEqualTo(updated.reason.code)
    assertThat(stored["reason_name"]).isEqualTo(updated.reason.name)
    assertThat(stored["minutes"]).isEqualTo(updated.minutes)
    assertThat(stored["created_at"]).isEqualTo(original["created_at"])
    assertThat(stored["updated_at"]).isNotEqualTo(beforeUpdate["updated_at"])
  }

  @Test
  fun `links known reasons and refreshes the link when the Delius reason changes`() {
    val adjustment = NDAdjustment.valid().copy(id = 900003L, reason = NDNameCode("Travel Time", "TTX"))
    retrieveAdjustments("X123456", 1, listOf(adjustment))
    val travelTimeId = jdbcClient.sql("SELECT id FROM adjustment_reasons WHERE delius_code = 'TTX'").query(UUID::class.java).single()
    assertThat(findAdjustment(adjustment.reference!!)["adjustment_reason_id"]).isEqualTo(travelTimeId)

    retrieveAdjustments("X123456", 1, listOf(adjustment.copy(reason = NDNameCode("Correction", "E"))))
    val correctionId = jdbcClient.sql("SELECT id FROM adjustment_reasons WHERE delius_code = 'E'").query(UUID::class.java).single()
    assertThat(findAdjustment(adjustment.reference!!)["adjustment_reason_id"]).isEqualTo(correctionId)

    retrieveAdjustments("X123456", 1, listOf(adjustment.copy(reason = NDNameCode("Unknown", "UNKNOWN"))))
    assertThat(findAdjustment(adjustment.reference!!)["adjustment_reason_id"]).isNull()
    assertThat(findAdjustment(adjustment.reference!!)["reason_code"]).isEqualTo("UNKNOWN")
  }

  @Test
  fun `repeated retrieval leaves unchanged adjustments untouched and retains missing adjustments`() {
    val adjustment = NDAdjustment.valid().copy(id = 900002L)
    retrieveAdjustments("X123456", 1, listOf(adjustment))
    jdbcClient.sql("UPDATE adjustments SET updated_at = '2000-01-01' WHERE id = :id")
      .param("id", adjustment.reference).update()
    val original = findAdjustment(adjustment.reference!!)

    retrieveAdjustments("X123456", 1, listOf(adjustment))
    retrieveAdjustments("X123456", 1, emptyList())

    assertThat(findAdjustment(adjustment.reference!!)).isEqualTo(original)
  }

  @Test
  fun `does not persist adjustments without a UUID reference`() {
    val adjustment = NDAdjustment.valid().copy(reference = null)
    retrieveAdjustments("X123456", 1, listOf(adjustment))

    assertThat(jdbcClient.sql("SELECT count(*) FROM adjustments").query(Long::class.java).single()).isZero()
  }

  private fun retrieveAdjustments(crn: String, eventNumber: Int, adjustments: List<NDAdjustment>) {
    CommunityPaybackAndDeliusMockServer.setupGetAdjustmentsResponse(crn, eventNumber, adjustments)
    service.getAdjustments(crn, eventNumber, PageRequest.of(1, 1, Sort.Direction.ASC, "date"))
  }

  private fun findAdjustment(id: UUID): Map<String, Any?> = jdbcClient
    .sql("SELECT * FROM adjustments WHERE id = :id")
    .param("id", id)
    .query().singleRow()
}
