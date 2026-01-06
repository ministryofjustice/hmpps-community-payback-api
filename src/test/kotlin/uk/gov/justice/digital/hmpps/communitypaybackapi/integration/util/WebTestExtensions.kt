package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util

import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

inline fun <reified T : Any> WebTestClient.ResponseSpec.bodyAsObject(): T = this.returnResult<T>().responseBody.blockFirst()!!
