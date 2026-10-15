package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.oneoffjobs

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.annotation.MergedAnnotation
import org.springframework.core.annotation.MergedAnnotation.Adapt
import org.springframework.core.annotation.MergedAnnotations
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.core.type.MethodMetadata
import org.springframework.mock.env.MockEnvironment
import uk.gov.justice.digital.hmpps.communitypaybackapi.oneoffjobs.JobIsEnabled
import java.util.Optional
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class JobIsEnabledTest {
  @MockK
  private lateinit var conditionContext: ConditionContext

  @Test
  fun `matches returns false when metadata is not a MethodMetadata`() {
    val metadata = mockk<AnnotatedTypeMetadata>()

    val result = JobIsEnabled().matches(conditionContext, metadata)

    Assertions.assertThat(result).isFalse()
  }

  @ParameterizedTest
  @MethodSource("metadataAndJobName")
  fun `matches returns false when job is not enabled in configuration`(metadata: MethodMetadata, jobName: String) {
    val env = MockEnvironment().also { it.setProperty("community-payback.one-off-jobs.jobs.$jobName.enabled", false) }
    every { conditionContext.environment } returns env

    val result = JobIsEnabled().matches(conditionContext, metadata)

    Assertions.assertThat(result).isFalse()
  }

  @ParameterizedTest
  @MethodSource("metadataAndJobName")
  fun `matches returns true when job is enabled in configuration`(metadata: MethodMetadata, jobName: String) {
    val env = MockEnvironment().also { it.setProperty("community-payback.one-off-jobs.jobs.$jobName.enabled", true) }
    every { conditionContext.environment } returns env

    val result = JobIsEnabled().matches(conditionContext, metadata)

    Assertions.assertThat(result).isTrue()
  }

  companion object {
    @JvmStatic
    fun metadataAndJobName(): Stream<Arguments> = Stream.of(
      Arguments.of(MockMethodMetadata(MockBeanNameSource.MethodName), "testJobMethodName"),
      Arguments.of(MockMethodMetadata(MockBeanNameSource.AnnotationValue), "testJobAnnotationValue"),
      Arguments.of(MockMethodMetadata(MockBeanNameSource.AnnotationName), "testJobAnnotationName"),
    )

    /**
     * Mocks the `AnnotatedTypeMetadata` for a `@Bean`-annotated method when using the `JobIsEnabled` condition.
     *
     * This is used instead of MockK because it seems to have trouble when too many levels of mocking are introduced.
     * These tests use three:
     * 1. The `MethodMetadata`
     * 2. The `MergedAnnotations` from `MethodMetadata.getAnnotations()`
     * 3. The `MergedAnnotation<Bean>` from `MergedAnnotations.get(Bean::class.java)`
     *
     *
     */
    data class MockMethodMetadata(private val nameSource: MockBeanNameSource) : MethodMetadata {
      override fun getMethodName(): String = "testJobMethodName"
      override fun getAnnotations(): MergedAnnotations = MergedAnnotations.of(listOf(MockBeanAnnotation(nameSource)))

      // region Not needed for tests
      override fun getDeclaringClassName(): String = TODO()
      override fun getReturnTypeName(): String = TODO()
      override fun isAbstract(): Boolean = TODO()
      override fun isStatic(): Boolean = TODO()
      override fun isFinal(): Boolean = TODO()
      override fun isOverridable(): Boolean = TODO()
      // endregion Not needed for tests
    }

    enum class MockBeanNameSource {
      MethodName,
      AnnotationValue,
      AnnotationName,
    }

    data class MockBeanAnnotation(private val nameSource: MockBeanNameSource) : MergedAnnotation<Bean> {
      override fun getType(): Class<Bean> = Bean::class.java
      override fun isPresent(): Boolean = true
      override fun isDirectlyPresent(): Boolean = true
      override fun getDistance(): Int = 0
      override fun getAggregateIndex(): Int = 0

      override fun hasNonDefaultValue(attributeName: String): Boolean = when (nameSource) {
        MockBeanNameSource.MethodName -> false
        MockBeanNameSource.AnnotationValue -> attributeName == "value"
        MockBeanNameSource.AnnotationName -> attributeName == "name"
      }

      override fun getStringArray(attributeName: String): Array<out String> = when (nameSource) {
        MockBeanNameSource.MethodName -> emptyArray()
        MockBeanNameSource.AnnotationValue -> if (attributeName == "value") arrayOf("testJobAnnotationValue") else emptyArray()
        MockBeanNameSource.AnnotationName -> if (attributeName == "name") arrayOf("testJobAnnotationName") else emptyArray()
      }

      // region Not needed for tests
      override fun isMetaPresent(): Boolean = TODO()
      override fun getSource(): Any? = TODO()
      override fun getMetaSource(): MergedAnnotation<*>? = TODO()
      override fun getRoot(): MergedAnnotation<*> = TODO()
      override fun getMetaTypes(): List<Class<out Annotation>> = TODO()
      override fun hasDefaultValue(attributeName: String): Boolean = TODO()
      override fun getByte(attributeName: String): Byte = TODO()
      override fun getByteArray(attributeName: String): ByteArray = TODO()
      override fun getBoolean(attributeName: String): Boolean = TODO()
      override fun getBooleanArray(attributeName: String): BooleanArray = TODO()
      override fun getChar(attributeName: String): Char = TODO()
      override fun getCharArray(attributeName: String): CharArray = TODO()
      override fun getShort(attributeName: String): Short = TODO()
      override fun getShortArray(attributeName: String): ShortArray = TODO()
      override fun getInt(attributeName: String): Int = TODO()
      override fun getIntArray(attributeName: String): IntArray = TODO()
      override fun getLong(attributeName: String): Long = TODO()
      override fun getLongArray(attributeName: String): LongArray = TODO()
      override fun getDouble(attributeName: String): Double = TODO()
      override fun getDoubleArray(attributeName: String): DoubleArray = TODO()
      override fun getFloat(attributeName: String): Float = TODO()
      override fun getFloatArray(attributeName: String): FloatArray = TODO()
      override fun getString(attributeName: String): String = TODO()
      override fun getClass(attributeName: String): Class<*> = TODO()
      override fun getClassArray(attributeName: String): Array<out Class<*>> = TODO()
      override fun <E : Enum<E>> getEnum(attributeName: String, type: Class<E>): E = TODO()
      override fun <E : Enum<E>> getEnumArray(attributeName: String, type: Class<E>): Array<out E> = TODO()
      override fun <T : Annotation> getAnnotation(attributeName: String, type: Class<T>): MergedAnnotation<T> = TODO()
      override fun <T : Annotation> getAnnotationArray(attributeName: String, type: Class<T>): Array<out MergedAnnotation<T>> = TODO()
      override fun getValue(attributeName: String): Optional<Any> = TODO()
      override fun <T : Any> getValue(attributeName: String, type: Class<T>): Optional<T> = TODO()
      override fun getDefaultValue(attributeName: String): Optional<Any> = TODO()
      override fun <T : Any> getDefaultValue(attributeName: String, type: Class<T>): Optional<T> = TODO()
      override fun filterDefaultValues(): MergedAnnotation<Bean> = TODO()
      override fun filterAttributes(predicate: Predicate<String>): MergedAnnotation<Bean> = TODO()
      override fun withNonMergedAttributes(): MergedAnnotation<Bean> = TODO()
      override fun asAnnotationAttributes(vararg adaptations: Adapt): AnnotationAttributes = TODO()
      override fun asMap(vararg adaptations: Adapt): Map<String, Any> = TODO()
      override fun <T : Map<String, Any>> asMap(factory: Function<MergedAnnotation<*>, T>, vararg adaptations: Adapt): T = TODO()
      override fun synthesize(): Bean = TODO()
      override fun synthesize(condition: Predicate<in MergedAnnotation<Bean>>): Optional<Bean> = TODO()
      // endregion Not needed for tests
    }
  }
}
