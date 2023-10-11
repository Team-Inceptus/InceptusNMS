package us.teaminceptus.inceptusnms.generation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import us.teaminceptus.inceptusnms.generation.DocGenerator.url
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
        "bytebuf" to "io.netty.buffer.ByteBuf",
        "nullable" to "javax.annotation.Nullable",
    )

    fun mapTypeAliases(type: String): String {
        var newType = type.replace("[\\[\\]]".toRegex(), "")
        val classes = type.split("[<>,]".toRegex()).filter { it.isNotEmpty() }

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

    fun getClassDocumentation(): List<ClassDocumentation> {
        return LOADED_DOCUMENTATION.toList()
    }

    private val SCHEDULED = mutableListOf<String>()

    fun getScheduled(): List<String> = SCHEDULED.toList()

    suspend fun loadDocumentation(input: File) = withContext(Dispatchers.IO) {
        val files = input.walkTopDown().filter { it.isFile && it.name.endsWith(".json") }

        files.forEach { file -> SCHEDULED.add(getJavaName(file)) }

        files.forEach { file ->
            launch(Dispatchers.IO) {
                val name = getJavaName(file)
                val obj = Json.parseToJsonElement(file.readText()).jsonObject

                ClassDocumentation.fromJson(name, obj, LOADED_DOCUMENTATION::add)
            }
        }
    }

    fun getSubclasses(info: ClassDocumentation): List<ClassDocumentation> {
        val subclasses = mutableListOf<ClassDocumentation>()
        for (clazz in getClassDocumentation())
            if (info.type == "interface") {
                if (clazz.implements.contains(info.name)) {
                    subclasses.add(clazz)
                    subclasses.addAll(getSubclasses(clazz))
                }
            } else {
                if (clazz.extends == info.name) {
                    subclasses.add(clazz)
                    subclasses.addAll(getSubclasses(clazz))
                }
            }

        return subclasses
    }

    fun getImplements(info: ClassDocumentation): List<String> {
        val implements = mutableListOf<String>()
        val classes = getClassDocumentation()

        if (info.implements.isNotEmpty())
            implements.addAll(info.implements)

        val extends = classes.firstOrNull { it.name == info.extends }
        if (extends != null)
            implements.addAll(getImplements(extends))

        for (implement in info.implements.filter { clazz -> classes.map { it.name }.contains(clazz) })
            implements.addAll(getImplements(classes.first { it.name == implement }))

        return implements.distinct()
    }

    internal val REPOSITORIES = listOf(
        "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/",
        "https://hub.spigotmc.org/javadocs/spigot/",
        "https://www.slf4j.org/apidocs/",
        "https://repo.karuslabs.com/repository/brigadier/",
        "https://kvverti.github.io/Documented-DataFixerUpper/snapshot/",
        "https://javadoc.scijava.org/Guava/",
        "https://www.javadoc.io/static/com.google.code.findbugs/jsr305/3.0.2/",
        "https://joml-ci.github.io/JOML/apidocs/",
        "https://fastutil.di.unimi.it/docs/",
        "https://netty.io/4.1/api/"
    )

    fun repository(name: String): String = when {
        name.startsWith("javax") -> REPOSITORIES[6]
        name.startsWith("java") -> REPOSITORIES[0]
        name.startsWith("org.bukkit") -> REPOSITORIES[1]
        name.startsWith("org.slf4j") -> REPOSITORIES[2]
        name.startsWith("com.mojang.brigadier") -> REPOSITORIES[3]
        name.startsWith("com.mojang.datafixers") || name.startsWith("com.mojang.serialization") -> REPOSITORIES[4]
        name.startsWith("com.google.common") -> REPOSITORIES[5]
        name.startsWith("org.joml") -> REPOSITORIES[7]
        name.startsWith("it.unimi.dsi.fastutil") -> REPOSITORIES[8]
        name.startsWith("io.netty") -> REPOSITORIES[9]
        else -> throw IllegalArgumentException("Could not find Exteral Repository for $name")
    }

    internal fun connect(url: String): Document {
        return Jsoup.connect(url)
            .userAgent("Team-Inceptus/InceptusNMS")
            .get()
    }

    private fun String.noGenerics() = this
        .substringBefore("<")
        .substringBefore("&lt;")

    fun getExternalExtends(name: String): List<String> {
        if (name.contains("net.minecraft") || name.contains("org.bukkit.craftbukkit")) return listOf("java.lang.Object") // Undocumented

        val repository = repository(name)
        val doc = connect("$repository${name.substringBefore("<").url()}.html")
        val inheritance = doc.select("div.inheritance") + doc.select("ul.inheritance > li")

        return inheritance.map {
            (it.selectFirst("a")?.text() ?: it.text()).noGenerics()
        }.filter { it.isNotEmpty() }.reversed().distinct()
    }

    fun getHierarchyTree(info: ClassDocumentation, includeSelf: Boolean = true): List<String> {
        if (info.extends == null || info.type == "interface" || info.type == "annotation")
            return if (includeSelf) listOf(info.name) else emptyList()

        val tree = mutableListOf("java.lang.Object")
        val classes = getClassDocumentation()

        if (info.extends != "java.lang.Object") {
            val extends = classes.firstOrNull { it.name == info.extends }
            if (extends != null)
                tree.addAll(getHierarchyTree(extends))
            else
                tree.addAll(getExternalExtends(info.extends))

            tree.add(info.extends)
        }

        if (includeSelf)
            tree.add(info.fullDocName)

        return tree.distinct()
    }

    fun getExternalMethods(name: String): List<String> {
        if (name.contains("net.minecraft") || name.contains("org.bukkit.craftbukkit")) return listOf() // Undocumented

        val repository = repository(name)
        val url = "$repository${name.url()}.html"
        val doc = connect(url)

        val methods8 = doc.selectFirst("a[name=\"method.summary\"] ~ table.memberSummary > tbody")?.select("tr")?.mapNotNull {
            val text = it.selectFirst("td.colLast > code > span.memberNameLink")?.text() ?: return@mapNotNull null
            val href = ("#" + it.selectFirst("td.colLast > code > span.memberNameLink")?.attr("href")?.substringAfterLast('#'))

            text to href
        } ?: emptyList()

        val methods11 = (doc.select("div.col-second.method-summary-table")
            .mapNotNull { it.selectFirst("code") }
            .mapNotNull { it.selectFirst("a") }
            .map { it.text() to it.attr("href") })

        return (methods8 + methods11).sortedBy { it.first }.map {
            "<a href=\"$url${it.second}\">${it.first}</a>"
        }
    }

    fun getInheritedMethods(info: ClassDocumentation): Map<String, Map<String, List<String>>> {
        val classes = getClassDocumentation()

        fun create(tree: List<String>): Map<String, List<String>> {
            val tree0 = tree.map { it.noGenerics() }
            val map = mutableMapOf<String, List<String>>()

            for (clazz in tree0) {
                val current = classes.firstOrNull { it.name == clazz }

                if (current?.methods?.methods?.isNotEmpty() == true) {
                    map[clazz] = current.methods.methods.sortedBy { it.fullName }.map {
                        "<a href=\"/${clazz.url()}.html#${it.name}\">${it.name}</a>"
                    }
                } else
                    map[clazz] = getExternalMethods(clazz)
            }

            return map
        }

        return mutableMapOf(
            "class" to create(getHierarchyTree(info, false).reversed()),
            "interface" to create(getImplements(info).sorted())
        )
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