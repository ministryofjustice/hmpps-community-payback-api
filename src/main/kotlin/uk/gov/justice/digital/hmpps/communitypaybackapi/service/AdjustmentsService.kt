package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest

@Service
class AdjustmentsService(
  private val offenderService: OffenderService,
  private val adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {

  fun createAdjustment(
    crn: String,
    deliusEventNumber: Int,
    createAdjustment: CreateAdjustmentDto,
    username: String,
  ) {
    val reason = adjustmentReasonEntityRepository.findByIdOrNull(createAdjustment.adjustmentReasonId)
      ?: throw NotFoundException("Adjustment Reason", createAdjustment.adjustmentReasonId.toString())

    offenderService.ensureUnpaidWorkDetailsExist(crn, deliusEventNumber, username)
    val requestedMinutes = createAdjustment.minutes

    val maxMinutesAllowed = reason.maxMinutesAllowed
    if (requestedMinutes > maxMinutesAllowed) {
      throw BadRequestException("Requested adjustment minutes $requestedMinutes exceeds the maximum of $maxMinutesAllowed minutes allowed for adjustments with reason '${reason.name}'")
    }

    communityPaybackAndDeliusClient.postAdjustments(
      username,
      listOf(
        createAdjustment.toNDAdjustmentRequest(
          crn = crn,
          deliusEventNumber = deliusEventNumber,
          reason = reason,
        ),
      ),
    )
  }
}
