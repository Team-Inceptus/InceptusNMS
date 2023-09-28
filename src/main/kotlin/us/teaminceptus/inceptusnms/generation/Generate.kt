@file:JvmName("Main")

package us.teaminceptus.inceptusnms.generation

import us.teaminceptus.inceptusnms.generation.Util.getJavaPackages
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage: ./gradlew generate -Parg1=<input docs folder> -Parg2=<output directory>")
        return
    }

    val input = File(args[0])
    val output = File(args[1])

    if (!input.exists()) {
        println("Input documentation folder does not exist!")
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
    println("Created element-list...")

    // JS Scripts
    println("Generating JavaScript Files...")
    JSGenerator.generateScripts(output, packages)

    // Copy Resources
    println("Copying Final Resources...")
    File(Paths.get("").toAbsolutePath().toString(), "/src/main/resources/javadoc")
        .copyRecursively(output)

    println("Done!")
}