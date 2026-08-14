package uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation

interface ValidationContext<in T> {
  object EmptyContext : ValidationContext<Any>
}
