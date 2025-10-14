package uk.gov.justice.digital.hmpps.communitypaybackapi.common.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FormCacheEntityRepository : JpaRepository<FormCacheEntity, FormCacheId> {
  fun findByFormIdAndFormType(formId: String, formType: String): FormCacheEntity?
}
