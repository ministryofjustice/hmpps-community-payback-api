package uk.gov.justice.digital.hmpps.communitypaybackapi

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.random.Random
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Provides a [TestExecutionListener] that hooks [TestRandom] into the test lifecycle so that random number generation
 * is replayable. This allows sporadically failing tests to be diagnosed more reliably as the failing run can be
 * reproduced.
 */
class RandomSeedTestExecutionListener : TestExecutionListener {
  private val logger = LoggerFactory.getLogger(javaClass)

  override fun testPlanExecutionStarted(testPlan: TestPlan) {
    TestRandom.setupHook()

    logBanner("Root seed is ${TestRandom.rootSeed}.")

    testPlan.accept(object : TestPlan.Visitor {
      override fun visit(testIdentifier: TestIdentifier) {
        TestRandom.setupRandomForTest(testIdentifier)
      }
    })
  }

  override fun dynamicTestRegistered(testIdentifier: TestIdentifier) {
    TestRandom.setupRandomForTest(testIdentifier)
    super.dynamicTestRegistered(testIdentifier)
  }

  override fun executionStarted(testIdentifier: TestIdentifier) {
    logger.info("Starting test '${testIdentifier.displayName}' with test-specific seed ${TestRandom.getSeedForTest(testIdentifier)}.")
    TestRandom.pushCurrentTestOntoThread(testIdentifier)
    super.executionStarted(testIdentifier)
  }

  override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
    TestRandom.popCurrentTestFromThread()
    super.executionFinished(testIdentifier, testExecutionResult)
  }

  override fun testPlanExecutionFinished(testPlan: TestPlan) {
    logBanner(
      """
        |To replay this test run, set the environment variable:
        |
        |${TestRandom.ROOT_SEED_ENVIRONMENT_VARIABLE}=${TestRandom.rootSeed}
      """.trimMargin(),
    )
    super.testPlanExecutionFinished(testPlan)
  }

  private fun logBanner(message: String) = logger.info(
    """
      |================================================================================
      |
      |    ${message.replace("\n", "\n    ")}
      |
      |================================================================================
    """.trimMargin(),
  )
}

/**
 * Provides a replayable, parallelisable, test-scoped random number generator that hooks into [Random.Default] when the
 * [setupHook] method is called.
 *
 * A test is registered using [setupRandomForTest], which generates a test-specific seed based on the test's name and
 * the root seed.
 *
 * Calling [pushCurrentTestOntoThread] when a test starts appends a new [Random] instance with the test-specific seed
 * onto a stack maintained for the current thread. This allows the test's calls to `Random` to always start from a known
 * state, even when the order is not guaranteed, such as when running tests in parallel.
 *
 * Calling [popCurrentTestFromThread] when a test finishes removes the current `Random` instance from the top of the
 * current thread's stack, resuming the previous instance. This allows situations where [org.junit.jupiter.api.Nested]
 * tests exist at multiple levels in a hierarchy to be handled gracefully.
 */
@OptIn(ExperimentalAtomicApi::class)
private object TestRandom : Random() {
  const val ROOT_SEED_ENVIRONMENT_VARIABLE = "COMMUNITY_PAYBACK_API_TEST_ROOT_SEED"

  private val logger = LoggerFactory.getLogger(javaClass)

  val rootSeed by lazy {
    val testSeedEnv = System.getenv(ROOT_SEED_ENVIRONMENT_VARIABLE).runCatching { toLong() }

    val seed = testSeedEnv
      .onSuccess { logger.info("Root seed set from environment variable $ROOT_SEED_ENVIRONMENT_VARIABLE.") }
      .onFailure { logger.info("Environment variable $ROOT_SEED_ENVIRONMENT_VARIABLE not set, using system time.") }
      .getOrElse { System.nanoTime() }

    seed
  }

  private val uniquifier by lazy { Random(rootSeed).nextLong() }

  private val currentThreadRandom: ThreadLocal<ArrayDeque<Random>> = ThreadLocal.withInitial { ArrayDeque() }

