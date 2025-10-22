package uk.gov.justice.digital.hmpps.communitypaybackapi.client

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange

interface ArnsClient {
  @GetExchange("/risks/rosh/{crn}")
  fun rosh(
    @PathVariable crn: String,
  ): AllRoshRisk
}

data class AllRoshRisk(
  val summary: RiskRoshSummary,
)

data class RiskRoshSummary(
  val overallRiskLevel: OverallRiskLevel?,
)

enum class OverallRiskLevel {
  VERY_HIGH,
  HIGH,
  MEDIUM,
  LOW,
}
