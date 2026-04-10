package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpLocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun PickUpLocationDto.Companion.valid() = PickUpLocationDto(
  deliusCode = String.random(5),
  description = String.random(50),
  buildingName = String.random(),
  buildingNumber = String.random(),
  streetName = String.random(),
  townCity = String.random(),
  county = String.random(),
  postCode = String.random(),
)
