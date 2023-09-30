package us.teaminceptus.inceptusnms.generation

import kotlinx.serialization.json.*

data class ClassDocumentation(
    val type: String,
    val name: String,
    val extends: String? = null,
    val implements: List<String> = emptyList(),
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
    val simpleName: String = name.substring(name.lastIndexOf('.') + 1)
    val docName: String = enclosing?.let { "${it.substring(it.lastIndexOf('.') + 1)}.$simpleName" } ?: simpleName

    // Definitions

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
    )

    // Properties

    data class EnumsDocumentation(val enums: List<EnumDocumentation> = emptyList())
    data class FieldsDocumentation(val fields: List<FieldDocumentation> = emptyList())
    data class ConstructorsDocumentation(val constructors: List<ConstructorDocumentation> = emptyList())
    data class MethodsDocumentation(val methods: List<MethodDocumentation> = emptyList())

    // Util Methods

    companion object {

        fun processType(name: String, type: String): String {
            var newType = Util.mapTypeAliases(type)

            if (newType == "<this>")
                newType = name

            return newType
        }

        fun params(name: String, json: JsonElement?): List<ParameterDocumentation> {
            if (json == null) return emptyList()

            return json.jsonArray.map {
                val param = it.jsonObject

                ParameterDocumentation(
                    processType(name, param["type"]!!.jsonPrimitive.content),
                    param["name"]!!.jsonPrimitive.content,
                    param["comment"]!!.jsonPrimitive.content
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

            var enums: EnumsDocumentation? = null
            if (type == "enum" && json.contains("enumerations"))
                enums = EnumsDocumentation(json["enumerations"]!!.jsonArray.map {
                    val obj = it.jsonObject

                    EnumDocumentation(
                        obj["name"]!!.jsonPrimitive.content,
                        annotations(name, obj["annotations"]),
                        obj["comment"]!!.jsonPrimitive.content
                    )
                })

            var fields: FieldsDocumentation? = null
            if (json.contains("fields"))
                fields = FieldsDocumentation(json["fields"]!!.jsonObject.map { field ->
                    val obj = field.value.jsonObject

                    FieldDocumentation(
                        obj["type"]!!.jsonPrimitive.content,
                        field.key,
                        obj["visibility"]?.jsonPrimitive?.content ?: "public",
                        obj["mods"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                        annotations(name, obj["annotations"]),
                        obj["comment"]!!.jsonPrimitive.content
                    )
                })

            var constructors: ConstructorsDocumentation? = null
            if (type != "interface")
                constructors = ConstructorsDocumentation(json["constructors"]?.jsonArray?.map {
                    val obj = it.jsonObject

                    ConstructorDocumentation(
                        obj["visibility"]?.jsonPrimitive?.content ?: "public",
                        params(name, obj["params"]),
                        annotations(name, obj["annotations"]),
                        obj["comment"]!!.jsonPrimitive.content
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
                        obj["return"]?.jsonPrimitive?.content ?: "void",
                        obj["throws"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                        annotations(name, obj["annotations"]),
                        obj["comment"]!!.jsonPrimitive.content
                    )
                })

            return ClassDocumentation(
                type,
                name,
                clazz["extends"]?.jsonPrimitive?.content,
                clazz["implements"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                clazz["enclosing"]?.jsonPrimitive?.content,
                clazz["visibility"]?.jsonPrimitive?.content ?: "public",
                clazz["mods"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                clazz["comment"]!!.jsonPrimitive.content,
                enums,
                fields,
                constructors,
                methods
            )
        }

    }
}
