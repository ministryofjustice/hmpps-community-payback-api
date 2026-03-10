package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun ContactOutcomeDto.Companion.valid() = ContactOutcomeDto(
  id = UUID.randomUUID(),
  name = String.Companion.random(),
  code = String.random(5),
  enforceable = Boolean.random(),
  attended = Boolean.random(),
  availableToSupervisors = Boolean.random(),
  willAlertEnforcementDiary = Boolean.random(),
)
