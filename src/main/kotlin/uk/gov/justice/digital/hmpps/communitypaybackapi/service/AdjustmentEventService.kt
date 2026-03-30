package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventEntityFactory.CreateAdjustmentEventDetails

@Service
class AdjustmentEventService(
  private val adjustmentEventEntityRepository: AdjustmentEventEntityRepository,
  private val adjustmentEventEntityFactory: AdjustmentEventEntityFactory,
) {

  fun publishCreateEvent(details: CreateAdjustmentEventDetails) {
    adjustmentEventEntityRepository.save(
      adjustmentEventEntityFactory.buildAdjustmentCreated(details),
    )
  }
}
