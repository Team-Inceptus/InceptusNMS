@file:JvmName("Generate")

package us.teaminceptus.inceptusnms

import java.io.File

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

    // TODO: Generate the documentation
}