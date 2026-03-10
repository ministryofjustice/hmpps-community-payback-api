package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun ProjectTypeDto.Companion.valid() = ProjectTypeDto(
  id = UUID.randomUUID(),
  name = String.Companion.random(20),
  code = String.random(5),
  group = ProjectTypeGroupDto.entries.random(),
)
