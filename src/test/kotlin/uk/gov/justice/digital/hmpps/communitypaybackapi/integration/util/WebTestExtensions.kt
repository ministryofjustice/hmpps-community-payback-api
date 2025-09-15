package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util

import org.springframework.test.web.reactive.server.WebTestClient

inline fun <reified T> WebTestClient.ResponseSpec.bodyAsObject(): T = this.returnResult(T::class.java).responseBody.blockFirst()!!
