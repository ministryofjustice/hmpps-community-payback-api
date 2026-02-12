package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tools.jackson.core.exc.UnexpectedEndOfInputException
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.FormKeyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.FormService

@ExtendWith(MockKExtension::class)
class FormServiceTest {

  @MockK
  lateinit var repository: FormCacheEntityRepository

  lateinit var service: FormService

  @BeforeEach
  fun setup() {
    service = FormService(repository, JsonMapper())
  }

  private val formType = "assessment"
  private val id = "12345"

  @Nested
  inner class FormGet {
    @Test
    fun `returns stored json when found`() {
      val json = """{"key":"value"}"""
      every { repository.findByFormIdAndFormType(id, formType) } returns FormCacheEntity(
        formId = id,
        formType = formType,
        formData = json,
      )

      val result = service.get(FormKeyDto(formType, id))

      assertThat(result).isEqualTo(json)
      verify(exactly = 1) { repository.findByFormIdAndFormType(id, formType) }
    }

    @Test
    fun `throws NotFoundException when missing`() {
      every { repository.findByFormIdAndFormType(id, formType) } returns null

      assertThatThrownBy {
        service.get(FormKeyDto(formType, id))
      }.hasMessage("Form data not found for ID 'assessment/12345'")
    }
  }

  @Nested
  inner class FormPut {
    @Test
    fun `valid json is saved`() {
      val json = """{"a":1}"""
      val slotEntity: CapturingSlot<FormCacheEntity> = slot()
      every { repository.save(capture(slotEntity)) } answers { slotEntity.captured }

      service.put(FormKeyDto(formType, id), json)

      verify(exactly = 1) { repository.save(any()) }
      val saved = slotEntity.captured
      assertThat(saved.formId).isEqualTo(id)
      assertThat(saved.formType).isEqualTo(formType)
      assertThat(saved.formData).isEqualTo(json)
    }

    @Test
    fun `invalid json throws exception and doesnt save`() {
      val badJson = "{"

      assertThatThrownBy {
        service.put(FormKeyDto(formType, id), badJson)
      }.isInstanceOf(UnexpectedEndOfInputException::class.java)

      verify(exactly = 0) { repository.save(any()) }
    }
  }
}
