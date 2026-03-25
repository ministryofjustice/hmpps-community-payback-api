package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import java.util.UUID

fun CreateAdjustmentDto.Companion.valid() = CreateAdjustmentDto(
  taskId = UUID.randomUUID(),
  type = CreateAdjustmentTypeDto.entries.random(),
  minutes = Int.random(0, 180),
  dateOfAdjustment = randomLocalDate(),
  adjustmentReasonId = UUID.randomUUID(),
)

fun CreateAdjustmentDto.Companion.valid(ctx: ApplicationContext) = CreateAdjustmentDto.valid().copy(
  adjustmentReasonId = ctx.getBean<AdjustmentReasonEntityRepository>().findAll().minByOrNull { it.name }!!.id,
)
