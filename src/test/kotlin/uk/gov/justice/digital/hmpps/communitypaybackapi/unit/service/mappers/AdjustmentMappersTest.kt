package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest
import java.time.LocalDate

class AdjustmentMappersTest {

  @Nested
  inner class CreateAdjustmentDtoToNDAdjustmentRequest {

    @ParameterizedTest
    @CsvSource(
      "Positive,POSITIVE",
      "Negative,NEGATIVE",
    )
    fun success(
      typeDto: CreateAdjustmentTypeDto,
      expectedUpstreamType: NDAdjustmentType,
    ) {
      val result = CreateAdjustmentDto.valid().copy(
        dateOfAdjustment = LocalDate.of(2014, 11, 4),
        type = typeDto,
        minutes = 5,
      ).toNDAdjustmentRequest(
        crn = "CRN888",
        deliusEventNumber = 5,
        reason = AdjustmentReasonEntity.valid().copy(deliusCode = "REASON1"),
      )

      assertThat(result.crn).isEqualTo("CRN888")
      assertThat(result.eventNumber).isEqualTo(5)
      assertThat(result.type).isEqualTo(expectedUpstreamType)
      assertThat(result.date).isEqualTo(LocalDate.of(2014, 11, 4))
      assertThat(result.reason).isEqualTo("REASON1")
      assertThat(result.minutes).isEqualTo(5)
    }
  }
}
