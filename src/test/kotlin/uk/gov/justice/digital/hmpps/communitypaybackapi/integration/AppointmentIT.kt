package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseName
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventListener
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ProjectTypeEntityRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class AppointmentIT : IntegrationTestBase() {

  @Autowired
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @Autowired
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @Autowired
  lateinit var enforcementActionEntityRepository: EnforcementActionEntityRepository

  @Autowired
  lateinit var projectEntityRepository: ProjectTypeEntityRepository

  @Autowired
  lateinit var domainEventListener: DomainEventListener

  @Nested
  @DisplayName("GET /appointment/{appointmentId}")
  inner class GetAppointment {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/appointments/101")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/appointments/101")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/appointments/101")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if an appointment can't be found`() {
      CommunityPaybackAndDeliusMockServer.projectAppointmentNotFound(101L)

      val response = webTestClient.get()
        .uri("/appointments/101")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound()
        .bodyAsObject<ErrorResponse>()

      assertThat(response.userMessage).isEqualTo("No resource found failure: Appointment not found for ID '101'")
    }

    @Test
    fun `Should return existing appointment with offender info`() {
      val id = 101L
      val projectName = "Community Garden Maintenance"
      val projectCode = "CGM101"
      val projectTypeName = "MAINTENANCE"
      val projectTypeCode = "MAINT"
      val crn = "X434334"
      val contactOutcomeId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
      val enforcementActionId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
      val supervisingTeam = "Team Lincoln"
      val date = LocalDate.of(2025, 9, 1)
      val startTime = LocalTime.of(9, 0)
      val endTime = LocalTime.of(17, 0)
      val penaltyTime = LocalTime.of(0, 0)
      val supervisorCode = "CRN1"
      val respondBy = LocalDate.of(2025, 10, 1)
      val hiVisWorn = true
      val workedIntensively = false
      val workQuality = ProjectAppointmentWorkQuality.SATISFACTORY
      val behaviour = ProjectAppointmentBehaviour.SATISFACTORY
      val notes = "This is a test note"

      CommunityPaybackAndDeliusMockServer.projectAppointment(
        ProjectAppointment(
          id = id,
          projectName = projectName,
          projectCode = projectCode,
          projectTypeName = projectTypeName,
          projectTypeCode = projectTypeCode,
          crn = crn,
          supervisingTeam = supervisingTeam,
          date = date,
          startTime = startTime,
          endTime = endTime,
          penaltyTime = penaltyTime,
          supervisorCode = supervisorCode,
          contactOutcomeId = contactOutcomeId,
          enforcementActionId = enforcementActionId,
          respondBy = respondBy,
          hiVisWorn = hiVisWorn,
          workedIntensively = workedIntensively,
          workQuality = workQuality,
          behaviour = behaviour,
          notes = notes,
        ),
      )

      CommunityPaybackAndDeliusMockServer.probationCasesSummaries(
        crns = listOf(crn),
        response = CaseSummaries(
          listOf(
            CaseSummary(crn = crn, name = CaseName("Jeff", "Jeffity")),
          ),
        ),
      )

      val response = webTestClient.get()
        .uri("/appointments/101")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk()
        .bodyAsObject<AppointmentDto>()

      assertThat(response.id).isEqualTo(id)
      assertThat(response.projectName).isEqualTo(projectName)
      assertThat(response.projectCode).isEqualTo(projectCode)
      assertThat(response.date).isEqualTo(date)
      assertThat(response.supervisingTeam).isEqualTo(supervisingTeam)
      assertThat(response.attendanceData?.supervisorOfficerCode).isEqualTo(supervisorCode)
      assertThat(response.attendanceData?.penaltyTime).isEqualTo(penaltyTime)
      assertThat(response.attendanceData?.behaviour).isEqualTo(AppointmentBehaviourDto.SATISFACTORY)
      assertThat(response.attendanceData?.workQuality).isEqualTo(AppointmentWorkQualityDto.SATISFACTORY)
      assertThat(response.attendanceData?.hiVisWorn).isEqualTo(hiVisWorn)
      assertThat(response.attendanceData?.contactOutcomeId).isEqualTo(contactOutcomeId)
      assertThat(response.enforcementData?.enforcementActionId).isEqualTo(enforcementActionId)
      assertThat(response.enforcementData?.respondBy).isEqualTo(respondBy)
      assertThat(response.notes).isEqualTo(notes)
      assertThat(response.offender.crn).isEqualTo(crn)
      assertThat(response.offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
    }
  }

  @Nested
  @DisplayName("PUT /appointments")
  inner class PutAppointmentsEndpoint {

    @BeforeEach
    fun setUp() {
      appointmentOutcomeEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.put()
        .uri("/appointments")
        .bodyValue(UpdateAppointmentOutcomesDto.valid())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.put()
        .uri("/appointments")
        .bodyValue(UpdateAppointmentOutcomesDto.valid())
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.put()
        .uri("/appointments")
        .bodyValue(UpdateAppointmentOutcomesDto.valid())
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 400 if an appointment can't be found`() {
      CommunityPaybackAndDeliusMockServer.projectAppointmentNotFound(1234L)

      val response = webTestClient.put()
        .uri("/appointments")
        .addUiAuthHeader()
        .bodyValue(
          UpdateAppointmentOutcomesDto.valid(
            ids = longArrayOf(1234L),
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest()
        .bodyAsObject<ErrorResponse>()

      assertThat(response.userMessage).isEqualTo("Validation failure: Appointment not found for ID '1234'")
    }

    @Test
    fun `Should persist single update, raising domain events`() {
      CommunityPaybackAndDeliusMockServer.projectAppointment(ProjectAppointment.valid().copy(id = 1L))

      val contactOutcomeEntity = contactOutcomeEntityRepository.findAll().first()
      val enforcementOutcomeEntity = enforcementActionEntityRepository.findAll().first()

      webTestClient.put()
        .uri("/appointments")
        .addUiAuthHeader()
        .bodyValue(
          UpdateAppointmentOutcomesDto.valid(
            ids = longArrayOf(1L),
            contactOutcomeId = contactOutcomeEntity.id,
            enforcementActionId = enforcementOutcomeEntity.id,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk()

      val persistedId = appointmentOutcomeEntityRepository.findAll()[0].id

      val domainEvent = domainEventListener.blockForDomainEventOfType("community-payback.appointment.outcome")
      assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/domain-event-details/appointment-outcome/$persistedId")
    }

    @Test
    fun `should persist multiple updates, raising domain events`() {
      CommunityPaybackAndDeliusMockServer.projectAppointment(ProjectAppointment.valid().copy(id = 1L))
      CommunityPaybackAndDeliusMockServer.projectAppointment(ProjectAppointment.valid().copy(id = 2L))
      CommunityPaybackAndDeliusMockServer.projectAppointment(ProjectAppointment.valid().copy(id = 3L))

      val contactOutcomeEntity = contactOutcomeEntityRepository.findAll().first()
      val enforcementOutcomeEntity = enforcementActionEntityRepository.findAll().first()

      webTestClient.put()
        .uri("/appointments")
        .addUiAuthHeader()
        .bodyValue(
          UpdateAppointmentOutcomesDto.valid(
            ids = longArrayOf(1L, 2L, 3L),
            contactOutcomeId = contactOutcomeEntity.id,
            enforcementActionId = enforcementOutcomeEntity.id,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk()

      assertThat(
        appointmentOutcomeEntityRepository.findAll()
          .map { it.appointmentDeliusId },
      ).containsExactlyInAnyOrder(1L, 2L, 3L)

      domainEventListener.assertEventCount("community-payback.appointment.outcome", 3)
    }
  }
}
