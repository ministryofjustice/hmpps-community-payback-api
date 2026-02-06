package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Email
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
  name = "ete_user",
  uniqueConstraints = [
    UniqueConstraint(
      name = "uk_ete_user_crn_email",
      columnNames = ["crn", "email"],
    ),
  ],
)
data class EteUser(
  @Id
  val id: UUID = UUID.randomUUID(),

  @Column(name = "crn", nullable = false)
  val crn: String,

  @field:Email
  @Column(name = "email", nullable = false)
  val email: String,

  val createdAt: OffsetDateTime = OffsetDateTime.now(),
  val updatedAt: OffsetDateTime = OffsetDateTime.now(),

  @OneToMany(mappedBy = "user")
  val courseCompletions: Set<EteCourseCompletionEventEntity> = emptySet(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as EteUser

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "EteUser(id=$id, crn='$crn', email='$email')"

  companion object
}
