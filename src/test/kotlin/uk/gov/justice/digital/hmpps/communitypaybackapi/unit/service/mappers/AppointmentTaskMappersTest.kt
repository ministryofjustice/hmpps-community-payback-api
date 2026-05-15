package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.validPending
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentTaskMappers

@ExtendWith(MockKExtension::class)
class AppointmentTaskMappersTest {

  @InjectMockKs
  lateinit var appointmentTaskMappers: AppointmentTaskMappers

  @Nested
  inner class AppointmentTaskEntityToDTO {
    @Test
    fun `should map task with full appointment when isLimited is false`() {
      val projectType = ProjectTypeEntity.valid()
      val appointment = AppointmentEntity.valid().copy(firstName = String.random(8), lastName = String.random(8), projectType = projectType)
      val task = AppointmentTaskEntity.validPending().copy(appointment = appointment)
      val appointmentSummaryDto = AppointmentSummaryDto.valid().copy(id = appointment.deliusId)

      val result = appointmentTaskMappers.toDto(task = task, isLimited = false, appointment = appointmentSummaryDto)

      assertThat(result.taskId).isEqualTo(task.id)
      assertThat(result.appointment).isEqualTo(appointmentSummaryDto)
      assertThat(result.offender).isNotNull
      assertThat(result.offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
      val offender = result.offender as OffenderDto.OffenderFullDto
      assertThat(offender.crn).isEqualTo(appointment.crn)
      assertThat(offender.forename).isEqualTo(appointment.firstName)
      assertThat(offender.surname).isEqualTo(appointment.lastName)
      assertThat(result.date).isEqualTo(appointment.date)
      assertThat(result.projectTypeName).isEqualTo(projectType.name)
    }

    @Test
    fun `should map task with partial appointment when isLimited is false`() {
      val task = AppointmentTaskEntity.validPending()
      val appointmentSummaryDto = AppointmentSummaryDto.valid()

      val result = appointmentTaskMappers.toDto(task = task, isLimited = false, appointment = appointmentSummaryDto)

      assertThat(result.taskId).isEqualTo(task.id)
      assertThat(result.appointment).isEqualTo(appointmentSummaryDto)
      assertThat(result.offender).isNotNull
      assertThat(result.offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
      val offender = result.offender as OffenderDto.OffenderFullDto
      assertThat(offender.crn).isEqualTo(task.appointment.crn)
      assertThat(offender.forename).isNull()
      assertThat(offender.surname).isNull()
      assertThat(result.date).isEqualTo(task.appointment.date)
      assertThat(result.projectTypeName).isNull()
    }

    @Test
    fun `should map task with full appointment when isLimited is true`() {
      val projectType = ProjectTypeEntity.valid()
      val appointment = AppointmentEntity.valid().copy(firstName = String.random(8), lastName = String.random(8), projectType = projectType)
      val task = AppointmentTaskEntity.validPending().copy(appointment = appointment)
      val appointmentSummaryDto = AppointmentSummaryDto.valid().copy(id = appointment.deliusId)

      val result = appointmentTaskMappers.toDto(task = task, isLimited = true, appointment = appointmentSummaryDto)

      assertThat(result.taskId).isEqualTo(task.id)
      assertThat(result.appointment).isEqualTo(appointmentSummaryDto)
      assertThat(result.offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
      val offender = result.offender as OffenderDto.OffenderLimitedDto
      assertThat(offender.crn).isEqualTo(appointment.crn)
      assertThat(result.date).isEqualTo(appointment.date)
      assertThat(result.projectTypeName).isEqualTo(projectType.name)
    }

    @Test
    fun `should map task with partial appointment when isLimited is true`() {
      val task = AppointmentTaskEntity.validPending()
      val appointmentSummaryDto = AppointmentSummaryDto.valid()

      val result = appointmentTaskMappers.toDto(task = task, isLimited = true, appointment = appointmentSummaryDto)

      assertThat(result.taskId).isEqualTo(task.id)
      assertThat(result.appointment).isEqualTo(appointmentSummaryDto)
      assertThat(result.offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
      val offender = result.offender as OffenderDto.OffenderLimitedDto
      assertThat(offender.crn).isEqualTo(task.appointment.crn)
      assertThat(result.date).isEqualTo(task.appointment.date)
      assertThat(result.projectTypeName).isNull()
    }
  }
}
