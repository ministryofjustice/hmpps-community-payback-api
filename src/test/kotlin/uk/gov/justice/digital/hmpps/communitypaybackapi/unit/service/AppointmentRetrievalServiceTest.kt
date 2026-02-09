package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentRetrievalService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory

@ExtendWith(MockKExtension::class)
class AppointmentRetrievalServiceTest {

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var appointmentMappers: AppointmentMappers

  @RelaxedMockK
  private lateinit var contextService: ContextService

  @InjectMockKs
  private lateinit var service: AppointmentRetrievalService

  private companion object {
    const val PROJECT_CODE = "PROJ123"
    const val USERNAME = "mr-user"
  }

  @BeforeEach
  fun setupUsernameContext() {
    every { contextService.getUserName() } returns USERNAME
  }

  @Nested
  inner class GetAppointment {

    @Test
    fun `if appointment not found, throw not found exception`() {
      every {
        communityPaybackAndDeliusClient.getAppointment(
          projectCode = PROJECT_CODE,
          appointmentId = 101L,
          username = USERNAME,
        )
      } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.getAppointment(PROJECT_CODE, 101L)
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Appointment not found for ID 'Project PROJ123, ID 101'")
    }

    @Test
    fun `appointment found`() {
      val appointment = NDAppointment.valid()
      every { communityPaybackAndDeliusClient.getAppointment(PROJECT_CODE, 101L, USERNAME) } returns appointment

      val appointmentDto = AppointmentDto.valid()
      every { appointmentMappers.toDto(appointment) } returns appointmentDto

      val result = service.getAppointment(PROJECT_CODE, 101L)

      assertThat(result).isSameAs(appointmentDto)
    }
  }
}
