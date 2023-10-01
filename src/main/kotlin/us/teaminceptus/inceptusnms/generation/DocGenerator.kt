package us.teaminceptus.inceptusnms.generation

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.nodes.DocumentType
import org.jsoup.nodes.Element
import us.teaminceptus.inceptusnms.generation.Util.log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
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

        for (clazz in Util.getClassDocumentation()) {
            File(output, "${clazz.name.replace('.', '/')}.html").apply {
                parentFile.mkdirs()
                createNewFile()
                writeText(generateClass(clazz).outerHtml())
            }

            log("Created Documentation for ${clazz.name}")
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

    fun startDocument(
        name: String,
        navbar: String,
        title: String,
        textTitle: String,
        description: String = ""
    ): Document {
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
            append("<meta name=\"description\" content=\"$description\">")
        }

        val main = Element("main").apply {
            attr("role", "main")
        }

        doc.body()
            .addClass("package-index-page")
            .append("""
                <script type="text/javascript">
                    var evenRowColor = "even-row-color";
                    var oddRowColor = "odd-row-color";
                    var tableTab = "table-tab";
                    var activeTableTab = "active-table-tab";
                    loadScripts(document, 'script');
                </script>
                <noscript>
                    <div>JavaScript is disabled on your browser.</div>
                </noscript>
            """.trimIndent())
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

    // Page Generators

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
        val summary = startDocument("package-summary.html", "package-summary", pkg, "Package $pkg",
            "declaration: package: $pkg")
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

    // Class Generators

    fun Iterable<String>.joinString(separator: CharSequence): String {
        if (toList().isEmpty())
            return ""

        if (toList().size == 1)
            return first() + " "

        return joinToString(separator) + " "
    }

    fun generateClass(info: ClassDocumentation): Document {
        val clazz = startDocument("${info.simpleName}.html", if (info.type == "record") "class" else info.type, info.simpleName, when (info.type) {
            "interface" -> "Interface ${info.simpleName}"
            "enum" -> "Enum Class ${info.simpleName}"
            "record" -> "Record Class ${info.simpleName}"
            "annotation" -> "Annotation Interface ${info.simpleName}"
            else -> "Class ${info.simpleName}"
        }, "declaration: package: ${info.pkg}; ${info.type} ${info.simpleName}")
        val main = clazz.main()

        // TODO Class Inheritance

        main.appendChild(Element("section").apply {
            addClass("class-signature")
            id("class-description")

            if (info.implements.isNotEmpty()) {
                appendChild(Element("dl").apply {
                    addClass("notes")
                    append("<dt>Implemented Interfaces:</dt>")
                    append("<dd><code>${info.implements.map { link(info.name, it) }.joinString(", ")}</code></dd>")
                })
            }

            append("<hr>")

            appendChild(Element("div").apply {
                addClass("type-signature")
                append("<span class=\"modifiers\">${info.visibility} ${info.mods.joinString(" ")}${info.type} </span>")
                append("<span class=\"element-name type-name-label\">${info.simpleName}</span>")

                if (info.implements.isNotEmpty() || info.extends != null) {
                    appendChild(Element("span").apply {
                        val builder = StringBuilder()
                        if (info.extends != null)
                            builder.append("\nextends ${info.extends}")

                        if (info.implements.isNotEmpty())
                            builder.append("\nimplements ${info.implements.map { link(info.name, it) }.joinString(", ")}")

                        append(builder.toString())
                    })
                }
            })

            append("<div class=\"block\">${info.comment}</div>")
        })

        main.appendChild(Element("section").apply {
            addClass("summary")

            val list = Element("ul").apply {
                addClass("summary-list")
            }

            val methods = generateMethodSummary(info)
            if (methods != null)
                list.appendChild(methods)

            appendChild(list)
        })

        return clazz
    }

    fun generateMethodSummary(info: ClassDocumentation): Element? {
        val methods = info.methods?.methods ?: return null
        val summary = Element("section").apply {
            addClass("method-summary")
            id("method-summary")
        }
        summary.append("<h2>Method Summary</h2>")

        fun methodSummaryButton(id: Int): Element = Element("button").apply {
            text(when (id) {
                0 -> "All Methods"; 1 -> "Static Methods"; 2 -> "Instance Methods"; 3 -> "Abstract Methods"; 4 -> "Concrete Methods"; 5 -> "Deprecated Methods"
                else -> throw IllegalArgumentException("Invalid ID: $id")
            })

            id("method-summary-table-tab$id")
            addClass(if (id == 0) "active-table-tab" else "table-tab")

            attr("role", "tab")
            attr("aria-selected", id == 0)
            attr("aria-controls", "method-summary-table.tabpanel")
            attr("tab-index", if (id == 0) "0" else "-1")
            attr("onkeydown", "switchTab(event)")
            attr("onclick", "show('method-summary-table', 'method-summary-table${if (id == 0) "" else id}', 3)")
        }

        val table = Element("div").apply {
            id("method-summary-table")
        }
        table.appendChild(Element("div").apply {
            addClass("table-tabs")
            attr("role", "tablist")
            attr("aria-orientation", "horizontal")

            appendChild(methodSummaryButton(0))

            if (methods.any { it.mods.contains("static") })
                appendChild(methodSummaryButton(1))

            if (methods.any { !it.mods.contains("static") })
                appendChild(methodSummaryButton(2))

            if (methods.any { it.mods.contains("abstract") })
                appendChild(methodSummaryButton(3))

            if (methods.any { !it.mods.contains("abstract") })
                appendChild(methodSummaryButton(4))

            if (methods.any { method -> method.annotations.any { it.type == "deprecated" } })
                appendChild(methodSummaryButton(5))
        })
        summary.appendChild(table)

        table.appendChild(Element("div").apply {
            id("method-summary-table.tabpanel")
            attr("role", "tabpanel")
        }.appendChild(Element("div").apply {
            classNames(setOf("summary-table three-column-summary"))
            attr("aria-labelledby", "method-summary-table-tab0")

            append("<div class=\"table-header col-first\">Modifier and Type</div>")
            append("<div class=\"table-header col-second\">Method</div>")
            append("<div class=\"table-header col-last\">Description</div>")

            var even = true

            for (method in methods) {
                val rowColor = "${if (even) "even" else "odd"}-row-color"
                val categories = mutableSetOf<Int>().apply {
                    if (method.mods.contains("static"))
                        add(1)

                    if (!method.mods.contains("static"))
                        add(2)

                    if (method.mods.contains("abstract"))
                        add(3)

                    if (!method.mods.contains("abstract"))
                        add(4)

                    if (method.annotations.any { it.type == "deprecated" })
                        add(5)
                }

                val paramString = if (method.parameters.isEmpty()) "()" else "(" + method.parameters.joinToString { it.name } + ")"

                val classes = setOf(rowColor, "method-summary-table") + categories.map { "method-summary-table-tab$it" }

                appendChild(Element("div").apply {
                    classNames(classes + "col-first")

                    appendChild(Element("code").apply {
                        val builder = StringBuilder()
                        if (method.visibility != "public")
                            builder.append("${method.visibility} ")

                        if (method.mods.isNotEmpty())
                            builder.append("${method.mods.joinString(" ")} ")

                        builder.append(link(info.name, method.returnType))

                        append(builder.toString())
                    })
                })

                appendChild(Element("div").apply {
                    classNames(classes + "col-second")
                    appendChild(Element("code").apply {
                        append("<a href=\"#${method.name}${paramString}\" class=\"member-name-link\">${method.name}</a>${
                            if (method.parameters.isEmpty()) "()" else "(" + method.parameters.joinToString { param -> "${link(info.name, param.type)} ${param.name}" } + ")"
                        }")
                    })
                })

                appendChild(Element("div").apply {
                    classNames(classes + "col-last")
                    append("<div class=\"block\">${method.comment}</div>")
                })

                even = !even
            }
        }))

        return summary
    }

    // Utility Methods

    private val REPOSITORIES = listOf(
        "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/",
        "https://hub.spigotmc.org/javadocs/spigot/"
    )

    fun link(self: String, clazz: String): String {
        var finalClass = clazz
        for (child in clazz.split("[<>,]".toRegex()).filter { it.isNotBlank() }) {
            val newClass = ClassDocumentation.processType(self, child)
            if (!newClass.contains(".")) continue

            val pkg = newClass.substring(0, newClass.lastIndexOf('.'))
            val simpleName = newClass.substring(newClass.lastIndexOf('.') + 1)
            val docUrl = newClass.replace('.', '/') + ".html"

            if (Util.getClassDocumentation().any { it.name == newClass })
                finalClass = finalClass.replace(child, "<a href=\"/${docUrl}\" title=\"member in $pkg\">${simpleName}</a>")

            for (repo in REPOSITORIES) {
                val url = URL(repo + docUrl)

                try {
                    with(url.openConnection() as HttpURLConnection) {
                        requestMethod = "GET"

                        if (responseCode == 200)
                            finalClass = finalClass.replace(child, "<a href=\"${repo + docUrl}\" title=\"member in $pkg\">${simpleName}</a>")
                    }
                } catch (ignored: UnknownHostException) { // Offline
                }
            }
        }

        return finalClass
    }

}