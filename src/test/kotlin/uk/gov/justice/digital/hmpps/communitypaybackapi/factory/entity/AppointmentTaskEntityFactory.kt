package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskType
import java.util.UUID

fun AppointmentTaskEntity.Companion.valid() = AppointmentTaskEntity(
  id = UUID.randomUUID(),
  appointment = AppointmentEntity.valid(),
  taskType = AppointmentTaskType.entries.random(),
  taskStatus = AppointmentTaskStatus.entries.random(),
)

fun AppointmentTaskEntity.persist(ctx: ApplicationContext) = ctx.getBean<AppointmentTaskEntityRepository>().save(this)
