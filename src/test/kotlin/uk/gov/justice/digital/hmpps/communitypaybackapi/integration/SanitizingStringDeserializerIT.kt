package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.SanitizingStringDeserializer

@RestController
@RequestMapping(
  "/it/sanitizing-strings",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class SanitizingStringDeserializerTestEndpoints {
  @PostMapping("/item")
  fun postItem(@RequestBody item: ItemDto) = ResponseEntity.status(HttpStatus.CREATED).body(item)
}

data class ItemDto(
  @field:JsonDeserialize(using = SanitizingStringDeserializer::class)
  val value: String,
)

class SanitizingStringDeserializerIT : IntegrationTestBase() {
  @Test
  fun `strings containing HTML tags should return a 400 Bad Request response`() {
    webTestClient.post()
      .uri("it/sanitizing-strings/item")
      .addAdminUiAuthHeader("theusername")
      .bodyValue(ItemDto("<h1>Big text</h1>"))
      .exchange()
      .expectStatus().isEqualTo(400)
  }

  @Test
  fun `strings without HTML tags should successfully deserialize`() {
    webTestClient.post()
      .uri("it/sanitizing-strings/item")
      .addAdminUiAuthHeader("theusername")
      .bodyValue(ItemDto("Regular text"))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.CREATED)
  }
}
