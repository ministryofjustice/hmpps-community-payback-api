package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.FormKeyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheId

@Service
class FormService(
  private val repository: FormCacheEntityRepository,
  private val jsonMapper: JsonMapper,
) {
  fun get(key: FormKeyDto): String {
    val existing = repository.findByFormIdAndFormType(key.id, key.type)
      ?: throw NotFoundException("Form data", "${key.type}/${key.id}")

    return existing.formData
  }

  fun put(key: FormKeyDto, json: String) {
    // Validate JSON is well-formed
    jsonMapper.readTree(json)

    val entity = FormCacheEntity(
      formId = key.id,
      formType = key.type,
      formData = json,
    )

    repository.save(entity)
  }

  fun deleteIfExists(key: FormKeyDto) {
    repository.deleteById(
      FormCacheId(
        formId = key.id,
        formType = key.type,
      ),
    )
  }
}
