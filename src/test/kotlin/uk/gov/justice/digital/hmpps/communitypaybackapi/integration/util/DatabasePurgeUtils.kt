package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository

@Service
class DatabasePurgeUtils {

  @Autowired
  lateinit var eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  @Autowired
  lateinit var eteCourseCompletionEventResolutionRepository: EteCourseCompletionEventResolutionRepository

  fun deleteAllEteData() {
    eteCourseCompletionEventResolutionRepository.deleteAll()
    eteCourseCompletionEventEntityRepository.deleteAll()
  }
}
