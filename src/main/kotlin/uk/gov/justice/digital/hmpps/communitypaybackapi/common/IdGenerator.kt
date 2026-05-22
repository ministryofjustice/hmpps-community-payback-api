package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import java.nio.ByteBuffer
import java.util.UUID
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * `IdGenerator` provides repeatable but random-looking UUIDs derived from the properties of a given object:
 *
 * ```
 * data class Point(val x: Float, val y: Float)
 *
 * val point1 = Point(0.0, 0.0)
 * val point2 = point1.copy()
 * val point3 = Point(1.0, 1.0)
 * val idGenerator = IdGenerator(Point::class)
 * val id1 = idGenerator.generateId(point1)
 * val id2 = idGenerator.generateId(point2) // id1 == id2: true
 * val id3 = idGenerator.generateId(point3) // id1 == id3: false
 * ```
 *
 * This can be used in situations where recomputability is more important than cryptographically secure
 * randomness, e.g. to determine if an object has been orphaned in NDelius and needs to be cleaned up
 * before attempting to recreate it.
 *
 * This generator is type-sensitive. This means that, for example:
 *
 * ```
 * data class Point(val x: Float, val y: Float)
 * data class Vector2(val x: Float, val y: Float)
 *
 * val pointId = IdGenerator(Point::class).generateId(Point(0.0, 0.0))
 * val vector2Id = IdGenerator(Vector2::class).generateId(Vector2(0.0, 0.0))
 * ```
 * ensures that `pointId` and `vector2Id` have different values, despite logically being equivalent.
 *
 * It also makes the assumption that the type parameter `T` is dataclass-like, although this is not enforced.
 * Using it on primitives or other types of object is not recommended, and may not work as expected.
 */
class IdGenerator<T : Any>(val cls: KClass<T>) {
  /**
   * Generates the ID representing the given properties.
   */
  fun generateId(properties: T): UUID {
    val props = listOf(cls.qualifiedName) + cls.memberProperties.map { prop -> prop.get(properties) }
    val hash = props.hashCode()

    // Construct a random Version 4 UUID.
    // This is almost identical to the `java.util.UUID` implementation, except:
    // - the `SecureRandom` instance is replaced with `Random` seeded with the hashed
    //   value of `properties`, so that multiple calls with equivalent properties always
    //   returns the same UUID
    // - the `UUID(byte[])` constructor is inaccessible so it must be constructed using
    //   the `UUID(Long, Long)` constructor, via a `ByteBuffer` to reinterpret the bytes
    val ng = Random(hash)

    val randomBytes = ByteArray(16)
    ng.nextBytes(randomBytes)
    randomBytes[6] = randomBytes[6] and 0x0f // clear version
    randomBytes[6] = randomBytes[6] or 0x40 // set to version 4
    randomBytes[8] = randomBytes[8] and 0x3f // clear variant
    randomBytes[8] = randomBytes[8] or 0x80.toByte() // set to IETF variant

    val buffer = ByteBuffer.wrap(randomBytes)
    return UUID(buffer.getLong(), buffer.getLong())
  }
}
