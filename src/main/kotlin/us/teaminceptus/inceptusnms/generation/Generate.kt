@file:JvmName("Main")

package us.teaminceptus.inceptusnms.generation

import us.teaminceptus.inceptusnms.generation.Util.getJavaPackages
import us.teaminceptus.inceptusnms.generation.Util.log
import java.io.File
import java.nio.file.Paths

fun main(args: Array<String>) {
    if (args.size != 2) {
        log("Usage: ./gradlew generate -Parg1=<input docs folder> -Parg2=<output directory>")
        return
    }

    val input = File(args[0])
    val output = File(args[1])

    if (!input.exists()) {
        log("Input documentation folder does not exist!")
        return
    }

    if (!output.exists())
        output.mkdirs()

    // element-list
    val packages = getJavaPackages(input)

    File(output, "element-list").apply {
        createNewFile()
        writeText(packages.joinToString("\n"))
    }
    log("Created element-list...")

    // HTML Files
    log("Generating HTML Pages...")
    Util.getClassDocumentation(input) // Load all documentation
    DocGenerator.generatePages(input, output)

    // JS Scripts
    log("Generating JavaScript Files...")
    JSGenerator.generateScripts(output, packages)

    // Copy Resources
    log("Copying Final Resources...")
    File(Paths.get("").toAbsolutePath().toString(), "/src/main/resources/javadoc")
        .copyRecursively(output)

    log("Done!")
}