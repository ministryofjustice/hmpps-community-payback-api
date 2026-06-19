package uk.gov.justice.digital.hmpps.communitypaybackapi.client

import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange
import java.time.LocalDate

interface ProbationOffenderSearchClient {
  @PostExchange("/search")
  fun searchPerson(@RequestBody request: OffenderSearchRequest): List<OffenderDetail>
}

data class OffenderSearchRequest(
  val firstName: String? = null,
  val surname: String? = null,
  val dateOfBirth: LocalDate? = null,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val crn: String? = null,
  val nomsNumber: String? = null,
  val includeAliases: Boolean? = null,
) {
  companion object
}

data class OffenderDetail(
  val previousSurname: String? = null,
  val offenderId: Long? = null,
  val title: String? = null,
  val firstName: String? = null,
  val middleNames: List<String>? = null,
  val surname: String? = null,
  val dateOfBirth: LocalDate? = null,
  val gender: String? = null,
  val otherIds: IDs,
  val contactDetails: ContactDetails? = null,
  val offenderProfile: OffenderProfile? = null,
  val offenderAliases: List<OffenderAlias>? = null,
  val offenderManagers: List<OffenderManager>? = null,
  val softDeleted: Boolean? = null,
  val currentDisposal: String? = null,
  val partitionArea: String? = null,
  val currentRestriction: Boolean? = null,
  val restrictionMessage: String? = null,
  val currentExclusion: Boolean? = null,
  val exclusionMessage: String? = null,
  val highlight: Map<String, List<String>>? = null,
  val accessDenied: Boolean? = null,
  val currentTier: String? = null,
  val activeProbationManagedSentence: Boolean? = null,
  val mappa: MappaDetails? = null,
  val probationStatus: ProbationStatus? = null,
  val age: Int? = null,
) {
  companion object
}

data class ContactDetails(
  val phoneNumbers: List<PhoneNumber>? = null,
  val emailAddresses: List<String>? = null,
  val allowSMS: Boolean? = null,
  val addresses: List<Address>? = null,
)

data class PhoneNumber(
  val number: String? = null,
  val type: String? = null,
)

data class Address(
  val id: Long? = null,
  val from: LocalDate? = null,
  val to: LocalDate? = null,
  val noFixedAbode: Boolean? = null,
  val notes: String? = null,
  val addressNumber: String? = null,
  val buildingName: String? = null,
  val streetName: String? = null,
  val district: String? = null,
  val town: String? = null,
  val county: String? = null,
  val postcode: String? = null,
  val telephoneNumber: String? = null,
  val status: KeyValue? = null,
  val type: KeyValue? = null,
)

data class OffenderProfile(
  val ethnicity: String? = null,
  val nationality: String? = null,
  val secondaryNationality: String? = null,
  val notes: String? = null,
  val immigrationStatus: String? = null,
  val offenderLanguages: OffenderLanguages? = null,
  val religion: String? = null,
  val sexualOrientation: String? = null,
  val offenderDetails: String? = null,
  val remandStatus: String? = null,
  val previousConviction: PreviousConviction? = null,
  val riskColour: String? = null,
  val disabilities: List<Disability>? = null,
  val provisions: List<Provision>? = null,
)

data class OffenderLanguages(
  val primaryLanguage: String? = null,
  val otherLanguages: List<String>? = null,
  val languageConcerns: String? = null,
  val requiresInterpreter: Boolean? = null,
)

data class PreviousConviction(
  val convictionDate: LocalDate? = null,
  val detail: Map<String, String>? = null,
)

data class Disability(
  val disabilityId: Long? = null,
  val disabilityType: KeyValue? = null,
  val condition: KeyValue? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val notes: String? = null,
)

data class Provision(
  val provisionId: Long? = null,
  val provisionType: KeyValue? = null,
  val category: KeyValue? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val notes: String? = null,
)

data class OffenderAlias(
  val id: String? = null,
  val dateOfBirth: LocalDate? = null,
  val firstName: String? = null,
  val middleNames: List<String>? = null,
  val surname: String? = null,
  val gender: String? = null,
)

data class OffenderManager(
  val trustOfficer: Human? = null,
  val staff: StaffHuman? = null,
  val providerEmployee: Human? = null,
  val partitionArea: String? = null,
  val softDeleted: Boolean? = null,
  val team: Team? = null,
  val probationArea: ProbationArea? = null,
  val fromDate: LocalDate? = null,
  val toDate: LocalDate? = null,
  val active: Boolean? = null,
  val allocationReason: KeyValue? = null,
)

data class Human(
  val forenames: String? = null,
  val surname: String? = null,
)

data class StaffHuman(
  val code: String? = null,
  val forenames: String? = null,
  val surname: String? = null,
)

data class Team(
  val code: String? = null,
  val description: String? = null,
  val telephone: String? = null,
  val localDeliveryUnit: KeyValue? = null,
  val district: KeyValue? = null,
  val borough: KeyValue? = null,
)

data class ProbationArea(
  val probationAreaId: Long? = null,
  val code: String? = null,
  val description: String? = null,
  val nps: Boolean? = null,
  val organisation: KeyValue? = null,
  val institution: Institution? = null,
  val teams: List<AllTeam>? = null,
)

data class Institution(
  val institutionId: Long? = null,
  val isEstablishment: Boolean? = null,
  val code: String? = null,
  val description: String? = null,
  val institutionName: String? = null,
  val establishmentType: KeyValue? = null,
  val isPrivate: Boolean? = null,
  val nomsPrisonInstitutionCode: String? = null,
)

data class AllTeam(
  val providerTeamId: Long? = null,
  val teamId: Long? = null,
  val code: String? = null,
  val description: String? = null,
  val name: String? = null,
  val isPrivate: Boolean? = null,
  val externalProvider: KeyValue? = null,
  val scProvider: KeyValue? = null,
  val localDeliveryUnit: KeyValue? = null,
  val district: KeyValue? = null,
  val borough: KeyValue? = null,
)

data class MappaDetails(
  val level: Int? = null,
  val levelDescription: String? = null,
  val category: Int? = null,
  val categoryDescription: String? = null,
  val startDate: LocalDate? = null,
  val reviewDate: LocalDate? = null,
  val team: KeyValue? = null,
  val officer: StaffHuman? = null,
  val probationArea: KeyValue? = null,
  val notes: String? = null,
)

data class ProbationStatus(
  val status: String,
  val previouslyKnownTerminationDate: LocalDate? = null,
  val inBreach: Boolean? = null,
  val preSentenceActivity: Boolean,
  val awaitingPsr: Boolean,
)

data class KeyValue(
  val code: String? = null,
  val description: String? = null,
)

data class IDs(
  val crn: String,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val niNumber: String? = null,
  val nomsNumber: String? = null,
  val immigrationNumber: String? = null,
  val mostRecentPrisonerNumber: String? = null,
  val previousCrn: String? = null,
) {
  companion object
}

sealed interface OffenderSearchResult {
  data class SingleMatch(val crn: String) : OffenderSearchResult
  data object NoMatch : OffenderSearchResult
  data object MultipleMatches : OffenderSearchResult

  companion object {
    fun from(response: List<OffenderDetail>): OffenderSearchResult = when (response.size) {
      0 -> NoMatch
      1 -> SingleMatch(response.first().otherIds.crn)
      else -> MultipleMatches
    }
  }
}
