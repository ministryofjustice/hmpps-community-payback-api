package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun ProjectTypeEntity.Companion.valid() = ProjectTypeEntity(
  id = UUID.randomUUID(),
  code = String.Companion.random(length = 3),
  name = String.random(length = 20),
  projectTypeGroup = ProjectTypeGroup.entries.random(),
)
