@file:JvmName("Main")

package us.teaminceptus.inceptusnms.template

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.DetectedVersion
import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import java.io.File
import java.util.Scanner

@OptIn(ExperimentalSerializationApi::class)
val printer = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

fun main(args: Array<String>) {
    // Boostrap
    val v = DetectedVersion::class.java.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
    SharedConstants.setVersion(v)
    Bootstrap.bootStrap();

    val clazz = try {
        Class.forName(args[0])
    } catch (e: ClassNotFoundException) {
        System.err.println("Class not found: ${args[0]}")
        return
    }

    val json = TemplateGenerator.generateTemplate(clazz)
    val file = File("../docs/${clazz.name.replace(".", "/")}.json")

    file.parentFile.mkdirs()
    file.writeText(printer.encodeToString(json))

    println("Generated template for ${clazz.name} at ${file.absolutePath}")
}