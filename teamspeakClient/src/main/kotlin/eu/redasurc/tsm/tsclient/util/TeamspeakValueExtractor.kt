package eu.redasurc.tsm.tsclient.util

import org.slf4j.LoggerFactory
import javax.management.InvalidAttributeValueException
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType
import kotlin.streams.toList

/**
 * Delegator class to extract a given value from a teamspeak property map
 */
@Suppress("UNCHECKED_CAST")
abstract class ValueExtractorBase(private val map: Map<String, Any?>, private val altKey: List<String> = listOf()) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private fun getFromList(name: String) : Any? {
        return tryAllFormats(name)?.run { map[this] } ?: run {
            for (s in altKey) {
                tryAllFormats(s)?.run { return map[this] }
            }
            throw NoSuchElementException("Key $name & alternatives $altKey is missing in the map.")
        }
    }

    private fun tryAllFormats(name: String) : String? {
        if(map.containsKey(name)) return name
        val snakeCase = camel2Snake(name)
        if(map.containsKey(snakeCase)) return snakeCase
        return null
    }

    protected fun <V, V1 : V> valueExtract(thisRef: Any?, prop: KProperty<*>): V1 {
        val type = prop.returnType
        val value = getFromList(prop.name)
        return if(value is String && !type.javaClass.isAssignableFrom(String::class.java)) {
            when (type.javaType.typeName) {
                "int" -> value.toInt()
                "long" -> value.toLong()
                "double" -> value.toDouble()
                "boolean" -> {
                    when (value) {
                        "1" -> true
                        "0" -> false
                        else -> throw InvalidAttributeValueException("Boolean value should be either 1 or 0 but is $value")
                    }
                }
                "java.util.List<java.lang.Integer>" -> {
                    value.split(",").stream().map { it.toInt() }.toList()
                }
                "java.lang.String" -> {
                    if (value == "null") {
                        ""
                    } else {
                        value.toString()
                    }
                }
                else -> {
                    log.error("Unknown type ${type.javaType.typeName}, trying to simply cast it")
                    value
                }
            }

        } else {
            value
        } as V1
    }
}

/**
 * Moves one key of this map to another or does nothing if the key is not contained in this map.
 * @param from key to be moved
 * @param target target key to move to
 * @return modified copy of the map or original map if key not found
 */
fun <T> Map<String, T>.moveKey(from: String, target: String): Map<String, T> {
    val value = this[from] ?: return this
    return this.minus(from).plus(target to value)
}

/**
 * Returns a copy of the map with only the given keys present.
 * @param from key to be moved
 * @param target target key to move to
 * @return modified copy of the map or original map if key not found
 */
fun <T> Map<String, T>.onlyKeys(vararg keys: String): Map<String, T> {
    val retMap = mutableMapOf<String, T>()
    for (key in keys) {
        this[key] ?.run {
            retMap[key] = this
        }
    }
    return retMap
}

fun camel2Snake(str: String): String {

    val headInUpperCase = str.takeWhile{ it.isUpperCase() || it.isDigit()}
    val tailAfterHeadInUppercase = str.dropWhile{it.isUpperCase() || it.isDigit()}

    return if (tailAfterHeadInUppercase.isEmpty()) {
        headInUpperCase.toLowerCase()
    } else {
        val firstWord = if (!headInUpperCase.isEmpty()) {
            if(headInUpperCase.last().isDigit()){
                headInUpperCase
            } else {
                headInUpperCase.toLowerCase()
            }
        } else {
            headInUpperCase.toLowerCase() + tailAfterHeadInUppercase.takeWhile{it.isLowerCase()}
        }

        if (firstWord == str.toLowerCase()) {
            firstWord
        } else {
            val last = camel2Snake(str.drop(firstWord.length))
            if(firstWord == headInUpperCase.toLowerCase()) {
                "${firstWord}$last"
            } else {
                "${firstWord}_$last"
            }
        }

    }
}

class TeamspeakValueExtractor(map: Map<String, Any?>, altKey: List<String> = listOf()) : ValueExtractorBase(map, altKey) {
    constructor(map: Map<String, Any?>, vararg alt: String) : this(map, listOf(*alt))

    operator fun <V, V1 : V> getValue(thisRef: Any?, prop: KProperty<*>): V1 {
        return valueExtract<V, V1>(thisRef, prop)
    }

}
class NullableTeamspeakValueExtractor(map: Map<String, Any?>, altKey: List<String> = listOf()) : ValueExtractorBase(map, altKey) {
    constructor(map: Map<String, Any?>, vararg alt: String) : this(map, listOf(*alt))

    operator fun <V, V1 : V> getValue(thisRef: Any?, prop: KProperty<*>): V1? {
        return try {
            valueExtract<V, V1>(thisRef, prop)
        } catch (e: NoSuchElementException) {
            null
        }
    }

}
