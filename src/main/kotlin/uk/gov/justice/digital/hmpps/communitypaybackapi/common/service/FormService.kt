package uk.gov.justice.digital.hmpps.communitypaybackapi.common.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.entity.FormCacheEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.entity.FormCacheEntityRepository

@Service
class FormService(
  private val repository: FormCacheEntityRepository,
  private val objectMapper: ObjectMapper,
) {
  fun formGet(formType: String, id: String): String {
    val existing = repository.findByFormIdAndFormType(id, formType)
      ?: throw NotFoundException("Form data", "$formType/$id")

    return existing.formData
  }

  fun formPut(formType: String, id: String, json: String) {
    // Validate JSON is well-formed
    objectMapper.readTree(json)

    val entity = FormCacheEntity(
      formId = id,
      formType = formType,
      formData = json,
    )

    repository.save(entity)
  }
}
