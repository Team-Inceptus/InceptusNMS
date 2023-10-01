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
        val parameters: List<ParameterDocumentation> = emptyList(),
        val returnType: String = "void",
        val throws: List<String> = emptyList(),
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

            for (str in comment.split("\\s".toRegex())) {
                if (str.contains("@(") && str.contains(")") && !str.startsWith("\\@("))
                    newComment = newComment.replace(str, link(name, str.substring(str.indexOf("@("), str.indexOf(")"))))

                if (str.contains("\\@("))
                    newComment = newComment.replace("\\@(", "@(")
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
            if (json.contains("methods"))
                methods = MethodsDocumentation(json["methods"]!!.jsonObject.map { method ->
                    val obj = method.value.jsonObject

                    MethodDocumentation(
                        method.key,
                        obj["visibility"]?.jsonPrimitive?.content ?: "public",
                        obj["mods"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                        params(name, obj["params"]),
                        processType(name, obj["return"]?.jsonPrimitive?.content ?: "void"),
                        obj["throws"]?.jsonArray?.map { processType(name, it.jsonPrimitive.content) } ?: emptyList(),
                        annotations(name, obj["annotations"]),
                        processComment(name, obj["comment"]!!.jsonPrimitive.content)
                    )
                })

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
