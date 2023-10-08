@file:JvmName("Main")

package us.teaminceptus.inceptusnms.validation

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import us.teaminceptus.inceptusnms.generation.Util.error
import us.teaminceptus.inceptusnms.generation.Util.getJavaName
import us.teaminceptus.inceptusnms.generation.Util.getJavaPackage
import us.teaminceptus.inceptusnms.generation.Util.log
import java.io.File
import java.lang.AssertionError

fun main(args: Array<String>) {
    try {
        if (args.size != 1) {
            error("Usage: ./gradlew validate -Parg1=<input docs folder>")
            return
        }

        val input = File(args[0])
        assert(input.exists()) { "Input documentation folder does not exist!" }

        val files = input.walkTopDown().filter { it.name.contains(".json") }
        files.forEach { file ->
            try {
                val json = Json.parseToJsonElement(file.readText()).jsonObject

                validateClass(file, json)
            } catch (e: SerializationException) {
                error("Invalid JSON File: ${file.absolutePath}")
            }
        }
    } catch (e: AssertionError) {
        error(e.message ?: "Assertion Error")
    }
}

fun validateClass(file: File, json: JsonObject) {
    assert(json.contains("class") && json["class"]?.jsonObject?.contains("type") == true) { "Class must have a type!" }

    val classType = json["class"]!!.jsonObject["type"]!!.jsonPrimitive.content
    assert(classType == "interface" || classType == "class" || classType == "enum" || classType == "record" || classType == "annotation") { "Class type must be either interface, class, enum, annotation, or record!" }

    if (classType == "enum")
        assert(json.contains("enumerations")) { "Enum must have enumerations!" }

    if (classType == "interface")
        assert(!json.contains("constructors")) { "Interface cannot have constructors!" }

    log("Class ${getJavaName(file)} validated")
}