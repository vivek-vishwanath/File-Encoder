import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


abstract class JSON {

    var parent: JSONCollection? = null

    val value
        get() = when (this) {
            is JSONString -> string
            is JSONInt -> int
            is JSONDouble -> num
            is JSONBoolean -> boolean
            is JSONNull -> null
            else -> throw RuntimeException()
        }

    abstract fun print(depth: Int): String

    abstract fun toBytes(): ByteArray

    open operator fun get(key: String) = (this as JSONObject)[key]

    open operator fun get(index: Int) = (this as JSONArray)[index]

    companion object {

        fun fromBytes(array: ByteArray) = fromBytes(array.toCollection(LinkedList()))

        fun fromBytes(bytes: LinkedList<Byte>): JSON {
            val selector = bytes.pop()
            return when (selector.toInt()) {
                0 -> JSONObject.fromBytes(bytes)
                1 -> JSONArray.fromBytes(bytes)
                2 -> JSONString.fromBytes(bytes)
                3 -> JSONInt.fromBytes(bytes)
                4 -> JSONDouble.fromBytes(bytes)
                5 -> JSONBoolean(false)
                6 -> JSONBoolean(true)
                7 -> JSONNull()
                else -> throw RuntimeException("Impossible")
            }
        }
    }
}

abstract class JSONCollection : JSON()

class JSONObject : JSONCollection() {
    private val map = HashMap<String, JSON>()

    private val list get() = map.toList()

    override operator fun get(key: String) = map[key]!!

    operator fun set(key: String, value: JSON) {
        map[key] = value
        value.parent = this
    }

    override fun print(depth: Int): String {
        if (map.size == 0) return "{}"
        val builder = StringBuilder("{")
        for (entry in map) {
            builder.append("\n").append("\t".repeat(depth)).append("\"${entry.key}\"").append(": ")
                .append(entry.value.print(depth + 1)).append(",")
        }
        return builder.substring(0, builder.length - 1) + "\n${"\t".repeat(depth - 1)}}"
    }

    override fun toString() = print(1)

    override fun toBytes() = byteArrayOf(
        0,
        *list.size.toBytes(),
        *Array(map.size) {
            byteArrayOf(
                *list[it].first.length.toBytes(),
                *list[it].first.toByteArray(),
                *list[it].second.toBytes()
            )
        }.flatMap { it.toList() }.toByteArray()
    )

    companion object {
        fun fromBytes(bytes: LinkedList<Byte>): JSONObject {
            val size = bytes.toInt()
            val map = JSONObject()
            for (i in 0..<size) {
                val key = String(ByteArray(bytes.toInt()) { bytes.pop() })
                val value = JSON.fromBytes(bytes)
                map[key] = value
            }
            return map
        }
    }
}

class JSONArray : JSONCollection() {
    private val array = ArrayList<JSON>()

    override operator fun get(index: Int) = array[index]

    operator fun plusAssign(value: JSON) {
        array.add(value)
        value.parent = this
    }

    override fun print(depth: Int): String {
        if (array.size == 0) return "[]"
        val builder = StringBuilder("[")
        for (entry in array) {
            builder.append("\n").append("\t".repeat(depth)).append(entry.print(depth + 1)).append(",")
        }
        return builder.substring(0, builder.length - 1) + "\n${"\t".repeat(depth - 1)}]"
    }

    override fun toBytes() = byteArrayOf(
        1,
        *array.size.toBytes(),
        *Array(array.size) { array[it].toBytes() }.flatMap { it.asList() }.toByteArray()
    )

    companion object {
        fun fromBytes(bytes: LinkedList<Byte>): JSONArray {
            val size = bytes.toInt()
            val map = JSONArray()
            for (i in 0..<size)
                map += JSON.fromBytes(bytes)
            return map
        }
    }
}

class JSONString(val string: String) : JSON() {
    override fun print(depth: Int) = "\"$string\""

    override fun toBytes() = byteArrayOf(2, *string.length.toBytes(), *string.toByteArray())

    companion object {
        fun fromBytes(bytes: LinkedList<Byte>) = JSONString(String(ByteArray(bytes.toInt()) { bytes.pop() }))
    }
}

class JSONInt(val int: Int) : JSON() {
    override fun print(depth: Int) = int.toString()

    override fun toBytes() = byteArrayOf(3, *int.toBytes())

    companion object {
        fun fromBytes(bytes: LinkedList<Byte>) = JSONInt(bytes.toInt())
    }
}

class JSONDouble(val num: Double) : JSON() {
    override fun print(depth: Int) = num.toString()

    override fun toBytes() = byteArrayOf(4, *num.toBytes())

    companion object {
        fun fromBytes(bytes: LinkedList<Byte>) = JSONDouble(bytes.toDouble())
    }
}

class JSONBoolean(val boolean: Boolean) : JSON() {
    override fun print(depth: Int) = boolean.toString()

    override fun toBytes() = byteArrayOf(if (boolean) 6 else 5)
}

class JSONNull : JSON() {
    override fun print(depth: Int) = "null"

    override fun toBytes() = byteArrayOf(7)
}

val data = HashMap<Number, Int>()

inline fun <reified T : Number> T.toBytes(): ByteArray {
    val size = when (this) {
        is Int -> 4
        is Double -> 8
        else -> throw IllegalArgumentException("Unsupported type: ${T::class.java}")
    }

    val byteBuffer = ByteBuffer.allocate(size)
    when (this) {
        is Int -> byteBuffer.putInt(this)
        is Double -> byteBuffer.putDouble(this)
        else -> throw IllegalArgumentException("Unsupported type: ${T::class.java}")
    }
    return byteBuffer.array()
}

inline fun <reified T : Number> ByteArray.fromBytes(): T {
    val byteBuffer = ByteBuffer.wrap(this)
    return when (T::class) {
        Int::class -> byteBuffer.int
        Double::class -> byteBuffer.double
        else -> throw IllegalArgumentException("Unsupported type: ${T::class.java}")
    } as T
}

fun LinkedList<Byte>.toInt() = ByteArray(4) { pop() }.fromBytes<Int>()
fun LinkedList<Byte>.toDouble() = ByteArray(8) { pop() }.fromBytes<Double>()