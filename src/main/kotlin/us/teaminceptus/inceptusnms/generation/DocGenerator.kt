package us.teaminceptus.inceptusnms.generation

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.nodes.DocumentType
import org.jsoup.nodes.Element
import us.teaminceptus.inceptusnms.generation.Util.log
import java.io.File
import java.nio.file.Paths

object DocGenerator {

    private lateinit var input: File

    private const val TITLE = "Minecraft 1.20.2-R0.1-SNAPSHOT API"

    // Util

    fun generatePages(input: File, output: File) {
        DocGenerator.input = input
        File(output, "index.html").apply {
            createNewFile()
            writeText(generateIndex().outerHtml())
        }
        log("Created index.html")

        for (pkg in Util.getAllJavaPackages(input)) {
            File(output, "${pkg.replace('.', '/')}/package-summary.html").apply {
                parentFile.mkdirs()
                createNewFile()
                writeText(generatePackageSummary(pkg).outerHtml())
            }

            log("Created package-summary.html for $pkg")
        }

        log("Finished HTML Page Generation!")
    }

    fun template(path: String): Document =
        Jsoup.parse(File(Paths.get("").toAbsolutePath().toString(), "/src/main/resources/templates/$path"))

    fun document(path: String): String
        = File(input, path).readText()

    fun packageInfo(pkg: String): String
        = document("${pkg.replace('.', '/')}/package-info.html")

    fun Document.main(): Element
        = body().selectFirst("main")!!
    
    // Generation

    fun startDocument(name: String, navbar: String, title: String, textTitle: String): Document {
        val doc = Document(name).apply {
            outputSettings(OutputSettings().apply {
                prettyPrint(false)
            })
        }

        doc.prependChild(DocumentType("html", "", ""))
        doc.appendElement("html").apply {
            attr("lang", "en")
        }
        doc.head().apply {
            append(template("head.html").head().html())

            selectFirst("title")?.text("$title ($TITLE)")
        }

        val main = Element("main").apply {
            attr("role", "main")
        }

        doc.body()
            .addClass("package-index-page")
            .appendChild(Element("div").apply {
                addClass("flex-box")

                append(template("navbar/$navbar.html").html())
                appendChild(Element("div").apply {
                    addClass("flex-content")
                    appendChild(main)
                })
            })

        main.appendChild(Element("div").apply {
            addClass("header")
        }.appendChild(Element("h1").apply {
            addClass("title")
            attr("title", textTitle)
            text(textTitle)
        }))

        return doc
    }

    fun generateIndex(): Document {
        val index = startDocument("index.html", "default", "Overview", TITLE)
        val main = index.main()

        main.appendChild(Element("div").apply {
            addClass("block")
        }.append(document("overview.html")))

        val packages = Element("div").apply {
            id("all-packages-table")
        }
        main.appendChild(packages)

        packages.append("<div class=\"caption\"><span>Packages</span></div>\n")

        val list = Element("div").apply {
            classNames(setOf("summary-table", "two-column-summary"))
        }
        packages.appendChild(list)

        list.append("<div class=\"table-header col-first\">Package</div>\n")
        list.append("<div class=\"table-header col-last\">Description</div>\n")

        var even = true
        for (pkg in Util.getAllJavaPackages(input)) {
            list.appendChild(Element("div").apply {
                classNames(setOf("col-first", "${if (even) "even" else "odd"}-row-color", "all-packages-table", "all-packages-table-tab1"))
                append("<a href=\"/${pkg.replace('.', '/')}/package-summary.html\">$pkg</a>")
            })

            list.appendChild(Element("div").apply {
                classNames(setOf("col-last", "${if (even) "even" else "odd"}-row-color", "all-packages-table", "all-packages-table-tab1"))
                append("<div class=\"block\">\n${packageInfo(pkg)}\n</div>")
            })

            even = !even
        }

        return index
    }

    fun generatePackageSummary(pkg: String): Document {
        val summary = startDocument("package-summary.html", "package-summary", pkg, "Package $pkg")
        val main = summary.main()

        main.append("<hr>")
        main.append("<div class=\"package-signature\">package <span class=\"element-name\">$pkg</span></div>")

        main.appendChild(Element("section").apply {
            addClass("package-description")
            id("package-description")

            append("<div class=\"block\">${packageInfo(pkg)}</div>")
        })

        main.appendChild(Element("section").apply {
            addClass("summary")

            append("<ul class=\"summary-list\">")
                .appendChild(Element("li").apply {
                    val packages = Util.getAllJavaPackages(input).filter {
                        (it.contains(pkg) || it == pkg.substring(0, pkg.lastIndexOf('.'))) && it != pkg
                    }.take(6)

                    appendChild(Element("div").apply {
                        id("related-package-summary")
                        append("<div class=\"caption\"><span>Related Packages</span></div>\n")

                        appendChild(Element("div").apply {
                            classNames(setOf("summary-table", "two-column-summary"))

                            append("<div class=\"table-header col-first\">Package</div>\n")
                            append("<div class=\"table-header col-last\">Description</div>\n")

                            var even = true

                            for (related in packages) {
                                appendChild(Element("div").apply {
                                    classNames(setOf("col-first", "${if (even) "even" else "odd"}-row-color"))
                                    append("<a href=\"/${related.replace('.', '/')}/package-summary.html\">$related</a>")
                                })

                                appendChild(Element("div").apply {
                                    classNames(setOf("col-last", "${if (even) "even" else "odd"}-row-color"))
                                    append("<div class=\"block\">\n${packageInfo(related)}\n</div>")
                                })

                                even = !even
                            }
                        })
                    })
                })
                .appendChild(Element("li").apply {
                    appendChild(Element("div").apply {
                        id("class-summary")
                        // TODO Add Class Summary
                    })
                })
        })

        return summary
    }

}