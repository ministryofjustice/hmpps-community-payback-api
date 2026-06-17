package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

class AdjustmentMappersTest {

  @Nested
  inner class NDAdjustmentToAdjustmentDto {
    @ParameterizedTest
    @EnumSource
    fun success(type: NDAdjustmentType) {
      val adjustment = NDAdjustment.valid().copy(type = type)

      val expectedAmount = when (type) {
        NDAdjustmentType.POSITIVE -> Duration.ofMinutes(adjustment.minutes.toLong())
        NDAdjustmentType.NEGATIVE -> Duration.ofMinutes(-adjustment.minutes.toLong())
      }

      val result = adjustment.toDto()

      assertThat(result.deliusId).isEqualTo(adjustment.id)
      assertThat(result.id).isEqualTo(adjustment.reference)
      assertThat(result.date).isEqualTo(adjustment.date)
      assertThat(result.reason).isEqualTo(adjustment.reason.name)
      assertThat(result.reasonCode).isEqualTo(adjustment.reason.code)
      assertThat(result.amount).isEqualTo(expectedAmount)
    }
  }

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
      val id = UUID.randomUUID()
      val result = CreateAdjustmentDto.valid().copy(
        type = typeDto,
        minutes = 5,
      ).toNDAdjustmentRequest(
        crn = "CRN888",
        deliusEventNumber = 5,
        reason = AdjustmentReasonEntity.valid().copy(deliusCode = "REASON1"),
        reference = id,
        dateOfAdjustment = LocalDate.of(2014, 11, 4),
      )

      assertThat(result.crn).isEqualTo("CRN888")
      assertThat(result.eventNumber).isEqualTo(5)
      assertThat(result.type).isEqualTo(expectedUpstreamType)
      assertThat(result.date).isEqualTo(LocalDate.of(2014, 11, 4))
      assertThat(result.reason).isEqualTo("REASON1")
      assertThat(result.minutes).isEqualTo(5)
      assertThat(result.reference).isEqualTo(id)
    }
  }
}
