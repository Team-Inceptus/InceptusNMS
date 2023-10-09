package us.teaminceptus.inceptusnms.generation

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
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
    val comment: String,
    val enumerations: EnumsDocumentation? = null,
    val fields: FieldsDocumentation? = null,
    val constructors: ConstructorsDocumentation? = null,
    val methods: MethodsDocumentation? = null,
) {

    val pkg: String = name.substring(0, name.lastIndexOf('.'))
    val simpleName: String = name.substring(name.lastIndexOf('.') + 1).replace("$", ".")
    val docName: String = "$simpleName${generics(this)}"
    val fullDocName: String
        get() {
            return if (generics.isEmpty()) name
            else "$name${generics(this)}"
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
    )

    data class AnnotationDocumentation(
        val type: String,
        val parameters: List<ParameterDocumentation> = emptyList(),
    )

    data class EnumDocumentation(
        val name: String,
        val annotations: List<AnnotationDocumentation> = emptyList(),
        val comment: String
    )

    data class FieldDocumentation(
        val type: String,
        val name: String,
        val visibility: String = "public",
        val mods: List<String> = emptyList(),
        val annotations: List<AnnotationDocumentation> = emptyList(),
        val value: String? = null,
        val comment: String
    )

    data class ConstructorDocumentation(
        val visibility: String = "public",
        val parameters: List<ParameterDocumentation> = emptyList(),
        val annotations: List<AnnotationDocumentation> = emptyList(),
        val comment: String
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
        val comment: String
    ) {

        val cleanName: String
            get() {
                if (parameters.isEmpty()) return "$name()"

                val params = parameters.map {
                    if (it.type.contains("."))
                        it.type.substring(it.type.lastIndexOf('.') + 1)
                    else
                        it.type
                }

                return "$name(${params.joinToString(",")})"
            }

        val fullName: String
            get() {
                if (parameters.isEmpty()) return "$name()"
                val params = parameters.map { it.type }

                return "$name(${params.joinToString(",")})"
            }
    }

    // Properties

    data class EnumsDocumentation(val enums: List<EnumDocumentation> = emptyList())
    data class FieldsDocumentation(val fields: List<FieldDocumentation> = emptyList())
    data class ConstructorsDocumentation(val constructors: List<ConstructorDocumentation> = emptyList())
    data class MethodsDocumentation(val methods: List<MethodDocumentation> = emptyList())

    // Util Methods

    companion object {

        fun processComment(name: String, comment: String): String {
            var newComment = comment
            val children = comment.split("\\s".toRegex()).filter { it.isNotBlank() }

            for (child in children) {
                when {
                    child.contains("@(") && !child.contains("\\@(") -> {
                        val str = child.substring(child.indexOf("@(") + 2, child.indexOf(")"))
                        newComment = newComment.replace(child, child.replace("@($str)", link(name, str)))
                    }
                    child.contains("\\@(") -> child.replace("\\@(", "@(")
                }
            }

            return newComment
        }

        fun processType(name: String, type: String): String =
            Util.mapTypeAliases(type)
                .replace("{this}", name)


        fun params(name: String, json: JsonElement?): List<ParameterDocumentation> {
            if (json == null) return emptyList()

            return json.jsonArray.map {
                val param = it.jsonObject

                ParameterDocumentation(
                    processType(name, param["type"]!!.jsonPrimitive.content),
                    param["name"]!!.jsonPrimitive.content,
                    annotations(name, param["annotations"]),
                    processComment(name, param["comment"]!!.jsonPrimitive.content)
                )
            }
        }

        fun annotations(name: String, json: JsonElement?): List<AnnotationDocumentation> {
            if (json == null) return emptyList()

            return json.jsonArray.map {
                val annotation = it.jsonObject

                AnnotationDocumentation(
                    processType(name, annotation.jsonObject["type"]!!.jsonPrimitive.content),
                    params(name, annotation.jsonObject["params"])
                )
            }
        }

        suspend fun fromJson(name: String, json: JsonObject, callback: (ClassDocumentation) -> Unit) = coroutineScope {
            val clazz = json["class"]!!.jsonObject
            val type = clazz["type"]!!.jsonPrimitive.content

            val generics = async {
                if (clazz.contains("generics"))
                    clazz["generics"]!!.jsonObject.map { generic ->
                        async {
                            val obj = generic.value.jsonObject

                            GenericDocumentation(
                                generic.key,
                                obj["extends"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                                obj["supers"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
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
                                annotations(name, obj["annotations"]),
                                processComment(name, obj["comment"]!!.jsonPrimitive.content)
                            )
                        }
                    }.awaitAll())
                else
                    null
            }

            val fields = async {
                if (json.contains("fields"))
                    FieldsDocumentation(json["fields"]!!.jsonObject.map { field ->
                        async {
                            val obj = field.value.jsonObject

                            FieldDocumentation(
                                processType(name, obj["type"]!!.jsonPrimitive.content),
                                field.key,
                                (obj["visibility"]?.jsonPrimitive?.content ?: "public").replace("package", "package-private"),
                                obj["mods"]?.jsonArray?.map { it.jsonPrimitive.content } ?: if (type == "interface") listOf("static", "final") else emptyList(),
                                annotations(name, obj["annotations"]),
                                obj["value"]?.jsonPrimitive?.content,
                                processComment(name, obj["comment"]!!.jsonPrimitive.content)
                            )
                        }
                    }.awaitAll())
                else
                    null
            }

            val constructors = async {
                if (type != "interface")
                    ConstructorsDocumentation(json["constructors"]?.jsonArray?.map {
                        async {
                            val obj = it.jsonObject

                            ConstructorDocumentation(
                                (obj["visibility"]?.jsonPrimitive?.content ?: (if (type == "enum") "private" else "public")).replace("package", "package-private"),
                                params(name, obj["params"]),
                                annotations(name, obj["annotations"]),
                                processComment(name, obj["comment"]!!.jsonPrimitive.content)
                            )
                        }
                    }?.awaitAll() ?: listOf(ConstructorDocumentation(clazz["comment"]!!.jsonPrimitive.content)))
                else
                    null
            }

            val fieldsV = fields.await()

            val methods = async {
                if (json.contains("methods")) {
                    fun construct(method: String, obj: JsonObject): MethodDocumentation {
                        //<editor-fold desc="Construct">
                        if (obj["\$getter"] != null && fieldsV != null)
                            return fieldsV.fields.first { it.name == obj["\$getter"]!!.jsonPrimitive.content }.let { field ->
                                MethodDocumentation(
                                    method,
                                    (obj["visibility"]?.jsonPrimitive?.content ?: "public").replace("package", "package-private"),
                                    field.mods - "final",
                                    emptyList(),
                                    emptyList(),
                                    field.type,
                                    processComment(name, obj["return"]?.jsonObject?.get("comment")?.jsonPrimitive?.content ?: field.comment),
                                    obj["throws"]?.jsonArray?.associate {
                                        processType(name, it.jsonObject["type"]!!.jsonPrimitive.content) to processComment(name, it.jsonObject["comment"]!!.jsonPrimitive.content)
                                    } ?: emptyMap(),
                                    annotations(name, obj["annotations"]),
                                    when {
                                        obj["comment"] != null -> processComment(name, obj["comment"]!!.jsonPrimitive.content)
                                        else -> "Gets ${field.comment.replaceFirstChar { it.lowercase() }}"
                                    }
                                )
                            }

                        if (obj["\$setter"] != null && fieldsV != null)
                            return fieldsV.fields.first { it.name == obj["\$setter"]!!.jsonPrimitive.content }.let { field ->
                                MethodDocumentation(
                                    method,
                                    (obj["visibility"]?.jsonPrimitive?.content ?: "public").replace("package", "package-private"),
                                    field.mods - "final",
                                    emptyList(),
                                    listOf(ParameterDocumentation(field.type, "value", emptyList(), field.comment)),
                                    processType(name, obj["return"]?.jsonObject?.get("type")?.jsonPrimitive?.content ?: "void"),
                                    processComment(name, obj["return"]?.jsonObject?.get("comment")?.jsonPrimitive?.content ?: ""),
                                    obj["throws"]?.jsonArray?.associate {
                                        processType(name, it.jsonObject["type"]!!.jsonPrimitive.content) to processComment(name, it.jsonObject["comment"]!!.jsonPrimitive.content)
                                    } ?: emptyMap(),
                                    annotations(name, obj["annotations"]),
                                    when {
                                        obj["comment"] != null -> processComment(name, obj["comment"]!!.jsonPrimitive.content)
                                        else -> "Sets ${field.comment.replaceFirstChar { it.lowercase() }}"
                                    }
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
                                    g["extends"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                                    g["supers"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                                    processComment(name, g["comment"]!!.jsonPrimitive.content)
                                )
                            } ?: emptyList(),
                            params(name, obj["params"]),
                            processType(name, obj["return"]?.jsonObject?.get("type")?.jsonPrimitive?.content ?: "void"),
                            processComment(name, obj["return"]?.jsonObject?.get("comment")?.jsonPrimitive?.content ?: ""),
                            obj["throws"]?.jsonArray?.associate {
                                processType(name, it.jsonObject["type"]!!.jsonPrimitive.content) to processComment(name, it.jsonObject["comment"]!!.jsonPrimitive.content)
                            } ?: emptyMap(),
                            annotations(name, obj["annotations"]),
                            processComment(name, obj["comment"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing comment for method '$method' in '$name'"))
                        )
                        //</editor-fold>
                    }

                    MethodsDocumentation(json["methods"]!!.jsonObject.map { method ->
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
                } else null
            }

            val enclosing = async { clazz["enclosing"]?.jsonPrimitive?.content }.await()

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
                async { clazz["mods"]?.jsonArray?.map { it.jsonPrimitive.content } ?: if (type == "enum" || type == "interface" && enclosing != null) listOf("static") else emptyList() }.await(),
                async { processComment(name, clazz["comment"]!!.jsonPrimitive.content) }.await(),
                enums.await(),
                fields.await(),
                constructors.await(),
                methods.await()
            )

            callback(info)
        }

    }
}
