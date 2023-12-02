package us.teaminceptus.inceptusnms.template

import kotlinx.serialization.json.*
import us.teaminceptus.inceptusnms.generation.Util
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

object TemplateGenerator {

    fun type(raw: Class<*>): String {
        val builder = StringBuilder()
        if (raw.isArray)
            builder.append(type(raw.componentType)).append("[]")
        else
            builder.append(alias(raw.canonicalName))

        return builder.toString()
    }

    fun alias(type: String): String = Util.TYPE_ALIASES.entries.firstOrNull { it.value == type }?.key ?: type

    fun visibility(mods: Int): String? {
        return when {
            Modifier.isProtected(mods) -> "protected"
            Modifier.isPrivate(mods) -> "private"
            Modifier.isPublic(mods) -> null
            else -> "package-private"
        }
    }

    fun mods(mods: Int): List<String> {
        val modifiers = mutableListOf<String>()

        if (Modifier.isAbstract(mods)) modifiers.add("abstract")
        if (Modifier.isStatic(mods)) modifiers.add("static")
        if (Modifier.isFinal(mods)) modifiers.add("final")
        if (Modifier.isVolatile(mods)) modifiers.add("volatile")
        if (Modifier.isStrict(mods)) modifiers.add("strictfp")
        if (Modifier.isTransient(mods)) modifiers.add("transient")

        return modifiers.toList()
    }

    // Utility Extensions

    fun JsonObjectBuilder.put(key: String, value: Iterable<String>) {
        if (value.toList().isNotEmpty())
            put(key, JsonArray(value.map { JsonPrimitive(it) }))
    }

    fun JsonObjectBuilder.putAndCheck(key: String, value: String?) {
        if (value != null)
            put(key, value)
    }

    fun JsonObjectBuilder.putAndCheck(key: String, value: JsonArray) {
        if (value.isNotEmpty())
            put(key, value)
    }

    fun Field.isConstant(): Boolean = type.isPrimitive || type.name == "java.lang.String"

    // Generation

    fun generateTemplate(clazz: Class<*>): JsonObject {
        return buildJsonObject {
            putJsonObject("class") {
                // deprecated annotation is not available at runtime

                put("type", when {
                    clazz.isAnnotation -> "annotation"
                    clazz.isEnum -> "enum"
                    clazz.isInterface -> "interface"
                    clazz.isRecord -> "record"
                    else -> "class"
                })

                if (clazz.isInterface) {
                    put("implements", clazz.interfaces.map { it.name })
                } else if (clazz.superclass?.name != "java.lang.Object") {
                    put("extends", clazz.superclass.name)
                }

                // Generic Information is not available at runtime

                putAndCheck("visibility", visibility(clazz.modifiers))
                put("mods", mods(clazz.modifiers))
                put("comment", "TODO")
            }

            if (clazz.isEnum)
                putJsonArray("enumerations") {
                    for (enum in clazz.enumConstants)
                        addJsonObject {
                            put("name", enum.toString())
                            put("comment", "TODO")
                        }
                }

            if (!clazz.declaredFields.isNullOrEmpty())
                putJsonObject("fields") {
                    for (field in clazz.declaredFields)
                        putJsonObject(field.name) {
                            put("type", type(field.type))
                            putAndCheck("visibility", visibility(field.modifiers))
                            put("mods", mods(field.modifiers))
                            if (Modifier.isStatic(field.modifiers) && Modifier.isFinal(field.modifiers)) {
                                field.isAccessible = true
                            }

                            put("comment", "TODO")
                        }

                }

            if (!clazz.declaredConstructors.isNullOrEmpty())
                putJsonArray("constructors") {
                    for (constructor in clazz.declaredConstructors)
                        addJsonObject {
                            putAndCheck("visibility", visibility(constructor.modifiers))
                            putAndCheck("params", buildJsonArray {
                                for (param in constructor.parameters)
                                    addJsonObject {
                                        put("type", type(param.type))
                                        put("name", param.name)
                                        put("comment", "TODO")
                                    }
                            })
                            put("comment", "TODO")
                        }
                }

            if (!clazz.declaredMethods.isNullOrEmpty())
                putJsonObject("methods") {
                    val methods = mutableMapOf<String, MutableList<JsonObject>>()

                    for (method in clazz.declaredMethods.sortedBy { it.name }) {
                        val obj = buildJsonObject {
                            putAndCheck("visibility", visibility(method.modifiers))
                            put("mods", mods(method.modifiers))
                            putAndCheck("params", buildJsonArray {
                                for (param in method.parameters)
                                    addJsonObject {
                                        put("type", type(param.type))
                                        put("name", param.name)
                                        put("comment", "TODO")
                                    }
                            })

                            if (method.returnType != Void.TYPE)
                                put("return", buildJsonObject {
                                    put("type", type(method.returnType))
                                    put("comment", "TODO")
                                })

                            if (method.exceptionTypes.isNotEmpty())
                                put("throws", buildJsonArray {
                                    for (exception in method.exceptionTypes)
                                        addJsonObject {
                                            put("type", type(exception))
                                            put("comment", "TODO")
                                        }
                                })

                            put("comment", "TODO")
                        }

                        if (methods.containsKey(method.name))
                            methods[method.name]?.add(obj)
                        else
                            methods[method.name] = mutableListOf(obj)
                    }

                    for ((name, overloads) in methods)
                        if (overloads.size == 1)
                            put(name, overloads.first())
                        else
                            putJsonArray(name) {
                                for (overload in overloads)
                                    add(overload)
                            }
                }
        }
    }

}