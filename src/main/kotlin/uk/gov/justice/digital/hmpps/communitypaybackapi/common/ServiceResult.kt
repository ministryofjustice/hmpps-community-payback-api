package uk.gov.justice.digital.hmpps.communitypaybackapi.common

sealed interface ServiceResult<V> {
  data class Success<V>(val value: V) : ServiceResult<V>

  sealed interface Error<V> : ServiceResult<V> {
    data class BadRequest<V>(val message: String) : Error<V>
    data class NotFound<V>(val entityType: String, val id: String) : Error<V>
  }
}