  private val testSeeds = mutableMapOf<TestIdentifier, Long>()

  fun setupRandomForTest(testIdentifier: TestIdentifier) {
    testSeeds[testIdentifier] = testIdentifier.displayName.hashCode().toLong() xor uniquifier
  }

  fun pushCurrentTestOntoThread(testIdentifier: TestIdentifier) {
    val seed = testSeeds[testIdentifier] ?: throw IllegalStateException("Test seed not set for current test.")
    currentThreadRandom.get().addLast(Random(seed))
  }

  fun popCurrentTestFromThread() {
    currentThreadRandom.get().removeLast()
  }

  fun getSeedForTest(testIdentifier: TestIdentifier) = testSeeds[testIdentifier]

  override fun nextBits(bitCount: Int): Int = currentThreadRandom.get().lastOrNull()?.nextBits(bitCount)
    ?: throw IllegalStateException("Random instance not set for current test.")

  fun setupHook() {
    val defaultRandomInnerField = Random.Default::class.memberProperties
      .first { it.name == "defaultRandom" }
      .javaField!!
      .apply { isAccessible = true }

    defaultRandomInnerField.setValueForPrivateStaticFinal(TestRandom)
  }
}

// The developers of the JDK don't want you to be able to modify `private static final` fields.
//
// There are good reasons for this.
//
// However, for the purposes of running the tests in a way that allows them to be repeated, there are two options:
// 1. Crack open the JVM like a walnut and stuff our own `Random` instance inside `Random.Default`
// 2. Refactor everywhere a method that relies on `Random.Default` is called to use a configurable global instance
//
// Given the scope of the change involved with the second approach, and the fact that this is not production-path code,
// the first option provides a quick way to do this without incurring too much risk.
//
// At some point, the JDK developers will probably close this loophole, so this should be thought of as a transitional
// step towards doing it properly with the second approach.
//
// Adapted for Kotlin from https://stackoverflow.com/a/77705202
// Posted by tesmo, modified by community. See post 'Timeline' for change history
// Retrieved 2026-06-26, License - CC BY-SA 4.0
private fun Field.setValueForPrivateStaticFinal(value: Any?) {
  removeFinal()

  val memberNameClass = Class.forName("java.lang.invoke.MemberName")
  val memberNameCtor = memberNameClass.getDeclaredConstructor(Field::class.java, Boolean::class.java)
    .apply { this.isAccessible = true }

  val memberNameInstanceForField = memberNameCtor.newInstance(this, true)

  memberNameClass.getDeclaredField("flags").apply { this.isAccessible = true }.apply {
    setInt(memberNameInstanceForField, getInt(memberNameInstanceForField) and Modifier.FINAL.inv())
  }

  val getReferenceKindMethod = memberNameClass.getDeclaredMethod("getReferenceKind").apply { this.isAccessible = true }

  val getReferenceKind = getReferenceKindMethod.invoke(memberNameInstanceForField) as Byte

  val methodHandle = MethodHandles.privateLookupIn(this.declaringClass, MethodHandles.lookup())
  val getDirectFieldCommonMethod = methodHandle.javaClass
    .getDeclaredMethod("getDirectFieldCommon", Byte::class.java, Class::class.java, memberNameClass)
    .apply { this.isAccessible = true }

  val invoker = getDirectFieldCommonMethod.invoke(
    methodHandle,
    getReferenceKind,
    this.declaringClass,
    memberNameInstanceForField,
  ) as MethodHandle

  invoker.invoke(value)
}

@Suppress("unchecked_cast")
private fun Field.removeFinal() {
  val classMethods = Class::class.java.declaredMethods
  val declaredFieldsMethod = classMethods.first { it.name == "getDeclaredFields0" }.apply { isAccessible = true }
  val declaredFieldsOfField = declaredFieldsMethod.invoke(Field::class.java, false) as Array<Field>
  val modifiersField = declaredFieldsOfField.first { it.name == "modifiers" }.apply { isAccessible = true }
  modifiersField.setInt(this, this.modifiers and Modifier.FINAL.inv())
}
