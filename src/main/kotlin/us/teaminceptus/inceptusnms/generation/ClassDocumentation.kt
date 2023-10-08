package us.teaminceptus.inceptusnms.generation

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

        fun fromJson(name: String, json: JsonObject): ClassDocumentation {
            val clazz = json["class"]!!.jsonObject
            val type = clazz["type"]!!.jsonPrimitive.content

            val generics = if (clazz.contains("generics"))
                clazz["generics"]!!.jsonObject.map { generic ->
                    val obj = generic.value.jsonObject

                    GenericDocumentation(
                        generic.key,
                        obj["extends"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                        obj["supers"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                        processComment(name, obj["comment"]!!.jsonPrimitive.content)
                    )
                }
            else
                emptyList()

            var enums: EnumsDocumentation? = null
            if (type == "enum" && json.contains("enumerations"))
                enums = EnumsDocumentation(json["enumerations"]!!.jsonArray.map {
                    val obj = it.jsonObject

                    EnumDocumentation(
                        obj["name"]!!.jsonPrimitive.content,
                        annotations(name, obj["annotations"]),
                        processComment(name, obj["comment"]!!.jsonPrimitive.content)
                    )
                })

            var fields: FieldsDocumentation? = null
            if (json.contains("fields"))
                fields = FieldsDocumentation(json["fields"]!!.jsonObject.map { field ->
                    val obj = field.value.jsonObject

                    FieldDocumentation(
                        processType(name, obj["type"]!!.jsonPrimitive.content),
                        field.key,
                        obj["visibility"]?.jsonPrimitive?.content ?: "public",
                        obj["mods"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                        annotations(name, obj["annotations"]),
                        processComment(name, obj["comment"]!!.jsonPrimitive.content)
                    )
                })

            var constructors: ConstructorsDocumentation? = null
            if (type != "interface")
                constructors = ConstructorsDocumentation(json["constructors"]?.jsonArray?.map {
                    val obj = it.jsonObject

                    ConstructorDocumentation(
                        obj["visibility"]?.jsonPrimitive?.content ?: (if (type == "enum") "private" else "public"),
                        params(name, obj["params"]),
                        annotations(name, obj["annotations"]),
                        processComment(name, obj["comment"]!!.jsonPrimitive.content)
                    )
                } ?: listOf(ConstructorDocumentation(clazz["comment"]!!.jsonPrimitive.content)))

            var methods: MethodsDocumentation? = null
            if (json.contains("methods")) {
                fun construct(method: String, obj: JsonObject): MethodDocumentation {
                    if (obj["\$getter"] != null && fields != null)
                        return fields.fields.first { it.name == obj["\$setter"]!!.jsonPrimitive.content }.let { field ->
                            MethodDocumentation(
                                method,
                                field.visibility,
                                field.mods,
                                emptyList(),
                                listOf(ParameterDocumentation(field.type, "value", emptyList(), field.comment)),
                                field.type,
                                field.comment,
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

                    if (obj["\$setter"] != null && fields != null)
                        return fields.fields.first { it.name == obj["\$setter"]!!.jsonPrimitive.content }.let { field ->
                            MethodDocumentation(
                                method,
                                field.visibility,
                                field.mods,
                                emptyList(),
                                listOf(ParameterDocumentation(field.type, "value", emptyList(), field.comment)),
                                "void",
                                "",
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
                        obj["visibility"]?.jsonPrimitive?.content ?: "public",
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
                        processComment(name, obj["comment"]!!.jsonPrimitive.content)
                    )
                }

                methods = MethodsDocumentation(json["methods"]!!.jsonObject.map { method ->
                    try {
                        val obj = method.value.jsonArray
                        obj.map { construct(method.key, it.jsonObject) }
                    } catch (ignored: IllegalArgumentException) {
                        val obj = method.value.jsonObject
                        listOf(construct(method.key, obj))
                    }
                }.flatten())
            }

            return ClassDocumentation(
                type,
                name,
                clazz["extends"]?.jsonPrimitive?.content,
                clazz["implements"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                generics,
                clazz["enclosing"]?.jsonPrimitive?.content,
                clazz["visibility"]?.jsonPrimitive?.content ?: "public",
                clazz["mods"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                processComment(name, clazz["comment"]!!.jsonPrimitive.content),
                enums,
                fields,
                constructors,
                methods
            )
        }

    }
}
