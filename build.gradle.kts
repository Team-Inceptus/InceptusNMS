import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import java.io.IOException
import java.nio.charset.StandardCharsets

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.20"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jsoup:jsoup:1.16.2")
    implementation("org.jetbrains:markdown:0.5.2")
}

group = "us.teaminceptus.inceptusnms"
version = "0.0.5"

val jvmVersion = JavaVersion.VERSION_17
java {
    sourceCompatibility = jvmVersion
    targetCompatibility = jvmVersion
}

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