package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.time.OffsetDateTime
import java.util.UUID

fun AppointmentTaskEntity.Companion.valid() = AppointmentTaskEntity(
  id = UUID.randomUUID(),
  appointment = AppointmentEntity.valid(),
  taskType = AppointmentTaskType.entries.random(),
  taskStatus = AppointmentTaskStatus.entries.random(),
  decisionMadeAt = OffsetDateTime.now(),
  decisionMadeByUsername = String.random(10),
  decisionDescription = String.random(100),
)

fun AppointmentTaskEntity.Companion.validPending() = AppointmentTaskEntity.valid().copy(
  taskStatus = AppointmentTaskStatus.PENDING,
  decisionMadeAt = null,
  decisionMadeByUsername = null,
  decisionDescription = null,
)

fun AppointmentTaskEntity.persist(ctx: ApplicationContext) = ctx.getBean<AppointmentTaskEntityRepository>().save(this)
