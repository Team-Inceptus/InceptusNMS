package us.teaminceptus.inceptusnms.generation

import kotlinx.coroutines.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.nodes.DocumentType
import org.jsoup.nodes.Element
import us.teaminceptus.inceptusnms.generation.Util.CRAFTBUKKIT_VERSION
import us.teaminceptus.inceptusnms.generation.Util.log
import java.io.File
import java.nio.file.Paths

private const val DEPRECATED_TYPE = "java.lang.Deprecated"

object DocGenerator {

    private lateinit var input: File

    private const val TITLE = "Minecraft 1.20.2-R0.1-SNAPSHOT API"

    // Util

    suspend fun generatePages(input: File, output: File) = withContext(Dispatchers.IO) {
        DocGenerator.input = input

        withContext(Dispatchers.IO) {
            File(output, "index.html").apply {
                createNewFile()
                writeText(generateIndex().outerHtml())
            }
            log("Created index.html")
        }

        withContext(Dispatchers.IO) {
            File(output, "index-all.html").apply {
                createNewFile()
                writeText(generateIndexAll().outerHtml())
            }
            log("Created index-all.html")
        }

        withContext(Dispatchers.IO) {
            File(output, "allpackages-index.html").apply {
                createNewFile()
                writeText(generateAllPackages().outerHtml())
            }
            log("Created allpackages-index.html")
        }

        withContext(Dispatchers.IO) {
            File(output, "allclasses-index.html").apply {
                createNewFile()
                writeText(generateAllClasses().outerHtml())
            }
            log("Created allclasses-index.html")
        }

        withContext(Dispatchers.IO) {
            File(output, "constant-values.html").apply {
                createNewFile()
                writeText(generateConstantValues().outerHtml())
            }
            log("Created constant-values.html")
        }

        for (pkg in Util.getAllJavaPackages(input))
            launch {
                File(output, "${pkg.url()}/package-summary.html").apply {
                    parentFile.mkdirs()
                    createNewFile()
                    writeText(generatePackageSummary(pkg).outerHtml())
                }

                log("Created package-summary.html for $pkg")
            }

        for (clazz in Util.getClassDocumentation())
            launch {
                File(output, "${clazz.name.url()}.html").apply {
                    parentFile.mkdirs()
                    createNewFile()

                    generateClass(clazz) {
                        writeText(it.outerHtml())
                    }
                }

                log("Created Documentation for ${clazz.name}")
            }
    }

    fun template(path: String): String =
       File(Paths.get("").toAbsolutePath().toString(), "/src/main/resources/templates/$path").readText()

    fun document(path: String): String
        = File(input, path).readText()

    fun packageInfo(pkg: String): String
        = document("/${pkg.url()}/package-info.html")

    fun Document.main(): Element
        = select("main").first()!!

    fun String.header(): String
        = substringBefore("<br>").run {
            if (contains(". "))
                substringBefore(". ") + "."
            else
                this
        }

    fun item(element: Element): Element = Element("li").appendChild(element)

    fun String.noGenerics() = this
        .substringBefore("<")
        .substringBefore("&lt;")

    fun String.url(): String
        = noGenerics().replace('.', '/').replace('$', '.').replace(CRAFTBUKKIT_VERSION, "{V}")

    fun String.serialize(): String
        = replace("<", "&lt;").replace(">", "&gt;")

    fun String.deserialize(): String
        = replace("&lt;", "<").replace("&gt;", ">")

    fun String.noArray(): String
        = replace("[\\[\\]]".toRegex(), "")

    fun classSummaryButton(id: Int, text: String, prefix: String = "class-summary"): Element =
        Element("button").apply {
            id("$prefix-tab$id")
            addClass(if (id == 0) "active-table-tab" else "table-tab")

            attr("role", "tab")
            attr("aria-selected", (id == 0).toString())
            attr("aria-controls", "class-summary.tabpanel")
            attr("tabindex", if (id == 0) "0" else "-1")
            attr("onkeydown", "switchTab(event)")
            attr("onclick", "show('$prefix', '$prefix${if (id == 0) "" else "-tab$id"}', 2)")

            text(text)
        }
    
    // Generation

    fun startDocument(
        name: String,
        navbar: String,
        title: String,
        textTitle: String? = null,
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
            append(template("head.html"))

            selectFirst("title")?.text("$title ($TITLE)")
            append("<meta name=\"description\" content=\"$description\">")
        }

        val main = Element("main").apply {
            attr("role", "main")
        }

        doc.body()
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

