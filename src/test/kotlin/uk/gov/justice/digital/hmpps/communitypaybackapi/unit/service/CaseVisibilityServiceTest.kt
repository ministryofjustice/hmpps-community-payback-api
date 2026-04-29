package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseAccessItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProbationAccessControlClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.excluded
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.excludedAndRestricted
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.restricted
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.unrestricted
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.CaseVisibilityService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService

@ExtendWith(MockKExtension::class)
class CaseVisibilityServiceTest {

  @MockK
  lateinit var contextService: ContextService

  @MockK
  lateinit var probationAccessControlClient: ProbationAccessControlClient

  @InjectMockKs
  lateinit var caseVisibilityService: CaseVisibilityService

  @BeforeEach
  fun beforeEach() {
    every { contextService.getUserName() } answers { String.random(8) }
  }

  @Nested
  inner class IsLimitedForCurrentUser {
    @Test
    fun `returns empty map for an empty list of CRNs`() {
      val result = caseVisibilityService.isLimitedForCurrentUser(emptyList())

      assertThat(result).isEmpty()
    }

    @Test
    fun `CRN defaults to not limited if not provided in Probation Access Control API response`() {
      val crns = listOf(
        "X123456",
        "Y234567",
        "Z345678",
        "W456789",
      )
      every { probationAccessControlClient.getAccessControlForCrns(any(), crns) } returns NDCaseAccess.valid(
        NDCaseAccessItem.unrestricted("X123456"),
        NDCaseAccessItem.unrestricted("Y234567"),
        NDCaseAccessItem.unrestricted("Z345678"),
      )

      val result = caseVisibilityService.isLimitedForCurrentUser(crns)

      assertThat(result).hasSize(4)
      assertThat(result).containsOnlyKeys(crns)
      assertThat(result["W456789"]).isEqualTo(false)
    }

    @Test
    fun `CRN is limited to user if excluded`() {
      val crns = listOf("X123456")
      every { probationAccessControlClient.getAccessControlForCrns(any(), crns) } returns NDCaseAccess.valid(
        NDCaseAccessItem.excluded("X123456"),
      )

      val result = caseVisibilityService.isLimitedForCurrentUser(crns)

      assertThat(result).hasSize(1)
      assertThat(result).containsOnlyKeys(crns)
      assertThat(result["X123456"]).isEqualTo(true)
    }

    @Test
    fun `CRN is limited to user if restricted`() {
      val crns = listOf("Y234567")
      every { probationAccessControlClient.getAccessControlForCrns(any(), crns) } returns NDCaseAccess.valid(
        NDCaseAccessItem.restricted("Y234567"),
      )

      val result = caseVisibilityService.isLimitedForCurrentUser(crns)

      assertThat(result).hasSize(1)
      assertThat(result).containsOnlyKeys(crns)
      assertThat(result["Y234567"]).isEqualTo(true)
    }

    @Test
    fun `CRN is limited to user if excluded and restricted`() {
      val crns = listOf("Z345678")
      every { probationAccessControlClient.getAccessControlForCrns(any(), crns) } returns NDCaseAccess.valid(
        NDCaseAccessItem.excludedAndRestricted("Z345678"),
      )

      val result = caseVisibilityService.isLimitedForCurrentUser(crns)

      assertThat(result).hasSize(1)
      assertThat(result).containsOnlyKeys(crns)
      assertThat(result["Z345678"]).isEqualTo(true)
    }

    @Test
    fun `CRN is not limited to user when unrestricted`() {
      val crns = listOf("W456789")
      every { probationAccessControlClient.getAccessControlForCrns(any(), crns) } returns NDCaseAccess.valid(
        NDCaseAccessItem.unrestricted("W456789"),
      )

      val result = caseVisibilityService.isLimitedForCurrentUser(crns)

      assertThat(result).hasSize(1)
      assertThat(result).containsOnlyKeys(crns)
      assertThat(result["W456789"]).isEqualTo(false)
    }
  }
}
