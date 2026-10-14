package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.CourseCompletionVisibilityConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.CourseFailureFilter
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.ResolutionStatus
import java.time.OffsetDateTime
import java.util.UUID

// JPQL fragments below use e as the EteCourseCompletionEventEntity alias.
// Each specification contributes a condition to the combined query.
object EteCourseCompletionEventSpecifications {
  // JPQL: WHERE e.pdu.providerCode = :code
  fun provider(code: String) = Specification<EteCourseCompletionEventEntity> { root, _, cb ->
    cb.equal(root.get<CommunityCampusPduEntity>("pdu").get<String>("providerCode"), code)
  }

  // JPQL: WHERE e.pdu.id = :id (omitted when id is null)
  fun pdu(id: UUID?) = Specification<EteCourseCompletionEventEntity> { root, _, cb ->
    id?.let { cb.equal(root.get<CommunityCampusPduEntity>("pdu").get<UUID>("id"), it) }
  }

  // JPQL: WHERE e.office IN :names (omitted when names is null; an empty list matches nothing)
  fun offices(names: List<String>?) = Specification<EteCourseCompletionEventEntity> { root, _, _ ->
    names?.let { root.get<String>("office").`in`(it) }
  }

  // JPQL: WHERE e.externalReference = :reference (omitted when reference is null)
  fun externalReference(reference: String?) = Specification<EteCourseCompletionEventEntity> { root, _, cb ->
    reference?.let { cb.equal(root.get<String>("externalReference"), it) }
  }

  // JPQL: WHERE e.completionDateTime >= :from (omitted when from is null)
  fun completedFrom(from: OffsetDateTime?) = Specification<EteCourseCompletionEventEntity> { root, _, cb ->
    from?.let { cb.greaterThanOrEqualTo(root.get("completionDateTime"), it) }
  }

  // JPQL: WHERE e.completionDateTime <= :to (omitted when to is null)
  fun completedTo(to: OffsetDateTime?) = Specification<EteCourseCompletionEventEntity> { root, _, cb ->
    to?.let { cb.lessThanOrEqualTo(root.get("completionDateTime"), it) }
  }

  // JPQL: LEFT JOIN e.resolution r
  // RESOLVED: WHERE r.id IS NOT NULL; UNRESOLVED: WHERE r.id IS NULL; ANY: no condition or join.
  fun resolution(status: ResolutionStatus) = Specification<EteCourseCompletionEventEntity> { root, _, cb ->
    if (status == ResolutionStatus.ANY) {
      null
    } else {
      val resolution = root.join<EteCourseCompletionEventEntity, EteCourseCompletionEventResolutionEntity>("resolution", JoinType.LEFT)
      when (status) {
        ResolutionStatus.RESOLVED -> cb.isNotNull(resolution.get<UUID>("id"))
        ResolutionStatus.UNRESOLVED -> cb.isNull(resolution.get<UUID>("id"))
        ResolutionStatus.ANY -> null
      }
    }
  }

  // JPQL for HIDE: WHERE e.status = 'PASSED'
  // SHOW_ONLY_WHEN_MAX_ATTEMPTS_REACHED: WHERE e.status = 'PASSED' OR MOD(e.attempts, 3) = 0
  // SHOW_ALL: no condition.
  fun courseFailures(filter: CourseFailureFilter) = Specification<EteCourseCompletionEventEntity> { root, _, cb ->
    val passed = cb.equal(root.get<EteCourseCompletionEventStatus>("status"), EteCourseCompletionEventStatus.PASSED)
    when (filter) {
      CourseFailureFilter.HIDE -> passed
      CourseFailureFilter.SHOW_ALL -> null
      CourseFailureFilter.SHOW_ONLY_WHEN_MAX_ATTEMPTS_REACHED -> cb.or(passed, cb.equal(cb.mod(root.get("attempts"), 3), 0))
    }
  }

  // JPQL: WHERE (e.office = :office1 AND e.completionDateTime >= :from1)
  //          OR (e.office = :office2 AND e.completionDateTime >= :from2) OR ...
  // Null config or showAll: no condition. Restricted mode with no offices: WHERE 1 = 0.
  fun visibleOffices(config: CourseCompletionVisibilityConfig?): Specification<EteCourseCompletionEventEntity> {
    if (config == null || config.showAll) return Specification.unrestricted()

    // OR combines office rules; an empty list intentionally matches no completions.
    if (config.offices.isEmpty()) return Specification { _, _, cb -> cb.disjunction() }

    return Specification.anyOf(
      config.offices.map { office ->
        offices(listOf(office.name)).and(completedFrom(requireNotNull(office.from)))
      },
    )
  }
}
