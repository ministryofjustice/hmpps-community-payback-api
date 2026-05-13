package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentReasonDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.util.UUID

fun CreateAdjustmentDto.Companion.valid() = CreateAdjustmentDto(
  taskId = UUID.randomUUID(),
  type = CreateAdjustmentTypeDto.entries.random(),
  minutes = Int.random(1, 180),
  dateOfAdjustment = randomLocalDate(),
  adjustmentReason = AdjustmentReasonDto.valid(),
  project = ProjectDto.valid(),
)

fun CreateAdjustmentDto.Companion.valid(ctx: ApplicationContext) = CreateAdjustmentDto.valid().copy(
  adjustmentReason = ctx.getBean<AdjustmentReasonEntityRepository>().findAll().minByOrNull { it.name }!!.toDto(),
)
