package us.teaminceptus.inceptusnms.generation

import java.io.File

object Util {

    fun getJavaPackage(file: File): String {
        assert(file.isFile) { "File is not a file!" }
        assert(file.exists()) { "File does not exist!" }

        val path = file.absolutePath
        val filePath = path.split("docs")[1].substring(1).replace("/", ".")

        return filePath.substring(0, filePath.length - (file.name.length + 1))
    }

    fun getJavaPackages(dir: File): Set<String> {
        assert(dir.isDirectory) { "File is not a directory!" }
        assert(dir.exists()) { "File does not exist!" }

        val packages = mutableSetOf<String>()

        dir.walk().forEach {
            if (it.isFile && it.name.endsWith(".json")) {
                packages.add(getJavaPackage(it))
            }
        }

        return packages
    }

    fun getJavaName(file: File): String {
        assert(file.isFile) { "File is not a file!" }
        assert(file.exists()) { "File does not exist!" }

        val pkg = getJavaPackage(file)
        val name = file.name.split(".json")[0]

        return "$pkg.$name"
    }

}