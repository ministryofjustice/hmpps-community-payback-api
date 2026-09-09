package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequestReferenceNotFound
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.formatForUser
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

@Suppress("detekt:CyclomaticComplexMethod", "unchecked_cast")
fun throwValidationErrorForAppointmentUpdateCreate(error: ValidationResultItem) {
  when (error.code) {
    "UNKNOWN_PROJECT_CODE" -> badRequestReferenceNotFound("Project", error.data["code"]!!)
    "COULD_NOT_FIND_UNPAID_WORK_DETAILS" -> badRequest("Cannot find unpaid work details for CRN ${error.data["crn"]} and event number ${error.data["deliusEventNumber"]}")
    "APPOINTMENT_DATE_IS_NOT_BEFORE_END_OF_PROJECT" -> {
      val date = (error.data["date"] as LocalDate).formatForUser()
      val projectEndDateExclusive = (error.data["projectEndDateExclusive"] as LocalDate).formatForUser()
      badRequest("Appointment Date of $date must be before project end date $projectEndDateExclusive")
    }

    "APPOINTMENT_DATE_IS_BEFORE_SENTENCE_DATE" -> {
      val date = (error.data["date"] as LocalDate).formatForUser()
      val sentenceDate = (error.data["sentenceDate"] as LocalDate).formatForUser()
      badRequest("Appointment Date of $date must be on or after sentence date of $sentenceDate")
    }

    "PROJECT_NOT_AVAILABLE_ON_REQUESTED_DAY_OF_WEEK" -> {
      val requestedDayOfWeek = (error.data["requestedDayOfWeek"] as DayOfWeek).formatForUser()
      val availableDays =
        (error.data["availableDays"] as List<DayOfWeek>).joinToString(separator = ", ") { it.formatForUser() }
      badRequest("Project is not available on $requestedDayOfWeek. Available days are $availableDays")
    }

    "UNKNOWN_CONTACT_OUTCOME" -> badRequest("Contact outcome not found for code '${error.data["code"]}'")
    "PAST_APPOINTMENT_REQUIRES_CONTACT_OUTCOME" -> badRequest("As the appointment is now complete a contact outcome is required")
    "FUTURE_APPOINTMENT_ONLY_ALLOWS_ACCEPTABLE_ABSENCE_OUTCOMES" -> badRequest("As the appointment is in the future only acceptable absence outcomes can be recorded")
    "ATTENDED_CONTACT_OUTCOME_REQUIRES_ATTENDANCE_DATA" -> badRequest("Attendance data is required for contact outcomes that indicate attendance")
    "END_TIME_NOT_AFTER_START_TIME" -> {
      val startTime = (error.data["startTime"] as LocalTime).formatForUser()
      val endTime = (error.data["endTime"] as LocalTime).formatForUser()
      badRequest("End Time '$endTime' must be after Start Time '$startTime'")
    }

    "PENALTY_DURATION_EXCEEDS_APPOINTMENT_DURATION" -> {
      val penaltyDuration = (error.data["penaltyDuration"] as Duration).formatForUser()
      val appointmentDuration = (error.data["appointmentDuration"] as Duration).formatForUser()
      badRequest("Penalty duration '$penaltyDuration' is greater than appointment duration '$appointmentDuration'")
    }

    "NOTES_TOO_LONG" -> badRequest("Notes must be fewer than 4000 characters")
    "CREDITED_TIME_EXCEEDS_REMAINING_REQUIREMENT_TIME" -> {
      val timeToCredit = (error.data["timeToCredit"] as Duration).formatForUser()
      val remainingRequirementTime = (error.data["remainingRequirementTime"] as Duration).formatForUser()
      badRequest("Credited minutes of '$timeToCredit' exceeds the remaining time required of '$remainingRequirementTime'")
    }

    "CREDITED_ETE_TIME_EXCEEDS_REMAINING_ETE_TIME" -> {
      val timeToCredit = (error.data["timeToCredit"] as Duration).formatForUser()
      val remainingEteTime = (error.data["remainingEteTime"] as LocalTime).formatForUser()
      badRequest("Credited minutes of '$timeToCredit' exceeds remaining allowed ETE time of '$remainingEteTime'")
    }

    "UNKNOWN_PICKUP_LOCATION" -> {
      val projectTeamCode = error.data["projectTeamCode"]
      val locationCode = error.data["locationCode"]
      badRequest("Pick Up Location not found for team '$projectTeamCode' and code '$locationCode'")
    }

    "APPOINTMENT_IS_SENSITIVE" -> badRequest("This appointment has previously been marked as sensitive so this cannot be changed")
  }
}
