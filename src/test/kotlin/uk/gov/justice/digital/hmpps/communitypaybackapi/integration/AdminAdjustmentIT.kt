package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.persist
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventAsserter
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentIdGenerator
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService
import java.time.LocalDate
import java.util.UUID

class AdminAdjustmentIT : IntegrationTestBase() {

  @Autowired
  lateinit var appointmentTaskEntityRepository: AppointmentTaskEntityRepository

  @Autowired
  lateinit var domainEventAsserter: DomainEventAsserter

  @MockitoSpyBean
  lateinit var adjustmentService: AdjustmentService

  @MockitoSpyBean
  lateinit var adjustmentIdGenerator: AdjustmentIdGenerator

  @Autowired
  lateinit var adjustmentEventEntityRepository: AdjustmentEventEntityRepository

  companion object {
    const val CRN = "X123456"
    const val DELIUS_EVENT_NUMBER = 92
  }

  @Nested
  @DisplayName("GET /admin/offenders/{crn}/unpaid-work-details/{deliusEventNumber}/adjustments")
  inner class GetAdjustments {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with list of adjustments`() {
      val adjustments = listOf(
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 8, 8)),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 7, 7)),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 6, 6)),
      )

      CommunityPaybackAndDeliusMockServer.setupGetAdjustmentsResponse(
        CRN,
        DELIUS_EVENT_NUMBER,
        adjustments,
      )

      val result = webTestClient.get()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<AdjustmentDto>>()

      assertThat(result.page.number).isEqualTo(0)
      assertThat(result.page.size).isEqualTo(10)
      assertThat(result.page.totalPages).isEqualTo(1)
      assertThat(result.page.totalElements).isEqualTo(3)
      assertThat(result.content[0].id).isEqualTo(adjustments[0].reference)
      assertThat(result.content[1].id).isEqualTo(adjustments[1].reference)
      assertThat(result.content[2].id).isEqualTo(adjustments[2].reference)
    }

    @Test
    fun `should perform pagination and sorting`() {
      val adjustments = listOf(
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 8, 8)),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 7, 7)),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 6, 6)),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 5, 5)),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 4, 4)),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 3, 3)),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 2, 2)),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 1, 1)),
      )

      CommunityPaybackAndDeliusMockServer.setupGetAdjustmentsResponse(
        CRN,
        DELIUS_EVENT_NUMBER,
        adjustments,
      )

      val result = webTestClient.get()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments?page=1&size=3&sort=date,asc")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<AdjustmentDto>>()

      assertThat(result.page.number).isEqualTo(1)
      assertThat(result.page.size).isEqualTo(3)
      assertThat(result.page.totalPages).isEqualTo(3)
      assertThat(result.page.totalElements).isEqualTo(8)
      assertThat(result.content[0].id).isEqualTo(adjustments[4].reference)
      assertThat(result.content[1].id).isEqualTo(adjustments[3].reference)
      assertThat(result.content[2].id).isEqualTo(adjustments[2].reference)
    }
  }

  @Nested
  @DisplayName("POST /admin/offenders/{crn}/unpaid-work-details/{deliusEventNumber}/adjustments")
  inner class PostAdjustment {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.post()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateAdjustmentDto.valid(ctx))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.post()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateAdjustmentDto.valid(ctx))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.post()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateAdjustmentDto.valid(ctx))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should create an adjustment upstream, raise a domain event and close related task`() {
      val appointment = AppointmentEntity.valid().copy(crn = CRN, deliusEventNumber = DELIUS_EVENT_NUMBER).persist(ctx)
      val task = AppointmentTaskEntity.valid().copy(appointment = appointment).persist(ctx)
      doReturn(task.id).whenever(adjustmentIdGenerator).generateId(any<CreateAdjustmentDto>())

      setupGetUpwDetailsResponse()
      CommunityPaybackAndDeliusMockServer.setupPostAdjustmentResponse(username = "theusername")
      CommunityPaybackAndDeliusMockServer.setupGetAdjustmentResponse(task.id, NDAdjustment.valid())

      callCreateAdjustment(
        request = CreateAdjustmentDto.valid(ctx).copy(taskId = task.id, appointmentId = appointment.id),
        expectedStatus = 201,
      )

      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = "theusername")

      domainEventAsserter.assertEventCount("community-payback.adjustment.created", 1)
      assertThat(appointmentTaskEntityRepository.findByIdOrNull(task.id)!!.taskStatus).isEqualTo(AppointmentTaskStatus.COMPLETE)
    }