                append(template("navbar/$navbar.html"))
                appendChild(Element("div").apply {
                    addClass("flex-content")
                    appendChild(main)
                })
            })

        main.appendChild(Element("div").apply {
            addClass("header")
            id("inceptusnms:header")
        })

        if (textTitle != null)
            main.getElementById("inceptusnms:header")!!.appendChild(Element("h1").apply {
                addClass("title")
                append(textTitle)
            })

        return doc
    }

    // Page Generators

    fun generateIndex(): Document {
        val index = startDocument("index.html", "default", "Overview", TITLE, "overview")
        index.body().addClass("package-index-page")

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
                append("<a href=\"/${pkg.url()}/package-summary.html\">$pkg</a>")
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
        summary.body().addClass("package-index-page")

        val main = summary.main()

        val classes = Util.getClassDocumentation().filter { it.pkg == pkg }.sortedBy { it.name }

        main.append("<hr>")
        main.append("<div class=\"package-signature\">package <span class=\"element-name\">$pkg</span></div>")

        main.appendChild(Element("section").apply {
            addClass("package-description")
            id("package-description")

            append("<div class=\"block\">${packageInfo(pkg)}</div>")
        })

        main.appendChild(Element("section").apply {
            addClass("summary")

            val packages = Util.getAllJavaPackages(input).filter {
                (it.contains(pkg) || it == pkg.substring(0, pkg.lastIndexOf('.'))) && it != pkg
            }.take(6)

            val list = append("<ul class=\"summary-list\">")

            if (packages.isNotEmpty())
                list.appendChild(Element("li").apply {
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
                                    append("<a href=\"/${related.url()}/package-summary.html\">$related</a>")
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

            list.appendChild(Element("li").apply {
                appendChild(Element("div").apply {
                    id("class-summary")

                    appendChild(Element("div").apply {
                        addClass("table-tabs")
                        attr("role", "tablist")
                        attr("aria-orientation", "horizontal")

                        when {
                            classes.all { it.type == "class" } -> {
                                append("<div class=\"caption\"><span>Classes</span></div>")
                            }
                            classes.all { it.type == "interface" } -> {
                                append("<div class=\"caption\"><span>Interfaces</span></div>")
                            }
                            classes.all { it.type == "enum" } -> {
                                append("<div class=\"caption\"><span>Enum Classes</span></div>")
                            }
                            classes.all { it.type == "record" } -> {
                                append("<div class=\"caption\"><span>Record Classes</span></div>")
                            }
                            classes.all { it.type == "annotation" } -> {
                                append("<div class=\"caption\"><span>Annotation Interfaces</span></div>")
                            }
                            else -> {
                                appendChild(classSummaryButton(0, "All Classes and Interfaces"))

                                if (classes.any { it.type == "interface" })
                                    appendChild(classSummaryButton(1, "Interfaces"))

                                if (classes.any { it.type == "class" })
                                    appendChild(classSummaryButton(2, "Classes"))

                                if (classes.any { it.type == "enum" })
                                    appendChild(classSummaryButton(3, "Enum Classes"))

                                if (classes.any { it.type == "record" })
                                    appendChild(classSummaryButton(4, "Record Classes"))

                                if (classes.any { it.type == "annotation" })
                                    appendChild(classSummaryButton(7, "Annotation Interfaces"))
                            }
                        }

                        appendChild(Element("div").apply {
                            classNames(setOf("summary-table", "two-column-summary"))

                            append("<div class=\"table-header col-first\">Class</div>")
                            append("<div class=\"table-header col-last\">Description</div>")

                            var even = true
                            for (clazz in classes) {
                                val rowColor = "${if (even) "even" else "odd"}-row-color"
                                val types = mapOf<Int, (ClassDocumentation) -> Boolean>(
                                    1 to { it.type == "interface" },
                                    2 to { it.type == "class" },
                                    3 to { it.type == "enum" },
                                    4 to { it.type == "record" },
                                    7 to { it.type == "annotation" }
                                ).filterValues { it(clazz) }.keys.map { "class-summary-tab$it" }

                                appendChild(Element("div").apply {
                                    classNames(setOf("col-first", rowColor, "class-summary") + types)

                                    val builder = StringBuilder()
                                    builder.append("<a href=\"${clazz.simpleName}.html\" title=\"${clazz.type} in $pkg\">${clazz.simpleName}</a>")

                                    if (clazz.generics.isNotEmpty())
                                        builder.append(generics(clazz))

                                    append(builder.toString())
                                })

                                appendChild(Element("div").apply {
                                    classNames(setOf("col-last", rowColor, "class-summary") + types)
                                    if (clazz.deprecated.isNotEmpty()) {
                                        appendChild(Element("div").apply {
                                            addClass("block")

                                            append("<span class=\"deprecated-label\">Deprecated.</span>")
                                            append("<div class=\"deprecation-comment\">${clazz.deprecated}</div>")
                                        })
                                    } else
                                        append("<div class=\"block\">${clazz.comment}</div>")
                                })

                                even = !even
                            }
                        })
                    })
                })
            })
        })

        return summary
    }

    fun generateAllClasses(): Document {
        val doc = startDocument("allclasses-index.html", "default", "All Classes", "All Classes and Interfaces", description = "class index")
        val classes = Util.getClassDocumentation().sortedBy { it.simpleName }
        val main = doc.main().appendChild(Element("div").apply { id("all-classes-table") })

        main.appendChild(Element("div").apply {
            addClass("table-tabs")
            attr("role", "tablist")
            attr("aria-orientation", "horizontal")

            appendChild(classSummaryButton(0, "All Classes and Interfaces", "all-classes-table"))

            if (classes.any { it.type == "interface" })
                appendChild(classSummaryButton(1, "Interfaces", "all-classes-table"))

            if (classes.any { it.type == "class" })
                appendChild(classSummaryButton(2, "Classes", "all-classes-table"))

            if (classes.any { it.type == "enum" })
                appendChild(classSummaryButton(3, "Enum Classes", "all-classes-table"))

            if (classes.any { it.type == "record" })
                appendChild(classSummaryButton(4, "Record Classes", "all-classes-table"))

            if (classes.any { it.type == "annotation" })
                appendChild(classSummaryButton(7, "Annotation Interfaces", "all-classes-table"))
        })

        main.appendChild(Element("div").apply {
            id("all-classes-table.tabpanel")
            attr("role", "tabpanel")

            appendChild(Element("div").apply {
                classNames(setOf("summary-table", "two-column-summary"))
                attr("aria-labelledby", "all-classes-tabe-tab0")

                append("<div class=\"table-header col-first\">Class</div>")
                append("<div class=\"table-header col-last\">Description</div>")

                var even = true
                val types = mapOf<Int, (ClassDocumentation) -> Boolean>(
                    1 to { it.type == "interface" },
                    2 to { it.type == "class" },
                    3 to { it.type == "enum" },
                    4 to { it.type == "record" },
                    7 to { it.type == "annotation" }
                )
                for (clazz in classes) {
                    val rowColor = "${if (even) "even" else "odd"}-row-color"
                    val clazzes = types.filterValues { it(clazz) }.keys.map { "all-classes-table-tab$it" }

                    appendChild(Element("div").apply {
                        classNames(setOf("col-first", rowColor, "all-classes-table") + clazzes)
                        append("<a href=\"/${clazz.pkg.url()}/${clazz.simpleName}.html\" title=\"${clazz.type} in ${clazz.pkg}\">${clazz.simpleName}</a>")
                    })

                    appendChild(Element("div").apply {
                        if (clazz.deprecated.isNotEmpty()) {
                            appendChild(Element("div").apply {
                                addClass("block")

                                append("<span class=\"deprecated-label\">Deprecated.</span>")
                                append("<div class=\"deprecation-comment\">${clazz.deprecated}</div>")
                            })
                        } else
                            append("<div class=\"block\">${clazz.comment}</div>")
                    })

                    even = !even
                }
            })
        })

        return doc
    }

    fun generateAllPackages(): Document {
        val doc = startDocument("allpackages-index.html", "default", "All Packages", "All Packages", description = "package index")
        val packages = Util.getAllJavaPackages(input)
        val main = doc.main()

        main.append("<div class=\"caption\"><span>Package Summary</span></div>")
        main.appendChild(Element("div").apply {
            classNames(setOf("summary-table", "two-column-summary"))
            append("<div class=\"table-header col-first\">Package</div>")
            append("<div class=\"table-header col-last\">Description</div>")

            var even = true
            for (pkg in packages) {
                val rowColor = "${if (even) "even" else "odd"}-row-color"

                appendChild(Element("div").apply {
                    classNames(setOf("col-first", rowColor))
                    append("<a href=\"/${pkg.url()}/package-summary.html\">$pkg</a>")
                })

                appendChild(Element("div").apply {
                    classNames(setOf("col-last", rowColor))
                    append("<div class=\"block\">${packageInfo(pkg)}</div>")
                })

                even = !even
            }
        })

        return doc
    }

    fun generateConstantValues(): Document {
        val doc = startDocument("constant-values.html", "default", "Constant Field Values", "Constant Field Values", description = "summary of constants")
        val main = doc.main()

        val classes = Util.getClassDocumentation().filter { clazz ->
            clazz.fields?.fields?.any { it.value != null } == true
        }.sortedBy { it.name }
        val packages = classes.map { it.pkg }.toSet().sorted()

        doc.getElementById("inceptusnms:header")!!.appendChild(Element("section").apply {
            addClass("packages")

            append("<h2 title=\"Contents\">Contents</h2>")

            appendChild(Element("ul").apply {
                for (pkg in packages)
                    appendChild(item(Element("a").apply {
                        attr("href", "#$pkg")
                        text("$pkg.*")
                    }))
            })
        })


        for (pkg in packages) {
            main.appendChild(Element("section").apply {
                addClass("constants-summary")
                id(pkg)

                append("<h2 title=\"$pkg\">$pkg.*</h2>")
                appendChild(Element("ul").apply {
                    addClass("block-list")

                    for (clazz in classes.filter { it.pkg == pkg }) {
                        val generics = clazz.generics.map { it.name }

                        appendChild(Element("li").apply {
                            append("<div class=\"caption\"><span>${clazz.pkg}.${link(clazz.fullName, clazz.docName, generics)}</span></div>")

                            appendChild(Element("div").apply {
                                classNames(setOf("summary-table", "three-column-summary"))

                                append("<div class=\"table-header col-first\">Modifier and Type</div>")
                                append("<div class=\"table-header col-second\">Constant Field</div>")
                                append("<div class=\"table-header col-third\">Value</div>")

                                var even = true
                                for (field in clazz.fields!!.fields.filter { it.value != null }) {
                                    val rowColor = "${if (even) "even" else "odd"}-row-color"

                                    appendChild(Element("div").apply {
                                        classNames(setOf("col-first", rowColor))
                                        append("<code id=\"${clazz.name}.${field.name}\">${field.visibility} ${field.mods.joinString(" ")} ${link(clazz.fullName, field.type, generics)}</code>")
                                    })

                                    appendChild(Element("div").apply {
                                        classNames(setOf("col-second", rowColor))
                                        append("<code><a href=\"/${clazz.name.url()}.html#${field.name}\">${field.name}</a></code>")
                                    })

                                    appendChild(Element("div").apply {
                                        classNames(setOf("col-third", rowColor))

                                        val value = when (field.type) {
                                            "float" -> "${field.value}f"
                                            "long" -> "${field.value}L"
                                            "double" -> "${field.value}d"
                                            "char" -> "'${field.value}'"
                                            "java.lang.String",
                                            "java.util.regex.Pattern",
                                            "java.time.format.DateTimeFormatter" -> "\"${field.value}\""
                                            "byte" -> "0x${field.value!!.toByte().toString(16)}"
                                            else -> field.value
                                        }

                                        append("<code>$value</code>")
                                    })

                                    even = !even
                                }
                            })
                        })
                    }

                })


            })
        }

        return doc
    }

    private val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun generateIndexAll(): Document {
        val index = startDocument("index-all.html", "index-all", "Index", "Index", description = "index")
        val main = index.main()
        val classes = Util.getClassDocumentation()
        val characters = alphabet.filter { char ->
            classes.any { clazz ->
                clazz.simpleName.substringAfterLast(".").startsWith(char, true) ||
                clazz.enumerations?.enums?.any { it.name.startsWith(char, true) } == true ||
                clazz.fields?.fields?.any { it.name.startsWith(char, true) } == true
                clazz.methods?.methods?.any { it.name.startsWith(char, true) } == true
            }
        }

        for (char in characters)
            main.append("<a href=\"#I:$char\">$char</a>&nbsp;")

        for (char in characters) {
            val content =
                (classes.filter { it.simpleName.substringAfterLast(".").startsWith(char, true) }.map { clazz ->
                    val title = when (clazz.type) {
                        "interface" -> "Interface in <a href=\"/${clazz.pkg.url()}/package-summary.html\">${clazz.pkg}</a>"
                        "enum" -> "Enum Class in <a href=\"/${clazz.pkg.url()}/package-summary.html\">${clazz.pkg}</a>"
                        "record" -> "Record Class in <a href=\"/${clazz.pkg.url()}/package-summary.html\">${clazz.pkg}</a>"
                        "annotation" -> "Annotation Interface in <a href=\"/${clazz.pkg.url()}/package-summary.html\">${clazz.pkg}</a>"
                        else -> "Class in <a href=\"/${clazz.pkg.url()}/package-summary.html\">${clazz.pkg}</a>"
                    }

                    Quadruple(
                        clazz.simpleName.substringAfterLast("."),
                        clazz.name,
                        "<a href=\"/${clazz.name.url()}.html\" class=\"type-name-link\" title=\"${clazz.type} in ${clazz.pkg}\">${clazz.simpleName}</a> - $title",
                        clazz.comment.header()
                    )
                } + classes.filter { clazz -> clazz.enumerations?.enums?.any { it.name.startsWith(char, true) } == true }.map { clazz ->
                    clazz.enumerations!!.enums.filter { it.name.startsWith(char, true) }.map { enum ->
                        Quadruple(
                            enum.name,
                            clazz.name,
                            "<a href=\"/${clazz.name.url()}.html#${enum.name}\" class=\"member-name-link\">${enum.name}</a> - Enum Constant in ${clazz.type} ${clazz.pkg}.<a href=\"/${clazz.name.url()}\" title=\"${clazz.type} in ${clazz.pkg}\">${clazz.simpleName}</a>",
                            enum.comment.header()
                        )
                    }
                }.flatten() + classes.filter { clazz -> clazz.fields?.fields?.any { it.name.startsWith(char, true) } == true }.map { clazz ->
                    clazz.fields!!.fields.filter { it.name.startsWith(char, true) }.map { field ->
                        Quadruple(
                            field.name,
                            clazz.name,
                            "<a href=\"/${clazz.name.url()}.html#${field.name}\" class=\"member-name-link\" title=\"Field in ${clazz.name}\">${field.name}</a> - Field in ${clazz.type} ${clazz.pkg}.<a href=\"/${clazz.name.url()}\" title=\"${clazz.type} in ${clazz.pkg}\">${clazz.simpleName}</a>",
                            field.comment.header()
                        )
                    }
                }.flatten() + classes.filter { clazz -> clazz.methods?.methods?.any { it.name.startsWith(char, true) } == true }.map { clazz ->
                    clazz.methods!!.methods.filter { it.name.startsWith(char, true) }.map { method ->
                        Quadruple(
                            method.fullName,
                            clazz.name,
                            "<a href=\"/${clazz.name.url()}.html#${method.fullName}\" class=\"member-name-link\">${method.cleanName}</a> - Method in ${clazz.type} ${clazz.pkg}.<a href=\"/${clazz.name.url()}\" title=\"${clazz.type} in ${clazz.pkg}\">${clazz.simpleName}</a>",
                            method.comment.header()
                        )
                    }
                }.flatten()).sortedWith(compareBy({ it.first }, { it.second }))

            main.append("<h2 class=\"title\" id=\"I:$char\">$char</h2>")
            main.appendChild(Element("dl").apply {
                addClass("index")

                for ((_, _, html, comment) in content) {
                    append("<dt>$html</dt>")
                    append("<dd><div class=\"block\">$comment</div></dd>")
                }
            })
        }

        return index
    }

    // Class Generators

    fun Iterable<String>.joinString(separator: CharSequence, suffix: String = " "): String {
        if (toList().isEmpty())
            return ""

        if (toList().size == 1)
            return first() + suffix

        return joinToString(separator) + suffix
    }

    suspend fun generateClass(info: ClassDocumentation, callback: (Document) -> Unit) = coroutineScope {
        val clazz = startDocument("${info.simpleName}.html", if (info.type == "record") "class" else info.type, info.simpleName, when (info.type) {
            "interface" -> "Interface ${info.docName}"
            "enum" -> "Enum Class ${info.docName}"
            "record" -> "Record Class ${info.docName}"
            "annotation" -> "Annotation Interface ${info.docName}"
            else -> "Class ${info.docName}"
        }, "declaration: package: ${info.pkg}; ${info.type} ${info.simpleName}")
        clazz.body().addClass("class-declaration-page")

        val main = clazz.main()

        main.getElementById("inceptusnms:header")!!.apply {
            prependChild(Element("div").apply {
                addClass("sub-title")

                append("<span class=\"package-label-in-type\">Package</span>&nbsp;")
                append("<a href=\"package-summary.html\">${info.pkg}</a>")
            })

            if (info.unfinished)
                append("<p style=\"color: red\">This class's documentation is unfinished and should not be used in production.</p>")
        }

        if (info.type != "annotation" && info.type != "interface") {
            val tree = Util.getHierarchyTree(info)

            var current = main
            var first = true
            for (node in tree) {
                val element = current.appendElement("div").apply {
                    addClass("inheritance")
                    if (first) {
                        attr("title", "Inheritance Tree")
                        first = false
                    }

                    append(if (node == info.fullDocName || node == info.fullName) node.replace('$', '.') else link(info.name, node.deserialize(), info.generics.map { it.name }, fullName = true))
                }
                current = element
            }
        }

        main.appendChild(Element("section").apply {
            addClass("class-description")
            id("class-description")

            if (info.generics.isNotEmpty())
                appendChild(Element("dl").apply {
                    addClass("notes")
                    append("<dt>Type Parameters:</dt>")
                    for (generic in info.generics)
                        append("<dd><code>${generic.name}</code> - ${generic.comment.header()}</dd>")
                })

            val implements = Util.getImplements(info).sorted()
            if (implements.isNotEmpty() && info.type != "interface") {
                appendChild(Element("dl").apply {
                    addClass("notes")
                    append("<dt>All Implemented Interfaces:</dt>")
                    append("<dd><code>${implements.map { clazz -> link(info.fullName, clazz, info.generics.map { it.name }) }.joinString(", ", "")}</code></dd>")
                })
            }

            if (info.type == "interface") {
                val implementing = Util.getClassDocumentation().filter { clazz -> clazz.implements.any { it.contains(info.name) } }.sortedBy { it.name }
                if (implementing.isNotEmpty()) {
                    appendChild(Element("dl").apply {
                        addClass("notes")
                        append("<dt>All Known Implementing Classes:</dt>")
                        append("<dd>${implementing.map { clazz -> "<code>${link(info.fullName, clazz.name, info.generics.map { it.name })}</code>" }.joinString(",&nbsp;", "")}</dd>")
                    })
                }
            } else {
                val children = Util.getSubclasses(info).sortedBy { it.name }
                if (children.isNotEmpty()) {
                    appendChild(Element("dl").apply {
                        addClass("notes")
                        append("<dt>Direct Known Subclasses:</dt>")
                        append("<dd>${children.map { child -> "<code>${link(info.fullName, child.name, info.generics.map { it.name })}</code>" }.joinString(",&nbsp;", "")}</dd>")
                    })
                }
            }

            if (info.enclosing != null) {
                appendChild(Element("dl").apply {
                    addClass("notes")
                    append("<dt>Enclosing Class:</dt>")
                    append("<dd>${link(info.fullName, info.enclosing, info.generics.map { it.name })}</dd>")
                })
            }

            append("<hr>")

            appendChild(Element("div").apply {
                addClass("type-signature")

                val annotations = info.annotations.map { annotation ->
                    val base = link(info.name, annotation.type, info.generics.map { it.name }, true)
                    if (annotation.parameters.isEmpty()) base
                    else "$base(${annotation.parameters.joinToString(", ") { param -> "${param.name} = ${param.value}" }})"
                }
                if (annotations.isNotEmpty())
                    append("<span class=\"annotations\">${annotations.joinString(" ", "")}</span><br>")

                val constructors = info.constructors?.constructors
                append("<span class=\"modifiers\">${info.visibility} ${info.mods.joinString(" ")}${info.type} </span>")

                val clazzBuilder = StringBuilder()
                clazzBuilder.append("<span class=\"element-name type-name-label\">${info.docName}")

                clazzBuilder.append("</span>")
                if (info.type == "record" && !constructors.isNullOrEmpty())
                    clazzBuilder.append("(${constructors.maxBy { if (it.primary) Int.MAX_VALUE else it.parameters.size }.parameters.joinToString { param -> "${link(info.name, param.type, info.generics.map { it.name })}&nbsp;${param.name}" }})")

                append("$clazzBuilder&nbsp;")

                if (info.implements.isNotEmpty() || info.extends != null) {
                    appendChild(Element("span").apply {
                        val builder = StringBuilder()
                        if (info.extends != null)
                            builder.append("\nextends ${link(info.fullName, info.extends)}")

                        if (info.implements.isNotEmpty())
                            builder.append("\n${if (info.type == "interface") "extends" else "implements"} ${info.implements.map { clazz -> link(info.fullName, clazz, info.generics.map { it.name }) }.joinString(", ")}")

                        append(builder.toString())
                    })
                }
            })

            if (info.annotations.any { it.type == DEPRECATED_TYPE }) {
                appendChild(Element("div").apply {
                    addClass("deprecation-block")

                    append("<span class=\"deprecated-label\">Deprecated.</span>")
                    if (info.deprecated.isNotEmpty())
                        append("<div class=\"deprecation-comment\">${info.deprecated}</div>")
                })
            }

            append("<div class=\"block\">${info.comment}</div>")
        })

        main.appendChild(Element("section").apply {
            addClass("summary")

            val list = Element("ul").apply {
                addClass("summary-list")
            }

            val elements = listOf(
                async { generateNestedSummary(info) },
                async { generateEnumSummary(info) },
                async { generateFieldSummary(info) },
                async { generateConstructorSummary(info) },
                async { generateMethodSummary(info) }
            ).awaitAll().filterNotNull()

            for (element in elements)
                list.appendChild(item(element))

            appendChild(list)
        })

        main.appendChild(Element("section").apply {
            addClass("details")

            val list = Element("ul").apply {
                addClass("details-list")
            }

            val elements = listOf(
                async { generateEnumDetail(info) },
                async { generateFieldDetail(info) },
                async { generateConstructorDetail(info) },
                async { generateMethodDetail(info) }
            ).awaitAll().filterNotNull()

            for (element in elements)
                list.appendChild(item(element))

            appendChild(list)
        })

        callback(clazz)
    }

    // Class Generators - Summary

    fun generateNestedSummary(info: ClassDocumentation): Element? {
        val nestedClasses = Util.getClassDocumentation().filter { it.enclosing == info.name }.sortedBy { it.name }
        if (nestedClasses.isEmpty()) return null

        val summary = Element("section").apply {
            addClass("nested-class-summary")
            id("nested-class-summary")
        }

        summary.append("<h2>Nested Class Summary</h2>")
        summary.append("<div class=\"caption\"><span>Nested Classes</span></div>")
        summary.appendChild(Element("div").apply {
            classNames(setOf("summary-table three-column-summary"))

            append("<div class=\"table-header col-first\">Modifier and Type</div>")
            append("<div class=\"table-header col-second\">Class</div>")
            append("<div class=\"table-header col-last\">Description</div>")

            var even = true
            for (clazz in nestedClasses) {
                val rowColor = "${if (even) "even" else "odd"}-row-color"

                appendChild(Element("div").apply {
                    classNames(setOf("col-first", rowColor))

                    val builder = StringBuilder()
                    if (clazz.visibility != "public")
                        builder.append("${clazz.visibility} ")

                    if (clazz.mods.isNotEmpty())
                        builder.append("${clazz.mods.joinString(" ")} ")

                    builder.append(clazz.type)
                    append("<code>$builder</code>")
                })

                appendChild(Element("div").apply {
                    classNames(setOf("col-second", rowColor))

                    append("<code>${clazz.fullDocName}</code>")
                })

                appendChild(Element("div").apply {
                    classNames(setOf("col-last", rowColor))
                    if (clazz.annotations.any { it.type == DEPRECATED_TYPE }) {
                        appendChild(Element("div").apply {
                            addClass("block")

                            append("<span class=\"deprecated-label\">Deprecated.</span>")
                            if (clazz.deprecated.isNotEmpty())
                                append("<div class=\"deprecation-comment\">${clazz.deprecated.header()}</div>")
                        })
                    } else
                        append("<div class=\"block\">${clazz.comment.header()}</div>")
                })

                even = !even
            }
        })

        return summary
    }

    fun generateEnumSummary(info: ClassDocumentation): Element? {
        if (info.type != "enum") return null

        val enums = info.enumerations?.enums?.sortedBy { it.name } ?: return null
        val summary = Element("section").apply {
            addClass("constants-summary")
            id("enum-constant-summary")
        }

        summary.append("<h2>Enum Constant Summary</h2>")
        summary.append("<div class=\"caption\"><span>Enum Constants</span></div>")
        summary.appendChild(Element("div").apply {
            classNames(setOf("summary-table two-column-summary"))

            append("<div class=\"table-header col-first\">Enum Constant</div>")
            append("<div class=\"table-header col-last\">Description</div>")

            var even = true
            for (enum in enums) {
                val rowColor = "${if (even) "even" else "odd"}-row-color"

                appendChild(Element("div").apply {
                    classNames(setOf("col-first", rowColor))
                    append("<code><a href=\"#${enum.name}\" class=\"member-name-link\">${enum.name}</a></code>")
                })

                appendChild(Element("div").apply {
                    classNames(setOf("col-last", rowColor))
                    if (enum.annotations.any { it.type == DEPRECATED_TYPE } || info.deprecated.isNotEmpty()) {
                        appendChild(Element("div").apply {
                            addClass("block")

                            append("<span class=\"deprecated-label\">Deprecated.</span>")
                            if (enum.deprecated.isNotEmpty())
                                append("<div class=\"deprecation-comment\">${enum.deprecated.header()}</div>")
                        })
                    } else
                        append("<div class=\"block\">${enum.comment.header()}</div>")
                })
                even = !even
            }
        })

        return summary
    }

    fun generateFieldSummary(info: ClassDocumentation): Element? {
        val fields = info.fields?.fields?.sortedWith(compareBy(
            {
                when {
                    it.mods.contains("static") && it.mods.contains("final") -> 0
                    it.mods.contains("static") -> 1
                    it.mods.contains("final") -> 2
                    else -> 3
                }
            },
            { it.name }
        )) ?: return null
        val summary = Element("section").apply {
            addClass("field-summary")
            id("field-summary")
        }
        summary.append("<h2>Field Summary</h2>")
        summary.append("<div class=\"caption\"><span>Fields</span></div>")

        summary.appendChild(Element("div").apply {
            classNames(setOf("summary-table three-column-summary"))

            append("<div class=\"table-header col-first\">Modifier and Type</div>")
            append("<div class=\"table-header col-second\">Field</div>")
            append("<div class=\"table-header col-last\">Description</div>")

            var even = true
            for (field in fields) {
                val rowColor = "${if (even) "even" else "odd"}-row-color"

                appendChild(Element("div").apply {
                    classNames(setOf("col-first", rowColor))

                    appendChild(Element("code").apply {
                        val builder = StringBuilder()
                        if (field.visibility != "public")
                            builder.append("${field.visibility} ")

                        if (field.mods.isNotEmpty())
                            builder.append("${field.mods.joinString(" ")} ")

                        builder.append(link(info.fullName, field.type, info.generics.map { it.name }))

                        append(builder.toString())
                    })
                })

                appendChild(Element("div").apply {
                    classNames(setOf("col-second", rowColor))

                    appendChild(Element("code").apply {
                        append("<a href=\"#${field.name}\" class=\"member-name-link\">${field.name}</a>")
                    })
                })

                appendChild(Element("div").apply {
                    classNames(setOf("col-last", rowColor))
                    if (field.annotations.any { it.type == DEPRECATED_TYPE } || info.deprecated.isNotEmpty()) {
                        appendChild(Element("div").apply {
                            addClass("block")

                            append("<span class=\"deprecated-label\">Deprecated.</span>")
                            if (field.deprecated.isNotEmpty())
                                append("<div class=\"deprecation-comment\">${field.deprecated.header()}</div>")
                        })
                    } else
                        append("<div class=\"block\">${field.comment.header()}</div>")
                })

                even = !even
            }
        })

        return summary
    }

    fun generateConstructorSummary(info: ClassDocumentation): Element? {
        val constructors = info.constructors?.constructors?.sortedWith(compareBy(
            { !it.primary },
            { it.parameters.size },
            { constr -> constr.parameters.joinToString(",") { it.type } }
        )) ?: return null
        val summary = Element("section").apply {
            addClass("constructor-summary")
            id("constructor-summary")
        }
        summary.append("<h2>Constructor Summary</h2>")
        summary.append("<div class=\"caption\"><span>Constructors</span></div>")

        val anyVisibility = constructors.any { it.visibility != "public" }
        summary.appendChild(Element("div").apply {
            classNames(setOf("summary-table ${if (anyVisibility) "three" else "two"}-column-summary"))

            if (anyVisibility)
                append("<div class=\"table-header col-first\">Modifier</div>")

            append("<div class=\"table-header col-${if (anyVisibility) "second" else "first"}\">Constructor</div>")
            append("<div class=\"table-header col-last\">Description</div>")

            var even = true
            for (constructor in constructors) {
                val rowColor = "${if (even) "even" else "odd"}-row-color"

                if (anyVisibility)
                    appendChild(Element("div").apply {
                        classNames(setOf("col-first", rowColor))

                        append("<code>${constructor.visibility}</code>")
                    })

                appendChild(Element("div").apply {
                    classNames(setOf("col-constructor-name", rowColor))

                    appendChild(Element("code").apply {
                        val builder = StringBuilder()

                        builder.append("<a href=\"#%3Cinit%3E(${constructor.parameters.map { it.type }.joinString(",", "")})\" class=\"member-name-link\">${info.simpleName}</a><wbr>")
                        if (constructor.parameters.isNotEmpty())
                            builder.append("(${constructor.parameters.joinToString { param -> "${link(info.fullName, param.type, info.generics.map { it.name })}&nbsp;${param.name}" }})")
                        else
                            builder.append("()")

                        append(builder.toString())
                    })
                })

                appendChild(Element("div").apply {
                    classNames(setOf("col-last", rowColor))
                    if (constructor.annotations.any { it.type == DEPRECATED_TYPE } || info.deprecated.isNotEmpty()) {
                        appendChild(Element("div").apply {
                            addClass("block")

                            append("<span class=\"deprecated-label\">Deprecated.</span>")
                            if (constructor.deprecated.isNotEmpty())
                                append("<div class=\"deprecation-comment\">${constructor.deprecated.header()}</div>")
                        })
                    } else
                        append("<div class=\"block\">${constructor.comment.header()}</div>")
                })

                even = !even
            }
        })

        return summary
    }

    fun generateMethodSummary(info: ClassDocumentation): Element? {
        val methods = info.methods?.methods?.sortedWith(compareBy(
            { it.name },
            { it.parameters.size },
            { method -> method.parameters.joinToString(",") { it.type } }
        ))
        val inheritedMethods = Util.getInheritedMethods(info)

        if (methods.isNullOrEmpty() && inheritedMethods.isEmpty()) return null

        val summary = Element("section").apply {
            addClass("method-summary")
            id("method-summary")
        }

        if (methods != null) {
            summary.append("<h2>Method Summary</h2>")

            fun methodSummaryButton(id: Int): Element = Element("button").apply {
                text(
                    when (id) {
                        0 -> "All Methods"
                        1 -> "Static Methods"
                        2 -> "Instance Methods"
                        3 -> "Abstract Methods"
                        4 -> "Concrete Methods"
                        5 -> "Default Methods"
                        6 -> "Deprecated Methods"
                        7 -> "Public Methods"
                        else -> throw IllegalArgumentException("Invalid ID: $id")
                    }
                )

                id("method-summary-table-tab$id")
                addClass(if (id == 0) "active-table-tab" else "table-tab")

                attr("role", "tab")
                attr("aria-selected", (id == 0).toString())
                attr("aria-controls", "method-summary-table.tabpanel")
                attr("tab-index", if (id == 0) "0" else "-1")
                attr("onkeydown", "switchTab(event)")
                attr("onclick", "show('method-summary-table', 'method-summary-table${if (id == 0) "" else "-tab$id"}', 3)")
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

                if (methods.any { !it.mods.contains("abstract") && info.type != "interface" })
                    appendChild(methodSummaryButton(4))

                if (methods.any { it.mods.contains("default") })
                    appendChild(methodSummaryButton(5))

                if (methods.any { method -> method.annotations.any { it.type == DEPRECATED_TYPE } })
                    appendChild(methodSummaryButton(6))

                if (methods.any { it.visibility == "public" } && info.visibility == "public")
                    appendChild(methodSummaryButton(7))
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

                        if (method.visibility == "public" && info.visibility == "public")
                            add(7)

                        if (method.mods.contains("default"))
                            add(5)

                        if (method.annotations.any { it.type == DEPRECATED_TYPE })
                            add(6)
                    }

                    val classes = setOf(rowColor, "method-summary-table") + categories.map { "method-summary-table-tab$it" }
                    val generics = if (method.mods.contains("static")) emptyList() else info.generics.map { it.name }

                    appendChild(Element("div").apply {
                        classNames(classes + "col-first")

                        appendChild(Element("code").apply {
                            val builder = StringBuilder()
                            if (method.visibility != "public")
                                builder.append("${method.visibility} ")

                            if (method.generics.isNotEmpty())
                                builder.append("&lt;${method.generics.map { it.name }.joinString(", ", "")}&gt; ")

                            if (method.mods.isNotEmpty())
                                builder.append("${method.mods.joinString(" ")} ")

                            builder.append(link(info.fullName, method.returnType, generics))

                            append(builder.toString())
                        })
                    })

                    appendChild(Element("div").apply {
                        classNames(classes + "col-second")
                        appendChild(Element("code").apply {
                            append("<a href=\"#${method.name}${method.paramString}\" class=\"member-name-link\">${method.name}</a>${
                                if (method.parameters.isEmpty()) "()" else "(" + method.parameters.joinToString { param ->
                                    "${
                                        link(
                                            info.fullName,
                                            param.type,
                                            generics
                                        )
                                    } ${param.name}"
                                } + ")"
                            }")
                        })
                    })

                    appendChild(Element("div").apply {
                        classNames(classes + "col-last")

                        if (method.annotations.any { it.type == DEPRECATED_TYPE } || info.deprecated.isNotEmpty()) {
                            appendChild(Element("div").apply {
                                addClass("block")

                                append("<span class=\"deprecated-label\">Deprecated.</span>")
                                if (method.deprecated.isNotEmpty())
                                    append("<div class=\"deprecation-comment\">${method.deprecated.header()}</div>")
                            })
                        } else
                            append("<div class=\"block\">${method.comment.header()}</div>")
                    })

                    even = !even
                }
            }))
        }

        for ((type, map) in inheritedMethods)
            for ((name, inherited) in map) {
                if (inherited.isEmpty()) continue
                summary.appendChild(Element("div").apply {
                    addClass("inherited-list")

                    appendChild(Element("h3").apply {
                        id("methods-inherited-from-class-$name")
                        append("Methods inherited from $type ${name.substringBeforeLast('.')}.${link(info.fullName, name)}")
                    })

                    append("<code>${inherited.joinToString()}</code>")
                })
            }

        return summary
    }

    // Class Generators - Detail

    fun generateEnumDetail(info: ClassDocumentation): Element? {
        if (info.type != "enum") return null

        val enums = info.enumerations?.enums?.sortedBy { it.name } ?: return null
        val detail = Element("section").apply {
            addClass("constants-details")
            id("enum-constant-detail")
        }
        detail.append("<h2>Enum Constant Detail</h2>")

        detail.appendChild(Element("ul").apply {
            addClass("member-list")

            for (enum in enums) {
                appendChild(item(Element("section").apply {
                    id(enum.name)
                    addClass("detail")

                    append("<h3>${enum.name}</h3>")
                    appendChild(Element("div").apply {
                        addClass("member-signature")

                        val annotations = enum.annotations.map { annotation ->
                            val base = link(info.name, annotation.type, info.generics.map { it.name }, true)
                            if (annotation.parameters.isEmpty()) base
                            else "$base(${annotation.parameters.joinToString(", ") { param -> "${param.name} = ${param.value}" }})"
                        }
                        if (annotations.isNotEmpty())
                            append("<span class=\"annotations\">${annotations.joinString(" ", "")}</span><br>")

                        append("<span class=\"modifiers\">public static final</span>&nbsp;")
                        append("<span class=\"return-type\"><a href=\"${info.simpleName}.html\" title=\"enum in ${info.pkg}\">${info.simpleName}</a></span>&nbsp;")
                        append("<span class=\"element-name\">${enum.name}</span>")
                    })

                    if (enum.annotations.any { it.type == DEPRECATED_TYPE } || info.deprecated.isNotEmpty()) {
                        appendChild(Element("div").apply {
                            addClass("deprecation-block")

                            append("<span class=\"deprecated-label\">Deprecated.</span>")
                            if (enum.deprecated.isNotEmpty())
                                append("<div class=\"deprecation-comment\">${enum.deprecated}</div>")
                        })
                    }

                    append("<div class=\"block\">${enum.comment}</div>")
                }))
            }
        })

        return detail
    }

    fun generateFieldDetail(info: ClassDocumentation): Element? {
        val fields = info.fields?.fields?.sortedWith(compareBy(
            {
                when {
                    it.mods.contains("static") && it.mods.contains("final") -> 0
                    it.mods.contains("static") -> 1
                    it.mods.contains("final") -> 2
                    else -> 3
                }
            },
            { it.name }
        )) ?: return null
        val detail = Element("section").apply {
            addClass("field-details")
            id("field-detail")
        }

        detail.append("<h2>Field Detail</h2>")
        detail.appendChild(Element("ul").apply {
            addClass("member-list")

            for (field in fields) {
                appendChild(item(Element("section").apply {
                    addClass("detail")
                    id(field.name)

                    append("<h3>${field.name}</h3>")
                    appendChild(Element("div").apply {
                        addClass("member-signature")

                        val annotations = field.annotations.map { annotation ->
                            val base = link(info.name, annotation.type, info.generics.map { it.name }, true)
                            if (annotation.parameters.isEmpty()) base
                            else "$base(${annotation.parameters.joinToString(", ") { param -> "${param.name} = ${param.value}" }})"
                        }
                        if (annotations.isNotEmpty())
                            append("<span class=\"annotations\">${annotations.joinString(" ", "")}</span><br>")

                        val builder = StringBuilder()
                        if (field.visibility != "public")
                            builder.append("${field.visibility} ")

                        if (field.mods.isNotEmpty())
                            builder.append(field.mods.joinString(" "))

                        builder.append(link(info.fullName, field.type, info.generics.map { it.name }))

                        append("<span class=\"modifiers\">$builder</span>&nbsp;")
                        append("<span class=\"element-name\">${field.name}</span>")
                    })

                    if (field.annotations.any { it.type == DEPRECATED_TYPE } || info.deprecated.isNotEmpty()) {
                        appendChild(Element("div").apply {
                            addClass("deprecation-block")

                            append("<span class=\"deprecated-label\">Deprecated.</span>")
                            if (field.deprecated.isNotEmpty())
                                append("<div class=\"deprecation-comment\">${field.deprecated}</div>")
                        })
                    }

                    append("<div class=\"block\">${field.comment}</div>")

                    if (field.value != null)
                        appendChild(Element("dl").apply {
                            addClass("notes")

                            append("<dt>See Also:</dt>")
                            append("<dd><ul class=\"see-list\"><li><a href=\"/constant-values.html#${info.name}.${field.name}\">Constant Field Values</a></li></ul></dd>")
                        })
                }))
            }
        })

        return detail
    }

    fun generateConstructorDetail(info: ClassDocumentation): Element? {
        val constructors = info.constructors?.constructors?.sortedWith(compareBy(
            { !it.primary },
            { it.parameters.size },
            { constr -> constr.parameters.joinToString(",") { it.type } }
        )) ?: return null
        val detail = Element("section").apply {
            addClass("constructor-details")
            id("constructor-detail")
        }

        detail.append("<h2>Constructor Detail</h2>")
        detail.appendChild(Element("ul").apply {
            addClass("member-list")

            for (constructor in constructors) {
                appendChild(item(Element("section").apply {
                    addClass("detail")
                    id("<init>(${constructor.parameters.map { it.type }.joinString(",", "")})")

                    append("<h3>${info.simpleName}</h3>")
                    appendChild(Element("div").apply {
                        addClass("member-signature")

                        val annotations = constructor.annotations.map { annotation ->
                            val base = link(info.name, annotation.type, info.generics.map { it.name }, true)
                            if (annotation.parameters.isEmpty()) base
                            else "$base(${annotation.parameters.joinToString(", ") { param -> "${param.name} = ${param.value}" }})"
                        }
                        if (annotations.isNotEmpty())
                            append("<span class=\"annotations\">${annotations.joinString(" ", "")}</span><br>")

                        append("<span class=\"modifiers\">${constructor.visibility}</span>&nbsp;")
                        append("<span class=\"element-name\">${info.simpleName}</span>")
                        append("<wbr>")
                        appendChild(Element("span").apply {
                            addClass("parameters")
                            append("(${constructor.parameters.joinToString(separator = ", \n") { 
                                param -> "${param.annotations.map { annotation -> link(info.fullName, annotation.type, info.generics.map { it.name }, true) }.joinString(" ")}${link(info.fullName, param.type, info.generics.map { it.name })}&nbsp;${param.name}" 
                            }})")
                        })
                    })

                    if (constructor.annotations.any { it.type == DEPRECATED_TYPE } || info.deprecated.isNotEmpty()) {
                        appendChild(Element("div").apply {
                            addClass("deprecation-block")

                            append("<span class=\"deprecated-label\">Deprecated.</span>")
                            if (constructor.deprecated.isNotEmpty())
                                append("<div class=\"deprecation-comment\">${constructor.deprecated}</div>")
                        })
                    }

                    append("<div class=\"block\">${constructor.comment}</div>")

                    if (constructor.parameters.isNotEmpty()) {
                        appendChild(Element("dl").apply {
                            addClass("notes")
                            append("<dt>Parameters:</dt>")
                            for (param in constructor.parameters)
                                append("<dd><code>${param.name}</code> - ${param.comment}</dd>")
                        })
                    }
                }))
            }
        })

        return detail
    }

    fun generateMethodDetail(info: ClassDocumentation): Element? {
        val methods = info.methods?.methods?.sortedWith(compareBy(
            { it.name },
            { it.parameters.size },
            { method -> method.parameters.joinToString(",") { it.type } }
        )) ?: return null
        val detail = Element("section").apply {
            addClass("method-details")
            id("method-detail")
        }

        detail.append("<h2>Method Details</h2>")
        detail.appendChild(Element("ul").apply {
            addClass("member-list")

            for (method in methods) {
                appendChild(item(Element("section").apply {
                    addClass("detail")
                    id(method.fullName)

                    append("<h3>${method.name}</h3>")
                    appendChild(Element("div").apply {
                        addClass("member-signature")

                        val annotations = method.annotations.map { annotation ->
                            val base = link(info.name, annotation.type, info.generics.map { it.name }, true)
                            if (annotation.parameters.isEmpty()) base
                            else "$base(${annotation.parameters.joinToString(", ") { param -> "${param.name} = ${param.value}" }})"
                        }
                        if (annotations.isNotEmpty())
                            append("<span class=\"annotations\">${annotations.joinString(" ", "")}</span><br>")

                        val generics = method.generics.map { it.name }
                        if (generics.isNotEmpty())
                            append("<span class=\"type-parameters\">&lt;${generics.joinString(", ", "")}&gt;</span>&nbsp;")

                        val lGenerics = if (method.mods.contains("static")) emptyList() else info.generics.map { it.name }
                        append("<span class=\"modifiers\">${method.visibility} ${method.mods.joinString(" ")}</span>")
                        append("<span class=\"return-type\">${link(info.fullName, method.returnType, lGenerics)}</span>&nbsp;")

                        if (method.parameters.isEmpty())
                            append("<span class=\"element-name\">${method.name}</span>()")
                        else {
                            append("<span class=\"element-name\">${method.name}</span>")
                            append("<wbr>")
                            append("(${method.parameters.joinToString { param -> 
                                "${param.annotations.map { annotation -> link(info.fullName, annotation.type, info.generics.map { it.name }, true) }.joinString(" ")}${link(info.fullName, param.type, lGenerics)}&nbsp;${param.name}" 
                            }})")
                        }
                    })

                    if (method.annotations.any { it.type == DEPRECATED_TYPE } || info.deprecated.isNotEmpty()) {
                        appendChild(Element("div").apply {
                            addClass("deprecation-block")

                            append("<span class=\"deprecated-label\">Deprecated.</span>")
                            if (method.deprecated.isNotEmpty())
                                append("<div class=\"deprecation-comment\">${method.deprecated}</div>")
                        })
                    }

                    append("<div class=\"block\">${method.comment}</div>")

                    appendChild(Element("dl").apply {
                        addClass("notes")

                        if (method.generics.isNotEmpty()) {
                            append("<dt>Type Parameters:</dt>")
                            for (generic in method.generics)
                                append("<dd><code>${generic.name}</code> - ${generic.comment}</dd>")
                        }

                        if (method.parameters.isNotEmpty()) {
                            append("<dt>Parameters:</dt>")
                            for (param in method.parameters)
                                append("<dd><code>${param.name}</code> - ${param.comment}</dd>")
                        }

                        if (method.returnType != "void") {
                            append("<dt>Returns:</dt>")
                            append("<dd>${method.returnComment}</dd>")
                        }

                        if (method.throws.isNotEmpty()) {
                            append("<dt>Throws:</dt>")
                            for ((exception, comment) in method.throws)
                                append("<dd><code>${link(info.fullName, exception, info.generics.map { it.name })}</code> - $comment</ddt>")
                        }
                    })
                }))
            }
        })

        return detail
    }

    // Utility Methods

    fun link(self: String, type: String, generics: List<String> = emptyList(), annotation: Boolean = false, fullName: Boolean = false): String {
        var finalType = type.serialize()
        val processed = type.split("#")[0]
        val suffix = (if (type.contains("#")) "#${type.split("#")[1]}" else "")
        val prefix = if (annotation) "@" else ""

        for (child in processed.split("[<>,\\s]".toRegex()).filter { it.isNotBlank() }.map { it.replace("(\\.{3}|\\s)".toRegex(), "") }) {
            if (child == "?") continue
            if (generics.contains(child)) {
                val arrayBuilder = StringBuilder()
                for (i in 0 until child.count { it == '[' })
                    arrayBuilder.append("[]")

                val regex = "(.*)($child)(.*)".toRegex()

                finalType = finalType.replace(suffix, "")
                finalType = regex.replace(finalType) {
                     "${it.groupValues[1]}<a href=\"/${self.url()}.html#class-description\" title=\"member in $self\">$prefix$child$suffix</a>$arrayBuilder${it.groupValues[3]}"
                }
                continue
            }

            val newType = ClassDocumentation.processType(self, child).replace("[\\[\\]]".toRegex(), "")
            if (!newType.contains(".")) continue

            val pkg = newType.substring(0, newType.lastIndexOf('.'))
            val linkName = if (fullName) newType else newType.substring(newType.lastIndexOf('.') + 1)
            val docUrl = "${newType.url()}.html$suffix"

            val arrayBuilder = StringBuilder()
            for (i in 0 until child.count { it == '[' })
                arrayBuilder.append("[]")

            if (Util.getScheduled().any { it == newType } || self == newType)
                finalType = finalType.replace(suffix, "").replace(child, "<a href=\"/${docUrl}\" title=\"member in $pkg\">$prefix$linkName$suffix</a>$arrayBuilder")
            else {
                fun externalLink(repo: String, str: String)
                    = str.replace(suffix, "").replace(child, "<a href=\"${repo + docUrl}\" title=\"member in $pkg\">$prefix$linkName$suffix</a>$arrayBuilder")

                try {
                    val repo = Util.repository(newType)
                    finalType = externalLink(repo, finalType)
                } catch (ignored: IllegalArgumentException) {}
            }
        }

        return finalType.replace('$', '.')
    }

    fun generics(clazz: ClassDocumentation, links: Boolean = true): String {
        val generics = clazz.generics
        if (generics.isEmpty()) return ""

        val builder = StringBuilder()
        builder.append("&lt;")
        for ((i, generic) in generics.withIndex()) {
            builder.append(generic.name)

            if (generic.extends.isNotEmpty())
                builder.append(" extends ${generic.extends.joinToString(" & ") { g -> if (links) link(clazz.name, g, generics.map { it.name }) else g }}")

            if (generic.supers.isNotEmpty())
                builder.append(" super ${generic.supers.joinToString(" & ") { g -> if (links) link(clazz.name, g, generics.map { it.name }) else g }}")

            if (i != generics.size - 1)
                builder.append(", ")
        }

        builder.append("&gt;")

        return builder.toString()
    }

}