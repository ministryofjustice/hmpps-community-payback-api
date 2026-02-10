package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import java.util.UUID

fun ProjectTypeDto.Companion.valid() = ProjectTypeDto(
  id = UUID.randomUUID(),
  name = String.random(20),
  code = String.random(5),
)
