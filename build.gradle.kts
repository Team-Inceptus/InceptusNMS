import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import java.io.IOException
import java.nio.charset.StandardCharsets

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    java
    `java-library`
}

val jvmVersion = JavaVersion.VERSION_17

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "java")
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        implementation("org.jsoup:jsoup:1.16.2")
        implementation("org.jetbrains:markdown:0.5.2")
    }

    java {
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
    }
}

group = "us.teaminceptus.inceptusnms"
version = "0.1.1"

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = jvmVersion.toString()
    }

    clean {
        try {
            File("generated").deleteRecursively()
        } catch (e: IOException) { // Running Test Server
            File("generated").deleteDirectoryContents()
        }
    }

    register("generate", JavaExec::class.java) {
        dependsOn("validate")

        mainClass.set("us.teaminceptus.inceptusnms.generation.Main")
        classpath = sourceSets["main"].runtimeClasspath
        args = listOf(
            file("docs").absolutePath,
            file("generated").absolutePath
        )
    }

    register("validate", JavaExec::class.java) {
        mainClass.set("us.teaminceptus.inceptusnms.validation.Main")
        classpath = sourceSets["main"].runtimeClasspath
        args = listOf(
            file("docs").absolutePath
        )
    }
}