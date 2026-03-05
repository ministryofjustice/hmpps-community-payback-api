package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.FormKeyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheId
import java.time.OffsetDateTime

@Service
class FormService(
  private val repository: FormCacheEntityRepository,
  private val jsonMapper: JsonMapper,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    const val TTL_DAYS: Long = 7
  }

  fun get(key: FormKeyDto): String {
    val existing = repository.findByIdOrNull(key.toJpaId())
      ?: throw NotFoundException("Form data", "${key.type}/${key.id}")

    return existing.formData
  }

  fun put(key: FormKeyDto, json: String) {
    // Validate JSON is well-formed
    jsonMapper.readTree(json)

    repository.save(
      repository.findByIdOrNull(key.toJpaId())?.apply {
        formData = json
        updatedAt = OffsetDateTime.now()
      }
        ?: FormCacheEntity(
          formId = key.id,
          formType = key.type,
          formData = json,
        ),
    )
  }

  fun deleteIfExists(key: FormKeyDto) {
    repository.deleteById(
      FormCacheId(
        formId = key.id,
        formType = key.type,
      ),
    )
  }

  @Transactional
  fun deleteExpiredEntries() {
    val threshold = OffsetDateTime.now().minusDays(TTL_DAYS)
    log.info("Removing form cache entries that were last updated on or before $threshold")
    val deletedCount = repository.deleteByLastUpdatedAtBefore(threshold)
    log.info("Have removed $deletedCount form cache entries")
  }

  private fun FormKeyDto.toJpaId() = FormCacheId(
    formId = id,
    formType = type,
  )
}
