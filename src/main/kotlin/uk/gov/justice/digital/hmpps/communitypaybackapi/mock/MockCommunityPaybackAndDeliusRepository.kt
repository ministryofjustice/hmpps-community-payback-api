package uk.gov.justice.digital.hmpps.communitypaybackapi.mock

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseName
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.mock.MockCommunityPaybackAndDeliusController.CaseSummaryWithRestrictions
import uk.gov.justice.digital.hmpps.communitypaybackapi.mock.MockCommunityPaybackAndDeliusController.MockProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.mock.MockCommunityPaybackAndDeliusController.MockProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.mock.MockCommunityPaybackAndDeliusController.MockProjectSession
import java.time.LocalDate
import java.time.LocalTime

@SuppressWarnings("MagicNumber")
object MockCommunityPaybackAndDeliusRepository {
  const val PROJECT1_ID = 101L
  const val PROJECT2_ID = 202L

  const val CRN1 = "CRN0001"
  const val CRN2 = "CRN0002"
  const val CRN3 = "CRN0003"

  const val APPOINTMENT1_ID = 1L
  const val APPOINTMENT2_ID = 2L
  const val APPOINTMENT3_ID = 3L

  val mockProject1 = MockProject(
    id = PROJECT1_ID,
    name = "Community Garden",
    code = "cg",
    typeName = "Environmental Improvement",
    typeCode = "ENV",
    location = "Garden Road, Sheffield",
  )

  val mockProject2 = MockProject(
    id = PROJECT2_ID,
    name = "Park Cleanup",
    code = "pc",
    typeName = "Environmental Improvement",
    typeCode = "ENV",
    location = "Park Road, Birmingham",
  )

  val mockProjectSessions = listOf(
    MockProjectSession(
      project = mockProject1,
      date = LocalDate.of(2025, 9, 1),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(17, 0),
      appointmentSummaries = listOf(
        MockProjectAppointmentSummary(
          id = APPOINTMENT1_ID,
          crn = CRN1,
          requirementMinutes = 600,
          completedMinutes = 60,
        ),
        MockProjectAppointmentSummary(
          id = APPOINTMENT2_ID,
          crn = CRN2,
          requirementMinutes = 300,
          completedMinutes = 30,
        ),
      ),
    ),
    MockProjectSession(
      project = mockProject2,
      date = LocalDate.of(2025, 9, 8),
      startTime = LocalTime.of(8, 0),
      endTime = LocalTime.of(16, 0),
      appointmentSummaries = listOf(
        MockProjectAppointmentSummary(
          id = APPOINTMENT3_ID,
          crn = CRN1,
          requirementMinutes = 1200,
          completedMinutes = 0,
        ),
      ),
    ),
  )

  val mockProjectAppointments = listOf(
    ProjectAppointment(
      id = APPOINTMENT1_ID,
      projectName = mockProject1.name,
      projectCode = mockProject1.code,
      projectTypeName = mockProject1.typeName,
      projectTypeCode = mockProject1.typeCode,
      crn = CRN1,
      supervisingTeam = "Team Lincoln",
      date = LocalDate.of(2025, 9, 1),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(17, 0),
      penaltyTime = null,
      supervisorCode = null,
      contactOutcomeId = null,
      enforcementActionId = null,
      respondBy = null,
      hiVisWorn = null,
      workedIntensively = null,
      workQuality = null,
      behaviour = null,
      notes = null,
    ),
    ProjectAppointment(
      id = APPOINTMENT2_ID,
      projectName = mockProject1.name,
      projectCode = mockProject1.code,
      projectTypeName = mockProject1.typeName,
      projectTypeCode = mockProject1.typeCode,
      crn = CRN2,
      supervisingTeam = "Team Lincoln",
      date = LocalDate.of(2025, 9, 1),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(17, 0),
      penaltyTime = null,
      supervisorCode = null,
      contactOutcomeId = null,
      enforcementActionId = null,
      respondBy = null,
      hiVisWorn = null,
      workedIntensively = null,
      workQuality = null,
      behaviour = null,
      notes = null,
    ),
    ProjectAppointment(
      id = APPOINTMENT3_ID,
      projectName = mockProject2.name,
      projectCode = mockProject2.code,
      projectTypeName = mockProject2.typeName,
      projectTypeCode = mockProject2.typeCode,
      crn = CRN1,
      supervisingTeam = "Team Lincoln",
      date = LocalDate.of(2025, 9, 1),
      startTime = LocalTime.of(8, 0),
      endTime = LocalTime.of(16, 0),
      penaltyTime = null,
      supervisorCode = null,
      contactOutcomeId = null,
      enforcementActionId = null,
      respondBy = null,
      hiVisWorn = null,
      workedIntensively = null,
      workQuality = null,
      behaviour = null,
      notes = null,
    ),
  )

  val cases = listOf(
    CaseSummaryWithRestrictions(
      caseSummary = CaseSummary(
        crn = CRN1,
        name = CaseName("Jack", "Sparrow", middleNames = emptyList()),
        currentExclusion = false,
        currentRestriction = false,
      ),
      isCrnRestricted = { false },
      isCrnExcluded = { false },
    ),
    CaseSummaryWithRestrictions(
      caseSummary = CaseSummary(
        crn = CRN2,
        name = CaseName("Norman", "Osbourn", middleNames = listOf("Green")),
        currentExclusion = true,
        currentRestriction = false,
      ),
      isCrnRestricted = { false },
      isCrnExcluded = { false },
    ),
    CaseSummaryWithRestrictions(
      caseSummary = CaseSummary(
        crn = CRN3,
        name = CaseName("Otto", "Octavius", middleNames = listOf("on")),
        currentExclusion = true,
        currentRestriction = false,
      ),
      isCrnRestricted = { it.endsWith("s") },
      isCrnExcluded = { false },
    ),
  )
}
