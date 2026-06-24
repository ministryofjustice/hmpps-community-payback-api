package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse

fun <T : Any> PageResponse<T>.asPage(pageable: Pageable = Pageable.unpaged()) = PageImpl(this.content, pageable, this.page.totalElements)

inline fun <T : Any, R : Any> PageResponse<T>.asPage(pageable: Pageable = Pageable.unpaged(), transform: (T) -> R) = PageImpl(this.content.map(transform), pageable, this.page.totalElements)
