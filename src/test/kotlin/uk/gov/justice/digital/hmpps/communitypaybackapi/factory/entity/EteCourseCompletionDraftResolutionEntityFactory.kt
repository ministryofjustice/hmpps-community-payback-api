package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun EteCourseCompletionDraftResolutionEntity.Companion.valid() = EteCourseCompletionDraftResolutionEntity(
  id = UUID.randomUUID(),
  eteCourseCompletionEvent = EteCourseCompletionEventEntity.valid(),
  crn = String.random(7),
)
