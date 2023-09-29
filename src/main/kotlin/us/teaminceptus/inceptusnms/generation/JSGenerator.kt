package us.teaminceptus.inceptusnms.generation

import kotlinx.serialization.json.*
import java.io.File

object JSGenerator {

    fun generateScripts(output: File, packages: Set<String>) {
        generatePackageSearchIndex(packages, output)
        println("Created package-search-index.js...")

        println("Finished Generating JavaScript Files!")
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
        }


        File(output, "package-search-index.js").apply {
            createNewFile()
            writeText("packageSearchIndex = $list;updateSearchResults();")
        }
    }

}