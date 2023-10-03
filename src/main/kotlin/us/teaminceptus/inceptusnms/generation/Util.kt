package us.teaminceptus.inceptusnms.generation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Util {

    private val LOADED_DOCUMENTATION = mutableListOf<ClassDocumentation>()

    private val TYPE_ALIASES = mapOf(
        "obj" to "java.lang.Object",
        "wboolean" to "java.lang.Boolean",
        "wbyte" to "java.lang.Byte",
        "wshort" to "java.lang.Short",
        "wint" to "java.lang.Integer",
        "wlong" to "java.lang.Long",
        "wfloat" to "java.lang.Float",
        "wdouble" to "java.lang.Double",
        "wchar" to "java.lang.Character",
        "wshort" to "java.lang.Short",
        "file" to "java.io.File",
        "string" to "java.lang.String",
        "date" to "java.util.Date",
        "component" to "net.minecraft.network.chat.Component",
        "codec" to "com.mojang.serialization.Codec",
        "deprecated" to "java.lang.Deprecated",
        "bytebuf" to "io.netty.buffer.ByteBuf"
    )

    fun mapTypeAliases(type: String): String {
        var newType = type.replace("[\\[\\]]".toRegex(), "")
        val classes = type.split("[<>,]".toRegex()).filterNot { it.isEmpty() }

        for (clazz in classes)
            newType = newType.replace(clazz, TYPE_ALIASES[clazz] ?: continue)

        val arrayBuilder = StringBuilder()
        for (i in 0 until type.count { it == '[' })
            arrayBuilder.append("[]")

        return newType + arrayBuilder.toString()
    }

    fun getAllJavaPackages(root: File): List<String> {
        val packages = mutableSetOf<String>()
        root.walkTopDown().forEach {
            if (it.isFile && it.name.endsWith(".json"))
                packages.add(getJavaPackage(it))
        }

        return packages.sorted()
    }

    fun getJavaPackage(file: File): String {
        assert(file.isFile) { "File is not a file!" }
        assert(file.exists()) { "File does not exist!" }

        val path = file.absolutePath
        val filePath = path.split("docs")[1].substring(1).replace("[/\\\\]".toRegex(), ".")

        return filePath.substring(0, filePath.length - (file.name.length + 1))
    }

    fun getJavaName(file: File): String {
        assert(file.isFile) { "File is not a file!" }
        assert(file.exists()) { "File does not exist!" }

        val pkg = getJavaPackage(file)
        val name = file.name.split(".json")[0]

        return "$pkg.$name"
    }

    fun getClassDocumentation(input: File? = null): List<ClassDocumentation> {
        if (LOADED_DOCUMENTATION.isEmpty() && input != null)
            input.walkTopDown().filter { it.isFile && it.name.endsWith(".json") }.forEach { file ->
                LOADED_DOCUMENTATION.add(
                    ClassDocumentation.fromJson(
                        getJavaName(file),
                        Json.parseToJsonElement(file.readText()).jsonObject
                    )
                )
            }

        return LOADED_DOCUMENTATION.toList()
    }
    
    // Logging
    
    fun log(message: String) {
        println("[${Thread.currentThread().name.uppercase()
            .substringAfter("-")} ${SimpleDateFormat("MMM dd, YYYY hh:mm:ss a").format(Date())}] $message")
    }

    fun error(message: String) {
        System.err.println("[${Thread.currentThread().name.uppercase()
            .substringAfter("-")} ${SimpleDateFormat("MMM dd, YYYY hh:mm:ss a").format(Date())}] $message")
    }

}