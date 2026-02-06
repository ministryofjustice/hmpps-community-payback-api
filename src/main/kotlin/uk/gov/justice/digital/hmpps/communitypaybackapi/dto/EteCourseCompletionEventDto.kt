package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventStatus
import java.time.LocalDate
import java.util.UUID

data class EteCourseCompletionEventDto(

  @param:Schema(description = "Id", example = "550e8400-e29b-41d4-a716-446655440000")
  val id: UUID,

  @param:Schema(description = "First name of the PoP", example = "John")
  val firstName: String,

  @param:Schema(description = "Last name of the PoP", example = "Smith")
  val lastName: String,

  @param:Schema(description = "Date of birth", example = "1990-01-15")
  val dateOfBirth: LocalDate,

  @param:Schema(description = "Region where the course was completed", example = "North West")
  val region: String,

  @param:Schema(description = "Email address", example = "john.smith@example.com")
  val email: String,

  @param:Schema(description = "Name of the course", example = "Health & Safety Level 1")
  val courseName: String,

  @param:Schema(description = "Type of course", example = "Course Type")
  val courseType: String,

  @param:Schema(description = "Course provider name", example = "Moodle")
  val provider: String,

  @param:Schema(description = "Date the course was completed", example = "2025-01-15")
  val completionDate: LocalDate,

  @param:Schema(description = "Status of the course completion", example = "COMPLETED")
  @Enumerated(EnumType.STRING)
  val status: EteCourseEventStatus,

  @param:Schema(description = "Total time spent on the course in minutes", example = "180")
  val totalTimeMinutes: Long,

  @param:Schema(description = "Expected time for the course in minutes", example = "240")
  val expectedTimeMinutes: Long,

  @param:Schema(description = "Number of attempts made to complete the course", example = "2", nullable = true)
  val attempts: Int?,

  @param:Schema(description = "External reference identifier", example = "EXT-12345")
  val externalReference: String,
)