    @Test
    fun `Rollback on unexpected request failure, ensuring previously created adjustments aren't rolled back too`() {
      val appointment = AppointmentEntity.valid().copy(crn = CRN, deliusEventNumber = DELIUS_EVENT_NUMBER).persist(ctx)
      val task = AppointmentTaskEntity.valid().copy(appointment = appointment).persist(ctx)
      doReturn(task.id).whenever(adjustmentIdGenerator).generateId(any<CreateAdjustmentDto>())

      setupGetUpwDetailsResponse()
      CommunityPaybackAndDeliusMockServer.setupPostAdjustmentResponse(username = "theusername", adjustmentId = 25L)
      CommunityPaybackAndDeliusMockServer.setupGetAdjustmentResponse(task.id, NDAdjustment.valid())

      // successful request
      callCreateAdjustment(
        request = CreateAdjustmentDto.valid(ctx).copy(taskId = task.id, appointmentId = appointment.id),
        expectedStatus = 201,
      )
      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = "theusername", count = 1)
      // preemptive attempt to delete orphaned adjustment with the same reference
      CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(reference = task.id, count = 1)

      // setup request that fails after adjustment is created
      doAnswer { invocation ->
        invocation.callRealMethod()
        error("Test-managed exception used to test rollback behaviour")
      }.`when`(adjustmentService).createAdjustment(any(), any(), any())

      CommunityPaybackAndDeliusMockServer.setupDeleteAdjustmentResponse(task.id)
      CommunityPaybackAndDeliusMockServer.resetDeleteAdjustmentRequestCount(task.id)

      callCreateAdjustment(
        request = CreateAdjustmentDto.valid(ctx).copy(taskId = task.id, appointmentId = appointment.id),
        expectedStatus = 500,
      )

      // ensure both creations worked, but only one was deleted
      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = "theusername", count = 2)
      // one delete for an orphaned adjustment and one for the actual rollback
      CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(reference = task.id, count = 2)

      // only 1 domain event is published because the second transaction is rolled back
      domainEventAsserter.assertEventCount("community-payback.adjustment.created", 1)
    }

    private fun setupGetUpwDetailsResponse() {
      CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(
        crn = CRN,
        case = NDCaseSummary.valid(),
        unpaidWorkDetails = listOf(
          NDUpwDetails.valid().copy(
            eventNumber = DELIUS_EVENT_NUMBER,
            requiredMinutes = 1000,
            completedMinutes = 0,
            adjustments = 0,
          ),
        ),
        username = "theusername",
      )
    }

    private fun callCreateAdjustment(
      request: CreateAdjustmentDto,
      expectedStatus: Int = 200,
    ) {
      webTestClient.post()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isEqualTo(expectedStatus)
    }
  }

  @Nested
  @DisplayName("DELETE /admin/adjustments/{communityPaybackId}")
  inner class DeleteAdjustment {

    val adjustmentId: UUID = UUID.randomUUID()

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.delete()
        .uri("/admin/adjustments/$adjustmentId")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.delete()
        .uri("/admin/adjustments/$adjustmentId")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.delete()
        .uri("/admin/adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should delete the adjustment upstream and raise a domain event`() {
      adjustmentEventEntityRepository.save(
        AdjustmentEventEntity.valid(ctx).copy(id = adjustmentId, eventType = AdjustmentEventType.CREATE),
      )

      CommunityPaybackAndDeliusMockServer.setupDeleteAdjustmentResponse(reference = adjustmentId)

      webTestClient.delete()
        .uri("/admin/adjustments/$adjustmentId")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isNoContent

      CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(reference = adjustmentId, count = 1)

      domainEventAsserter.assertEventCount("community-payback.adjustment.deleted", 1)
    }

    @Test
    fun `Should return a 404 Not Found response when adjustment not found`() {
      CommunityPaybackAndDeliusMockServer.setupDeleteAdjustmentResponse(reference = adjustmentId)

      webTestClient.delete()
        .uri("/admin/adjustments/$adjustmentId")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isNotFound

      CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(reference = adjustmentId, count = 0)

      domainEventAsserter.assertEventCount("community-payback.adjustment.deleted", 0)
    }
  }
}
