@file:JvmName("Main")

package us.teaminceptus.inceptusnms.generation

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import us.teaminceptus.inceptusnms.generation.Util.getJavaPackages
import us.teaminceptus.inceptusnms.generation.Util.log
import java.io.File
import java.nio.file.Paths

suspend fun main(args: Array<String>): Unit = coroutineScope {
    if (args.size != 2) {
        log("Usage: ./gradlew generate -Parg1=<input docs folder> -Parg2=<output directory>")
        return@coroutineScope
    }

    val input = File(args[0])
    val output = File(args[1])

    if (!input.exists()) {
        log("Input documentation folder does not exist!")
        return@coroutineScope
    }

    if (!output.exists())
        output.mkdirs()

    val packages = getJavaPackages(input)
    Util.getClassDocumentation(input) // Load all documentation

    // element-list
    launch {
        File(output, "element-list").apply {
            createNewFile()
            writeText(packages.joinToString("\n"))
        }
        log("Created element-list...")
    }

    // HTML Files
    launch {
        log("Generating HTML Pages...")
        DocGenerator.generatePages(input, output)
    }

    // JS Scripts
    launch {
        log("Generating JavaScript Files...")
        JSGenerator.generateScripts(output, packages)
    }

    // Copy Resources
    launch {
        log("Copying Final Resources...")
        File(Paths.get("").toAbsolutePath().toString(), "/src/main/resources/javadoc")
            .copyRecursively(output)
    }
}