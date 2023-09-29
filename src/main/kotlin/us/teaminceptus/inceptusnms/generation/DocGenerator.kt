package us.teaminceptus.inceptusnms.generation

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.nio.file.Paths

object DocGenerator {

    fun generatePages(output: File) {
        File(output, "index.html").apply {
            createNewFile()
            writeText(generateIndex().outerHtml())
        }
        println("Created index.html...")

        println("Finished HTML Page Generation!")
    }

    fun template(path: String): Document =
        Jsoup.parse(File(Paths.get("").toAbsolutePath().toString(), "/src/main/resources/templates/$path"))

    fun startDocument(name: String): Document {
        val doc = Document(name)
        doc.head().append(template("head.html").head().html())

        doc.body()
            .addClass("package-index-page")

        return doc
    }

    fun generateIndex(): Document {
        val index = startDocument("index.html")
        index.append("<h1>Coming Soon!</h1>")

        return index
    }

}