repositories {
    maven("https://repo.codemc.io/repository/nms/")
}

dependencies {
    api(rootProject)
    runtimeOnly("org.spigotmc:spigot:1.20.2-R0.1-SNAPSHOT:remapped-mojang")
}

tasks {
    register("generateTemplate", JavaExec::class.java) {
        mainClass.set("us.teaminceptus.inceptusnms.template.Main")
        classpath = sourceSets["main"].runtimeClasspath
    }
}