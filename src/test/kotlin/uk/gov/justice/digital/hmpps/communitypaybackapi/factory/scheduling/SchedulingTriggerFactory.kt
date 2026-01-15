package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingTriggerType

fun SchedulingTrigger.Companion.valid() = SchedulingTrigger(
  type = SchedulingTriggerType.AppointmentChange,
  description = String.random(50),
)
