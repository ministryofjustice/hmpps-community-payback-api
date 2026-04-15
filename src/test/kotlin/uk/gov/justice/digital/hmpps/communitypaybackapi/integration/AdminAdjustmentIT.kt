package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.persist
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventAsserter
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService

class AdminAdjustmentIT : IntegrationTestBase() {

  @Autowired
  lateinit var appointmentTaskEntityRepository: AppointmentTaskEntityRepository

  @Autowired
  lateinit var domainEventAsserter: DomainEventAsserter

  @MockitoSpyBean
  lateinit var adjustmentService: AdjustmentService

  companion object {
    const val CRN = "X123456"
    const val DELIUS_EVENT_NUMBER = 92
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

      setupGetUpwDetailsResponse()
      CommunityPaybackAndDeliusMockServer.setupPostAdjustmentResponse(username = "theusername")

      callCreateAdjustment(
        request = CreateAdjustmentDto.valid(ctx).copy(taskId = task.id),
        expectedStatus = 200,
      )

      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = "theusername")

      domainEventAsserter.assertEventCount("community-payback.adjustment.created", 1)
      assertThat(appointmentTaskEntityRepository.findByIdOrNull(task.id)!!.taskStatus).isEqualTo(AppointmentTaskStatus.COMPLETE)
    }

    @Test
    fun `Rollback on unexpected request failure, ensuring previously created adjustments aren't rolled back too`() {
      val appointment = AppointmentEntity.valid().copy(crn = CRN, deliusEventNumber = DELIUS_EVENT_NUMBER).persist(ctx)
      val task = AppointmentTaskEntity.valid().copy(appointment = appointment).persist(ctx)

      setupGetUpwDetailsResponse()
      CommunityPaybackAndDeliusMockServer.setupPostAdjustmentResponse(username = "theusername", adjustmentId = 25L)

      // successful request
      callCreateAdjustment(
        request = CreateAdjustmentDto.valid(ctx).copy(taskId = task.id),
        expectedStatus = 200,
      )
      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = "theusername", count = 1)

      // setup request that fails after adjustment is created
      doAnswer { invocation ->
        invocation.callRealMethod()
        error("Test-managed exception used to test rollback behaviour")
      }.`when`(adjustmentService).createAdjustment(any(), any(), any())

      CommunityPaybackAndDeliusMockServer.setupDeleteAdjustmentResponse(25)

      callCreateAdjustment(
        request = CreateAdjustmentDto.valid(ctx).copy(taskId = task.id),
        expectedStatus = 500,
      )

      // ensure both creations worked, but only one was deleted
      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = "theusername", count = 2)
      CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(adjustmentId = 25L, count = 1)

      // only 1 domain event is published because the second transaction is rolled back
      domainEventAsserter.assertEventCount("community-payback.adjustment.created", 1)
    }

    private fun setupGetUpwDetailsResponse() {
      CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(
        crn = CRN,
        case = NDCaseSummary.valid(),
        unpaidWorkDetails = listOf(
          NDUpwDetails.valid().copy(eventNumber = DELIUS_EVENT_NUMBER),
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
}
