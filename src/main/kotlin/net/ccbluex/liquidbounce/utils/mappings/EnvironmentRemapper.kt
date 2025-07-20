package net.ccbluex.liquidbounce.utils.mappings

import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.resource
import net.fabricmc.mappings.model.V2MappingsProvider

object EnvironmentRemapper {

    private val mappings = runCatching {
        resource("/mappings/mappings.tiny").bufferedReader().use {
            V2MappingsProvider.readTinyMappings(it)
        }
    }.onFailure {
        logger.error("Unable to load mappings. Ignore this if you are using a development environment.", it)
    }.getOrNull()

    private val environment = runCatching {
        probeEnvironment()
    }.onFailure {
        logger.error("Unable to probe environment. Please make sure you are using a valid environment.", it)
    }.getOrNull()

    private fun probeEnvironment(): String? {
        val mappings = mappings ?: return null

        val minecraftClassEntry = mappings.classEntries?.find { entry ->
            entry?.get("named") == "net/minecraft/client/MinecraftClient"
        }

        if (minecraftClassEntry == null) {
            logger.error("Unable to probe environment. Please make sure you are using a valid environment.")
            return null
        }

        logger.info("Probing environment...")
        return when {
            isClassPresent(minecraftClassEntry.get("intermediary")?.toDotNotation()) -> {
                logger.info("Intermediary environment detected.")
                "intermediary"
            }
            else -> {
                logger.error("No matching environment detected. Please make sure you are using a valid environment.")
                null
            }
        }
    }

    private fun isClassPresent(className: String?): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    fun remapClassName(clazz: String): String {
        environment ?: return clazz

        val className = clazz.toSlashNotation()
        return mappings?.classEntries?.find {
            it?.get("named") == className
        }?.get(environment)?.toDotNotation() ?: clazz
    }

    fun remapClass(clazz: Class<*>): String {
        environment ?: return clazz.name

        val className = clazz.name.toSlashNotation()
        return mappings?.classEntries?.find {
            it?.get(environment) == className
        }?.get("named")?.toDotNotation() ?: clazz.name
    }

    fun remapField(clazz: Class<*>, name: String): String {
        environment ?: return name

        val clazzNames = getClassHierarchyNames(clazz)

        return mappings?.fieldEntries?.find { entry ->
            val intern = entry.get(environment)
            clazzNames.contains(intern.owner) && intern.name == name
        }?.get("named")?.name ?: name
    }

    fun remapField(clazz: String, name: String): String {
        environment ?: return name

        val className = clazz.toSlashNotation()
        return mappings?.fieldEntries?.find { entry ->
            val intern = entry.get(environment)
            className == intern.owner && intern.name == name
        }?.get("named")?.name ?: name
    }

    fun remapMethod(clazz: Class<*>, name: String): String {
        environment ?: return name

        val clazzNames = getClassHierarchyNames(clazz)

        return mappings?.methodEntries?.find { entry ->
            val intern = entry.get(environment)
            clazzNames.contains(intern.owner) && intern.name == name
        }?.get("named")?.name ?: name
    }

    private fun getClassHierarchyNames(clazz: Class<*>): Set<String> {
        val clazzNames = mutableSetOf(clazz.name.toSlashNotation())
        var current = clazz

        current.interfaces.forEach { interfaceClazz ->
            clazzNames.addAll(getClassHierarchyNames(interfaceClazz))
        }

        while (current.name != "java.lang.Object") {
            current = current.superclass ?: break
            clazzNames.addAll(getClassHierarchyNames(current))
        }

        return clazzNames
    }

    private fun String.toDotNotation(): String = replace('/', '.')

    private fun String.toSlashNotation(): String = replace('.', '/')

}
