package us.teaminceptus.inceptusnms.generation

import kotlinx.serialization.json.*
import us.teaminceptus.inceptusnms.generation.Util.log
import java.io.File

object JSGenerator {

    fun generateScripts(output: File, packages: Set<String>) {
        generatePackageSearchIndex(packages, output)
        log("Created package-search-index.js")

        generateTypeSearchIndex(output)
        log("Created type-search-index.js")

        generateMemberSearchIndex(output)
        log("Created member-search-index.js")

        log("Finished Generating JavaScript Files!")
    }

    fun generatePackageSearchIndex(packages: Set<String>, output: File) {
        val list = buildJsonArray {
            add(JsonObject(mapOf(
                "l" to JsonPrimitive("All Packages"),
                "u" to JsonPrimitive("allpackages-index.html"),
            )))

            for (pkg in packages.sorted())
                add(JsonObject(mapOf(
                    "l" to JsonPrimitive(pkg)
                )))
        }.sortedBy { it.jsonObject["l"]!!.jsonPrimitive.content }

        File(output, "package-search-index.js").apply {
            createNewFile()
            writeText("packageSearchIndex = $list;updateSearchResults();")
        }
    }

    fun generateTypeSearchIndex(output: File) {
        val list = buildJsonArray {
            add(JsonObject(mapOf(
                "l" to JsonPrimitive("All Classes and Interfaces"),
                "u" to JsonPrimitive("allclasses-index.html"),
            )))

            for (type in Util.getClassDocumentation())
                add(JsonObject(mapOf(
                    "p" to JsonPrimitive(type.pkg),
                    "l" to JsonPrimitive(type.simpleName),
                )))
        }.sortedBy { it.jsonObject["l"]!!.jsonPrimitive.content }

        File(output, "type-search-index.js").apply {
            createNewFile()
            writeText("typeSearchIndex = $list;updateSearchResults();")
        }
    }

    fun generateMemberSearchIndex(output: File) {
        val list = buildJsonArray {
            for (type in Util.getClassDocumentation()) {
                for (enum in type.enumerations?.enums ?: emptyList())
                    add(JsonObject(mapOf(
                        "p" to JsonPrimitive(type.pkg),
                        "c" to JsonPrimitive(type.simpleName),
                        "l" to JsonPrimitive(enum.name)
                    )))

                for (field in type.fields?.fields ?: emptyList())
                    add(JsonObject(mapOf(
                        "p" to JsonPrimitive(type.pkg),
                        "c" to JsonPrimitive(type.simpleName),
                        "l" to JsonPrimitive(field.name),
                    )))

                for (method in type.methods?.methods ?: emptyList())
                    add(JsonObject(mapOf(
                        "p" to JsonPrimitive(type.pkg),
                        "c" to JsonPrimitive(type.simpleName),
                        "l" to JsonPrimitive(method.cleanName),
                        "u" to JsonPrimitive(method.fullName),
                    )))
            }
        }

        File(output, "member-search-index.js").apply {
            createNewFile()
            writeText("memberSearchIndex = $list;updateSearchResults();")
        }
    }

}