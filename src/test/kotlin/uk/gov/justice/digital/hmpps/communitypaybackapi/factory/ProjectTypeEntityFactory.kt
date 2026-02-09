package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup
import java.util.UUID

fun ProjectTypeEntity.Companion.valid() = ProjectTypeEntity(
  id = UUID.randomUUID(),
  code = String.random(length = 3),
  name = String.random(length = 20),
  projectTypeGroup = ProjectTypeGroup.entries.random(),
)
