package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime
import java.util.UUID

@Immutable
@Entity
@Table(name = "incentives_scheme_eligibility_scopes")
data class IncentivesSchemeEligibilityScopeEntity(
  @Id
  val id: UUID,
  val code: String,
  val description: String,
  @Enumerated(EnumType.STRING)
  val scope: IncentivesSchemeEligibilityScope,
  val isEligible: Boolean,
  @CreationTimestamp
  val createdAt: OffsetDateTime = OffsetDateTime.now(),
  @UpdateTimestamp
  val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

enum class IncentivesSchemeEligibilityScope {
  OUTCOME,
  REQUIREMENT_SUBTYPE,
}
