package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid

class TestSupportIT : IntegrationTestBase() {
  @Autowired
  lateinit var formCacheEntityRepository: FormCacheEntityRepository

  @Nested
  inner class DeleteForms {
    @Test
    fun `PUT stores and GET returns the JSON, no auth required`() {
      formCacheEntityRepository.save(FormCacheEntity.valid())
      formCacheEntityRepository.save(FormCacheEntity.valid())
      formCacheEntityRepository.save(FormCacheEntity.valid())

      webTestClient.delete()
        .uri("/test-support/forms")
        .exchange()
        .expectStatus().isOk

      assertThat(formCacheEntityRepository.count()).isEqualTo(0)
    }
  }
}
