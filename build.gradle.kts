import java.nio.charset.StandardCharsets

plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jsoup:jsoup:1.16.1")
}

group = "us.teaminceptus.inceptusnms"
version = file("version.txt").readText()

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
        delete("generated")
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