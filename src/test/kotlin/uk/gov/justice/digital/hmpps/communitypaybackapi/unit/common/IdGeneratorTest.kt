package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.IdGenerator
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

class IdGeneratorTest {
  @Test
  fun `should generate a valid Version 4 UUID`() {
    val result = IdGenerator(TestPropertiesA::class).generateId(TestPropertiesA.valid())

    assertThat(result)
      .withFailMessage { "Expected the UUID version to equal 4 but was ${result.version()}" }
      .matches { it.version() == 4 }
    assertThat(result)
      .withFailMessage { "Expected the UUID variant to equal 2 but was ${result.variant()}" }
      .matches { it.variant() == 2 }
  }

  @Test
  fun `multiple calls using the same instance should return the same UUID`() {
    val idGenerator = IdGenerator(TestPropertiesA::class)
    val properties = TestPropertiesA.valid()

    val result1 = idGenerator.generateId(properties)
    val result2 = idGenerator.generateId(properties)

    assertThat(result1).isEqualTo(result2)
  }

  @Test
  fun `different instances of the same type with the same data should return the same UUID`() {
    val idGenerator = IdGenerator(TestPropertiesA::class)

    val properties1 = TestPropertiesA.valid()
    val properties2 = properties1.copy()

    val result1 = idGenerator.generateId(properties1)
    val result2 = idGenerator.generateId(properties2)

    assertThat(result1).isEqualTo(result2)
  }

  @Test
  fun `different instances of the same type with different data should return different UUIDs`() {
    val idGenerator = IdGenerator(TestPropertiesA::class)
    val properties1 = TestPropertiesA.valid()
    val properties2 = TestPropertiesA.valid()

    val result1 = idGenerator.generateId(properties1)
    val result2 = idGenerator.generateId(properties2)

    assertThat(result1).isNotEqualTo(result2)
  }

  @Test
  fun `different types containing identical data should return different UUIDs`() {
    val idGeneratorA = IdGenerator(TestPropertiesA::class)
    val idGeneratorB = IdGenerator(TestPropertiesB::class)

    val propertiesA = TestPropertiesA(x = 42, y = "foo bar")
    val propertiesB = TestPropertiesB(x = 42, y = "foo bar")

    val resultA = idGeneratorA.generateId(propertiesA)
    val resultB = idGeneratorB.generateId(propertiesB)

    assertThat(resultA).isNotEqualTo(resultB)
  }

  @Test
  fun `different types containing different data should return different UUIDs`() {
    val idGeneratorA = IdGenerator(TestPropertiesA::class)
    val idGeneratorB = IdGenerator(TestPropertiesB::class)
    val idGeneratorC = IdGenerator(TestPropertiesC::class)

    val propertiesA = TestPropertiesA.valid()
    val propertiesB = TestPropertiesB.valid()
    val propertiesC = TestPropertiesC.valid()

    val resultA = idGeneratorA.generateId(propertiesA)
    val resultB = idGeneratorB.generateId(propertiesB)
    val resultC = idGeneratorC.generateId(propertiesC)

    assertThat(resultA).isNotEqualTo(resultB)
    assertThat(resultA).isNotEqualTo(resultC)
    assertThat(resultB).isNotEqualTo(resultC)
  }
}

data class TestPropertiesA(val x: Int, val y: String) {
  companion object {
    fun valid(): TestPropertiesA = TestPropertiesA(x = Int.random(), y = String.random())
  }
}

data class TestPropertiesB(val x: Int, val y: String) {
  companion object {
    fun valid(): TestPropertiesB = TestPropertiesB(x = Int.random(), y = String.random())
  }
}

data class TestPropertiesC(val x: Int, val y: String, val z: Boolean) {
  companion object {
    fun valid(): TestPropertiesC = TestPropertiesC(x = Int.random(), y = String.random(), z = Boolean.random())
  }
}
