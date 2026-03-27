package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest

@Service
class AdjustmentService(
  private val adjustmentValidationService: AdjustmentValidationService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {

  fun createAdjustment(
    upwDetailsId: UnpaidWorkDetailsIdDto,
    createAdjustment: CreateAdjustmentDto,
    username: String,
  ) {
    val validatedAdjustment = adjustmentValidationService.validateCreate(createAdjustment, upwDetailsId, username)

    val (crn, deliusEventNumber) = upwDetailsId

    communityPaybackAndDeliusClient.postAdjustments(
      username,
      listOf(
        createAdjustment.toNDAdjustmentRequest(
          crn = crn,
          deliusEventNumber = deliusEventNumber,
          reason = validatedAdjustment.reason,
        ),
      ),
    )
  }
}
