package us.teaminceptus.inceptusnms.generation

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jsoup.Jsoup
import us.teaminceptus.inceptusnms.generation.DocGenerator.generics
import us.teaminceptus.inceptusnms.generation.DocGenerator.link

data class ClassDocumentation(
    val type: String,
    val name: String,
    val extends: String? = null,
    val implements: List<String> = emptyList(),
    val generics: List<GenericDocumentation> = emptyList(),
    val enclosing: String? = null,
    val visibility: String = "public",
    val mods: List<String> = emptyList(),
    val annotations: List<AnnotationDocumentation> = emptyList(),
    val comment: String,
    val enumerations: EnumsDocumentation? = null,
    val fields: FieldsDocumentation? = null,
    val constructors: ConstructorsDocumentation? = null,
    val methods: MethodsDocumentation? = null,
    val deprecated: String = ""
) {

    var unfinished: Boolean = false

    val pkg: String = name.substring(0, name.lastIndexOf('.'))
    val simpleName: String = name.substring(name.lastIndexOf('.') + 1).replace("$", ".")
    val docName: String = "$simpleName${generics(this)}"
    val fullDocName: String
        get() {
            val name = link(name, name, generics.map { it.name })

            return if (generics.isEmpty()) name
            else "$name${generics(this)}"
        }
    val fullName: String
        get() {
            return if (generics.isEmpty()) name
            else "$name${generics(this, false)}"
        }

    // Definitions
    
    data class GenericDocumentation(
        val name: String,
        val extends: List<String> = emptyList(),
        val supers: List<String> = emptyList(),
        val comment: String
    )

    data class ParameterDocumentation(
        val type: String,
        val name: String,
        val annotations: List<AnnotationDocumentation> = emptyList(),
        val comment: String,
        val value: String? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ParameterDocumentation

            if (type != other.type) return false
            if (name != other.name) return false
            if (annotations != other.annotations) return false
            if (comment != other.comment) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + annotations.hashCode()
            result = 31 * result + comment.hashCode()
            return result
        }
    }

    data class AnnotationDocumentation(
        val type: String,
        val parameters: List<ParameterDocumentation> = emptyList(),
    ) {

        companion object {
            val DEPRECATED_DEFAULT = AnnotationDocumentation(
                "java.lang.Deprecated",
                emptyList()
            )
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AnnotationDocumentation

            if (type != other.type) return false
            if (parameters != other.parameters) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + parameters.hashCode()
            return result
        }

    }

    data class EnumDocumentation(
        val name: String,
        val annotations: List<AnnotationDocumentation> = emptyList(),
        val comment: String,
        val deprecated: String = ""
    )

    data class FieldDocumentation(
        val type: String,
        val name: String,
        val visibility: String = "public",
        val mods: List<String> = emptyList(),
        val annotations: List<AnnotationDocumentation> = emptyList(),
        val value: String? = null,
        val comment: String,
        val deprecated: String = ""
    )

    data class ConstructorDocumentation(
        val visibility: String = "public",
        val parameters: List<ParameterDocumentation> = emptyList(),
        val annotations: List<AnnotationDocumentation> = emptyList(),
        val comment: String,
        val deprecated: String = "",
        val primary: Boolean = false
    ) {
        constructor(comment: String) : this("public", comment = comment)
    }

    data class MethodDocumentation(
        val name: String,
        val visibility: String = "public",
        val mods: List<String> = emptyList(),
        val generics: List<GenericDocumentation> = emptyList(),
        val parameters: List<ParameterDocumentation> = emptyList(),
        val returnType: String = "void",
        val returnComment: String = "",
        val throws: Map<String, String> = emptyMap(),
        val annotations: List<AnnotationDocumentation> = emptyList(),
        val comment: String,
        val deprecated: String = ""
    ) {
        val cleanName: String
            get() {
                if (parameters.isEmpty()) return "$name()"

                val params = parameters.map {
                    (if (it.type.contains("."))
                        it.type.substring(it.type.lastIndexOf('.') + 1)
                    else
                        it.type).replace("$", ".")
                }

                return "$name(${params.joinToString(",")})"
            }

        val fullName: String
            get() {
                if (parameters.isEmpty()) return "$name()"
                val params = parameters.map { it.type }

                return "$name(${params.joinToString(",")})"
            }

        val paramString: String = if (parameters.isEmpty()) "()" else "(" + parameters.joinToString { it.type }.replace(" ", "") + ")"

        companion object {
            val VALUES = buildJsonObject {
                put("mods", buildJsonArray { add("static") })
                put("return", buildJsonObject {
                    put("type", "{this}[]")
                    put("comment", "an array containing the constants of this enum type, in the order they are declared.")
                })
                put("comment", JsonPrimitive("Returns an array containing the constants of this enum class, in the order they are declared."))
            }

            val VALUE_OF = buildJsonObject {
                put("mods", buildJsonArray { add("static") })
                put("params", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "string")
                        put("name", "name")
                        put("comment", "the name of the constant to returned.")
                    })
                })
                put("return", buildJsonObject {
                    put("type", "{this}")
                    put("comment", "the enum constant with the specified name")
                })
                put("throws", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "java.lang.IllegalArgumentException")
                        put("comment", "if this enum class has no constant with the specified name")
                    })

                    add(buildJsonObject {
                        put("type", "java.lang.NullPointerException")
                        put("comment", "if the argument is null")
                    })
                })
                put("comment", JsonPrimitive("Returns the enum constant of this class with the specified name. The string must match <i>exactly</i> an identifier used to declare an enum constant in this class. (Extraneous whitespace characters are not permitted.)"))
            }
        }
    }

    // Properties

    data class EnumsDocumentation(val enums: List<EnumDocumentation> = emptyList())
    data class FieldsDocumentation(val fields: List<FieldDocumentation> = emptyList())
    data class ConstructorsDocumentation(var constructors: List<ConstructorDocumentation> = emptyList())
    data class MethodsDocumentation(val methods: List<MethodDocumentation> = emptyList())

    // Util Methods

    companion object {

        private val markdownFlavorDescriptor = CommonMarkFlavourDescriptor()
        private val markdownParser = MarkdownParser(markdownFlavorDescriptor)

        fun processComment(name: String, comment: String): String {
            if (comment.isEmpty()) return ""

            var newComment = comment
            val children = comment.split("\\s".toRegex()).filter { it.isNotBlank() }

            for (child in children) {
                when {
                    child.contains("@(") && !child.contains("\\@(") -> {
                        val str = child.substring(child.indexOf("@(") + 2, child.lastIndexOf(")"))
                        newComment = newComment.replace(child, child.replace("@($str)", link(name, str)))
                    }
                    child.contains("\\@(") -> child.replace("\\@(", "@(")
                }
            }

            val parsed = markdownParser.buildMarkdownTreeFromString(newComment)
            val text = HtmlGenerator(newComment, parsed, markdownFlavorDescriptor).generateHtml()
            return Jsoup.parse(text).selectFirst("body > p")?.html() ?: text
        }

        fun processType(name: String, type: String): String =
            Util.mapTypeAliases(type)
                .replace("{this}", name)
                .replace("{pkg}", name.substring(0, name.lastIndexOf('.')))


        fun params(name: String, json: JsonElement?): List<ParameterDocumentation> {
            if (json == null) return emptyList()

            return json.jsonArray.map {
                val param = it.jsonObject

                val type = param["type"]?.jsonPrimitive?.content
                val comment = param["comment"]?.jsonPrimitive?.content

                ParameterDocumentation(
                    if (type == null) "" else processType(name, type),
                    param["name"]!!.jsonPrimitive.content,
                    annotations(name, param["annotations"]),
                    if (comment == null) "" else processComment(name, comment),
                    param["value"]?.jsonPrimitive?.content
                )
            }
        }

        fun annotations(name: String, json: JsonElement?, deprecated: Boolean = false): List<AnnotationDocumentation> {
            val added = json?.jsonArray?.map {
                val annotation = it.jsonObject

                AnnotationDocumentation(
                    processType(name, annotation.jsonObject["type"]!!.jsonPrimitive.content),
                    params(name, annotation.jsonObject["params"])
                )
            }?.toMutableList() ?: mutableListOf()

            if (!added.any { it.type == "java.lang.Deprecated" } && deprecated)
                added.add(AnnotationDocumentation.DEPRECATED_DEFAULT)

            return added.toList()
        }

        suspend fun fromJson(name: String, json: JsonObject, callback: (ClassDocumentation) -> Unit) = coroutineScope {
            val clazz = json["class"]!!.jsonObject
            val type = clazz["type"]!!.jsonPrimitive.content
            val annotations = async { annotations(name, clazz["annotations"], clazz["deprecated"] != null) }

            val generics = async {
                if (clazz.contains("generics"))
                    clazz["generics"]!!.jsonObject.map { generic ->
                        async {
                            val obj = generic.value.jsonObject

                            GenericDocumentation(
                                generic.key,
                                obj["extends"]?.jsonArray?.map { processType(name, it.jsonPrimitive.content) } ?: emptyList(),
                                obj["supers"]?.jsonArray?.map { processType(name, it.jsonPrimitive.content) } ?: emptyList(),
                                processComment(name, obj["comment"]!!.jsonPrimitive.content)
                            )
                        }
                    }.awaitAll()
                else
                    emptyList()
            }

            val enums = async {
                if (type == "enum" && json.contains("enumerations"))
                    EnumsDocumentation(json["enumerations"]!!.jsonArray.map {
                        async {
                            val obj = it.jsonObject

                            EnumDocumentation(
                                obj["name"]!!.jsonPrimitive.content,
                                annotations(name, obj["annotations"], obj["deprecated"] != null),
                                processComment(name, obj["comment"]!!.jsonPrimitive.content),
                                processComment(name, obj["deprecated"]?.jsonPrimitive?.content ?: "")
                            )
                        }
                    }.awaitAll())
                else
                    null
            }

            val constructors = async {
                if (type != "interface")
                    ConstructorsDocumentation(json["constructors"]?.jsonArray?.mapNotNull {
                        if (it.jsonObject["\$fields"] != null) return@mapNotNull null
                        async {
                            val obj = it.jsonObject

                            ConstructorDocumentation(
                                (obj["visibility"]?.jsonPrimitive?.content ?: (if (type == "enum") "private" else "public")).replace("package", "package-private"),
                                params(name, obj["params"]),
                                annotations(name, obj["annotations"], obj["deprecated"] != null),
                                processComment(name, obj["comment"]!!.jsonPrimitive.content),
                                processComment(name, obj["deprecated"]?.jsonPrimitive?.content ?: ""),
                                obj["primary"]?.jsonPrimitive?.content?.toBoolean() ?: false
                            )
                        }
                    }?.awaitAll() ?: listOf(ConstructorDocumentation(processComment(name, clazz["comment"]!!.jsonPrimitive.content))))
                else
                    null
            }
            val constructorsV = constructors.await()

            val fields = async {
                val list = mutableListOf<FieldDocumentation>()

                if (json.contains("fields"))
                    list.addAll(json["fields"]!!.jsonObject.map { field ->
                        async {
                            val obj = field.value.jsonObject

                            FieldDocumentation(
                                processType(name, obj["type"]!!.jsonPrimitive.content),
                                field.key,
                                (obj["visibility"]?.jsonPrimitive?.content ?: "public").replace("package", "package-private"),
                                obj["mods"]?.jsonArray?.map { it.jsonPrimitive.content } ?: if (type == "interface") listOf("static", "final") else emptyList(),
                                annotations(name, obj["annotations"], obj["deprecated"] != null),
                                obj["value"]?.jsonPrimitive?.content,
                                processComment(name, obj["comment"]!!.jsonPrimitive.content),
                                processComment(name, obj["deprecated"]?.jsonPrimitive?.content ?: "")
                            )
                        }
                    }.awaitAll())

                if (type == "record") {
                    val constr = constructorsV?.constructors?.maxByOrNull {
                        if (it.primary) Int.MAX_VALUE
                        it.parameters.size
                    }
                    if (constr != null)
                        for (param in constr.parameters)
                            list.add(
                                FieldDocumentation(
                                    param.type,
                                    param.name,
                                    "private",
                                    listOf("final"),
                                    emptyList(),
                                    null,
                                    param.comment
                                )
                            )
                }

                if (list.isNotEmpty())
                    FieldsDocumentation(list)
                else
                    null
            }
            val fieldsV = fields.await()

            if (constructorsV != null && fieldsV != null)
                constructorsV.constructors += json["constructors"]?.jsonArray?.mapNotNull { constructor ->
                    if (constructor.jsonObject["\$fields"] == null) return@mapNotNull null

                    async {
                        val obj = constructor.jsonObject
                        val cFields = obj["\$fields"]!!.jsonArray

                        ConstructorDocumentation(
                            (obj["visibility"]?.jsonPrimitive?.content ?: "public").replace("package", "package-private"),
                            cFields.map { jfieldName ->
                                val fieldName = jfieldName.jsonPrimitive.content
                                val field = fieldsV.fields.firstOrNull { it.name == fieldName } ?: throw IllegalArgumentException("No backing field for constructor parameter named '$fieldName' in $name")

                                ParameterDocumentation(field.type, fieldName, field.annotations, field.comment)
                            },
                            annotations(name, obj["annotations"], obj["deprecated"] != null),
                            processComment(name, obj["comment"]!!.jsonPrimitive.content),
                        )
                    }
                }?.awaitAll() ?: emptyList()

            val methods = async {
                val list = mutableListOf<MethodDocumentation>()

                fun construct(method: String, obj: JsonObject): MethodDocumentation {
                    //<editor-fold desc="Construct">
                    if (obj["\$getter"] != null && fieldsV != null)
                        return fieldsV.fields.firstOrNull { it.name == obj["\$getter"]!!.jsonPrimitive.content }.let { field ->
                            if (field == null) throw IllegalArgumentException("No backing field for getter parameter named '${obj["\$getter"]}' in $name")

                            MethodDocumentation(
                                method,
                                (obj["visibility"]?.jsonPrimitive?.content ?: "public").replace("package", "package-private"),
                                field.mods - listOf("final", "volatile"),
                                emptyList(),
                                emptyList(),
                                obj["return"]?.jsonObject?.get("type")?.jsonPrimitive?.content ?: field.type,
                                processComment(name, obj["return"]?.jsonObject?.get("comment")?.jsonPrimitive?.content ?: field.comment),
                                obj["throws"]?.jsonArray?.associate {
                                    processType(name, it.jsonObject["type"]!!.jsonPrimitive.content) to processComment(name, it.jsonObject["comment"]!!.jsonPrimitive.content)
                                } ?: emptyMap(),
                                annotations(name, obj["annotations"], obj["deprecated"] != null) + field.annotations,
                                when {
                                    obj["comment"] != null -> processComment(name, obj["comment"]!!.jsonPrimitive.content)
                                    else -> "Gets ${field.comment.replaceFirstChar { it.lowercase() }}"
                                },
                                processComment(name, obj["deprecated"]?.jsonPrimitive?.content ?: "")
                            )
                        }

                    if (obj["\$setter"] != null && fieldsV != null)
                        return fieldsV.fields.firstOrNull { it.name == obj["\$setter"]!!.jsonPrimitive.content }.let { field ->
                            if (field == null) throw IllegalArgumentException("No backing field for setter parameter named '${obj["\$setter"]}' in $name")

                            MethodDocumentation(
                                method,
                                (obj["visibility"]?.jsonPrimitive?.content ?: "public").replace("package", "package-private"),
                                field.mods - listOf("final", "volatile"),
                                emptyList(),
                                listOf(ParameterDocumentation(field.type, "value", emptyList(), field.comment)),
                                processType(name, obj["return"]?.jsonObject?.get("type")?.jsonPrimitive?.content ?: "void"),
                                processComment(name, obj["return"]?.jsonObject?.get("comment")?.jsonPrimitive?.content ?: ""),
                                obj["throws"]?.jsonArray?.associate {
                                    processType(name, it.jsonObject["type"]!!.jsonPrimitive.content) to processComment(name, it.jsonObject["comment"]!!.jsonPrimitive.content)
                                } ?: emptyMap(),
                                annotations(name, obj["annotations"], obj["deprecated"] != null),
                                when {
                                    obj["comment"] != null -> processComment(name, obj["comment"]!!.jsonPrimitive.content)
                                    else -> "Sets ${field.comment.replaceFirstChar { it.lowercase() }}"
                                },
                                processComment(name, obj["deprecated"]?.jsonPrimitive?.content ?: "")
                            )
                        }

                    return MethodDocumentation(
                        method,
                        (obj["visibility"]?.jsonPrimitive?.content ?: "public").replace("package", "package-private"),
                        obj["mods"]?.jsonArray?.map { it.jsonPrimitive.content } ?: (if (type == "interface") listOf("abstract") else emptyList()),
                        obj["generics"]?.jsonObject?.map { generic ->
                            val g = generic.value.jsonObject

                            GenericDocumentation(
                                generic.key,
                                g["extends"]?.jsonArray?.map { processType(name, it.jsonPrimitive.content) } ?: emptyList(),
                                g["supers"]?.jsonArray?.map { processType(name, it.jsonPrimitive.content) } ?: emptyList(),
                                processComment(name, g["comment"]!!.jsonPrimitive.content)
                            )
                        } ?: emptyList(),
                        params(name, obj["params"]),
                        processType(name, obj["return"]?.jsonObject?.get("type")?.jsonPrimitive?.content ?: "void"),
                        processComment(name, obj["return"]?.jsonObject?.get("comment")?.jsonPrimitive?.content ?: ""),
                        obj["throws"]?.jsonArray?.associate {
                            processType(name, it.jsonObject["type"]!!.jsonPrimitive.content) to processComment(name, it.jsonObject["comment"]!!.jsonPrimitive.content)
                        } ?: emptyMap(),
                        annotations(name, obj["annotations"], obj["deprecated"] != null),
                        processComment(name, obj["comment"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing comment for method '$method' in '$name'")),
                        processComment(name, obj["deprecated"]?.jsonPrimitive?.content ?: "")
                    )
                    //</editor-fold>
                }

                if (json.contains("methods"))
                    list.addAll(json["methods"]!!.jsonObject.map { method ->
                        try {
                            val obj = method.value.jsonArray
                            obj.map {
                                async {
                                    construct(method.key, it.jsonObject)
                                }
                            }.awaitAll()
                        } catch (ignored: IllegalArgumentException) {
                            async {
                                val obj = method.value.jsonObject
                                listOf(construct(method.key, obj))
                            }.await()
                        }
                    }.flatten())

                if (type == "enum") {
                    list.add(construct("values", MethodDocumentation.VALUES))
                    list.add(construct("valueOf", MethodDocumentation.VALUE_OF))
                }

                if (type == "record") {
                    val constr = constructorsV?.constructors?.maxByOrNull { it.parameters.size }
                    if (constr != null)
                        for (param in constr.parameters)
                            list.add(
                                MethodDocumentation(
                                    param.name,
                                    "public",
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    param.type,
                                    param.comment,
                                    emptyMap(),
                                    emptyList(),
                                    "Gets ${param.comment.replaceFirstChar { it.lowercase() }}"
                                )
                            )
                }

                if (list.isNotEmpty())
                    MethodsDocumentation(list)
                else
                    null
            }

            val enclosing = async {
                val rawEnclosing = clazz["enclosing"]?.jsonPrimitive?.content ?: return@async null

                processType(name, rawEnclosing)
            }.await()

            val info = ClassDocumentation(
                type,
                name,
                async {
                    val extends = clazz["extends"]?.jsonPrimitive?.content ?: when (type) {
                        "class" -> "java.lang.Object"
                        "record" -> "java.lang.Record"
                        "enum" -> "java.lang.Enum<{this}>"
                        else -> null
                    }

                    if (extends != null) processType(name, extends) else null
                }.await(),
                async { clazz["implements"]?.jsonArray?.map { processType(name, it.jsonPrimitive.content) } ?: emptyList() }.await(),
                generics.await(),
                enclosing,
                async { clazz["visibility"]?.jsonPrimitive?.content ?: "public" }.await().replace("package", "package-private"),
                async { clazz["mods"]?.jsonArray?.map { it.jsonPrimitive.content } ?: if ((type == "enum" || type == "interface") && enclosing != null) listOf("static") else emptyList() }.await(),
                annotations.await(),
                async { processComment(name, clazz["comment"]!!.jsonPrimitive.content) }.await(),
                enums.await(),
                fieldsV,
                constructorsV,
                methods.await(),
                async { processComment(name, clazz["deprecated"]?.jsonPrimitive?.content ?: "") }.await()
            )

            if (json["_"]?.jsonPrimitive?.content?.equals("UNFINISHED", ignoreCase = true) == true)
                info.unfinished = true

            callback(info)
        }

    }
}
