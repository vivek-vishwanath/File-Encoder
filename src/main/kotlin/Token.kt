import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.*

fun JSON.Companion.parse(file: File): JSONObject {
    val scanner = Scanner(file)
    val builder = StringBuilder()
    while (scanner.hasNextLine()) builder.append(scanner.nextLine())
    return parse(builder.toString())
}

fun JSON.Companion.parse(text: String) = parse(tokenize(text))

fun JSON.Companion.encode(from: File, to: File) =
    FileOutputStream(to).write(parse(from).toBytes())

fun JSON.Companion.decode(from: File, to: File) {
    val writer = FileWriter(to)
    val array = ByteArray(from.length().toInt())
    FileInputStream(from).read(array)
    writer.write(fromBytes(array).toString())
    writer.flush()
}

private fun tokenize(text: String): LinkedList<Token> {
    val tokens = LinkedList<Token>()
    var inString = false
    var escape = false
    var index = -1
    for ((i, c) in text.withIndex()) {
        if (inString) {
            if (escape) escape = false
            else if (c == '\\') escape = true
            else if (c == '"') {
                inString = false
                tokens.add(StringToken(text.substring(index + 1, i)))
            }
        } else
            when {
                c == '{' || c == '[' -> tokens.add(OpenToken(c))
                c == '}' || c == ']' -> tokens.add(ClosedToken(c))
                c == ',' -> tokens.add(CommaToken())
                c == ':' -> tokens.add(ColonToken())
                c == '"' -> {
                    inString = true
                    index = i
                }

                c.isDigit() || c == '.' -> {
                    val last = tokens.last()
                    if (last is NumberToken)
                        tokens[tokens.lastIndex] = NumberToken("${last.value}$c")
                    else tokens.add(NumberToken("$c"))
                }

                c.isLetter() -> {
                    val last = tokens.last()
                    if (last is LetterToken)
                        tokens[tokens.lastIndex] = LetterToken("${last.value}$c")
                    else tokens.add(LetterToken("$c"))
                }
            }
    }
    return tokens
}

private fun parse(tokens: LinkedList<Token>): JSONObject {
    tokens.poll()
    var key: String? = null
    val root = JSONObject()
    var focus: JSONCollection = root
    while (tokens.size > 0) {
        val token = tokens.pop()
        if (token is StringToken && focus is JSONObject && key == null) {
            key = token.value
            continue
        }
        val value: JSON? = when (token) {
            is StringToken -> when (focus) {
                is JSONObject -> JSONString(token.value)
                is JSONArray -> JSONString(token.value)
                else -> throw RuntimeException()
            }

            is NumberToken -> token.value.toIntOrNull()?.let { JSONInt(it) } ?: JSONDouble(token.value.toDouble())
            is LetterToken -> if (token.value == "true") JSONBoolean(true) else if (token.value == "false") JSONBoolean(
                false
            ) else if (token.value == "null") JSONNull() else throw RuntimeException()
            is OpenToken -> if (token.value == "{") JSONObject() else JSONArray()
            is ClosedToken -> {
                focus = focus.parent
                    ?: if (tokens.size > 0) throw RuntimeException("Mismatched opening and closing tokens") else break
                null
            }

            else -> null
        }
        if (value != null)
            if (focus is JSONObject) {
                focus[key!!] = value
                key = null
            } else if (focus is JSONArray) focus += value
        if (token is OpenToken) focus = value as JSONCollection
    }
    return root
}

open class Token(val value: String) {

    override fun toString() = value
}

class OpenToken(value: Char) : Token("$value")
class ClosedToken(value: Char) : Token("$value")
class CommaToken : Token(",")
class ColonToken : Token(":")
class StringToken(value: String) : Token(value)
class NumberToken(value: String) : Token(value)
class LetterToken(value: String) : Token(value)