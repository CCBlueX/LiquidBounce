/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.RecordComponentVisitor
import java.io.File
import java.util.jar.JarFile

abstract class CompareJsonKeysTask : DefaultTask() {

    /**
     * Baseline file
     */
    @get:InputFile
    abstract val baselineFile: RegularFileProperty

    /**
     * Files to check
     */
    @get:InputFiles
    abstract val files: ConfigurableFileCollection

    /**
     * Logger output limitation of missing keys
     */
    @get:Input
    abstract val consoleOutputCount: Property<Int>

    init {
        consoleOutputCount.convention(Int.MAX_VALUE)
    }

    @TaskAction
    fun run() {
        val baselineFile = baselineFile.orNull?.asFile

        if (baselineFile == null || !baselineFile.exists()) {
            throw GradleException("Baseline file $baselineFile not found")
        }

        @Suppress("UNCHECKED_CAST")
        fun File.readJsonObject() = inputStream().use(JsonSlurper()::parse) as Map<String, String>

        val baseline = baselineFile.readJsonObject()

        val outputCount = consoleOutputCount.get().coerceAtLeast(1)

        for (file in files.files) {
            if (file == baselineFile) {
                continue
            }

            val currentFile = file.readJsonObject()

            val missingKeys = baseline.keys - currentFile.keys

            if (missingKeys.isEmpty()) {
                logger.info("${file.name} is complete. No missing keys.")
            } else {
                val output = missingKeys.joinToString(
                    separator = ", ",
                    limit = outputCount,
                    truncated = "..."
                )
                logger.warn("${file.name} is missing the following keys (${missingKeys.size}): $output")
            }
        }
    }

}

/**
 * Converts a Fabric access widener (v1, official/Mojang names) into a NeoForge
 * access transformer (accesstransformer.cfg).
 *
 * The access widener remains the single source of truth; the NeoForge module
 * generates its access transformer from it at build time.
 *
 * Mapping:
 * - `accessible class a/b/C`        -> `public a.b.C`
 * - `accessible field C name desc`  -> `public a.b.C name`
 * - `accessible method C name desc` -> `public a.b.C name(desc)`
 * - `mutable field C name desc`     -> `public-f a.b.C name`
 *
 * `mutable` has no exact equivalent in access transformers, which always pair
 * definalization with an access level; `public-f` is a strict superset of the
 * Fabric behavior.
 *
 * Unlike access wideners, which Fabric applies to class files, access
 * transformers are applied to the decompiled Minecraft sources before they are
 * recompiled. javac rejects overrides of a widened method that keep the
 * original, now weaker, access level, so method widenings are propagated to
 * all overrides found in [hierarchyClasspath].
 *
 * Records need special handling for the same reason:
 * - Their component fields only exist in class files, not in source, so field
 *   widenings targeting them are dropped. Nothing is lost: record classes
 *   already expose a public accessor per component.
 * - A widened record class declares its canonical constructor with the
 *   original, now weaker, access level, so the constructor is widened along
 *   with the class.
 */
abstract class ConvertAccessWidenerTask : DefaultTask() {

    @get:InputFile
    abstract val accessWidener: RegularFileProperty

    /**
     * Jars to scan for overrides of widened methods. Class and member names
     * must use the same mappings as the access widener.
     */
    @get:Classpath
    abstract val hierarchyClasspath: ConfigurableFileCollection

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun run() {
        val file = accessWidener.get().asFile
        val lines = file.readLines()

        val header = lines.firstOrNull()?.split(WHITESPACE).orEmpty()
        if (header.size < 3 || header[0] != "accessWidener" || header[1] != "v1" || header[2] != "official") {
            throw GradleException("Unsupported access widener header in $file: ${lines.firstOrNull()}")
        }

        // Entry (class + optional member) -> definalize flag, preserving first-seen order
        val entries = LinkedHashMap<String, Boolean>()
        // Internal class name -> argument descriptors of widened methods
        val widenedMethods = LinkedHashMap<String, MutableSet<MethodKey>>()
        // Internal class names of widened classes
        val widenedClasses = LinkedHashSet<String>()
        // Entry -> internal owner name and field name of widened fields
        val widenedFields = LinkedHashMap<String, Pair<String, String>>()

        for ((index, raw) in lines.withIndex().drop(1)) {
            val line = raw.substringBefore('#').trim()
            if (line.isEmpty()) {
                continue
            }

            fun fail(reason: String): Nothing =
                throw GradleException("$file:${index + 1}: $reason: '$raw'")

            val parts = line.split(WHITESPACE)
            val access = parts[0]
            val kind = parts.getOrNull(1) ?: fail("missing member kind")

            val entry = when (kind) {
                "class" -> {
                    if (parts.size != 3) fail("malformed class entry")
                    widenedClasses += parts[2]
                    parts[2].toBinaryName()
                }
                "field" -> {
                    if (parts.size != 5) fail("malformed field entry")
                    val entry = "${parts[2].toBinaryName()} ${parts[3]}"
                    widenedFields[entry] = parts[2] to parts[3]
                    entry
                }
                "method" -> {
                    if (parts.size != 5) fail("malformed method entry")
                    val (_, _, owner, name, desc) = parts
                    if (name != "<init>" && name != "<clinit>") {
                        widenedMethods.getOrPut(owner, ::LinkedHashSet) +=
                            MethodKey(name, desc.substring(0, desc.indexOf(')') + 1))
                    }
                    "${owner.toBinaryName()} $name$desc"
                }
                else -> fail("unsupported member kind '$kind'")
            }

            when (access) {
                "accessible" -> entries.merge(entry, false, Boolean::or)
                "mutable" -> {
                    if (kind != "field") fail("'mutable' is only valid for fields")
                    entries.merge(entry, true, Boolean::or)
                }
                else -> fail("unsupported access type '$access' (only accessible/mutable are handled)")
            }
        }

        val classes = scanHierarchyClasspath()

        val overrides = collectOverrides(widenedMethods, classes).filterNot { it in entries }

        val recordComponentFields = widenedFields.filterValues { (owner, fieldName) ->
            classes[owner]?.let { info -> info.recordComponents.any { it.name == fieldName } } == true
        }.keys

        for (entry in recordComponentFields) {
            if (entries[entry] == true) {
                throw GradleException(
                    "Cannot definalize record component field '$entry': access transformers " +
                        "are applied to sources, where record component fields do not exist"
                )
            }
        }

        val recordConstructors = widenedClasses.mapNotNull { owner ->
            val components = classes[owner]?.recordComponents?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            "${owner.toBinaryName()} <init>(${components.joinToString("") { it.desc }})V"
        }.filterNot { it in entries }

        val outputFile = output.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(buildString {
            appendLine("# Generated from ${file.name} - do not edit, edit the access widener instead.")
            for ((entry, definalize) in entries) {
                if (entry in recordComponentFields) {
                    // The accessible-only widening is redundant in source form: the
                    // component already has a public accessor.
                    appendLine("# omitted record component field: $entry")
                    continue
                }

                appendLine(if (definalize) "public-f $entry" else "public $entry")
            }
            if (recordConstructors.isNotEmpty()) {
                appendLine()
                appendLine("# Canonical constructors of the widened record classes above, which must not keep")
                appendLine("# a weaker access level than their class.")
                for (entry in recordConstructors) {
                    appendLine("public $entry")
                }
            }
            if (overrides.isNotEmpty()) {
                appendLine()
                appendLine("# Overrides of the widened methods above, which must not keep a weaker access level.")
                for (entry in overrides) {
                    appendLine("public $entry")
                }
            }
        })
    }

    /**
     * Reads the class hierarchy, member and record metadata of every class in
     * [hierarchyClasspath], keyed by internal class name.
     */
    private fun scanHierarchyClasspath(): Map<String, ClassInfo> {
        if (hierarchyClasspath.isEmpty) {
            throw GradleException(
                "hierarchyClasspath is required to resolve record classes and method overrides"
            )
        }

        val classes = HashMap<String, ClassInfo>()

        for (jar in hierarchyClasspath.files) {
            JarFile(jar).use { jarFile ->
                for (jarEntry in jarFile.entries()) {
                    if (!jarEntry.name.endsWith(".class")) {
                        continue
                    }

                    val info = jarFile.getInputStream(jarEntry).use { stream ->
                        ClassInfoCollector().also {
                            ClassReader(stream).accept(
                                it,
                                ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
                            )
                        }.toClassInfo()
                    }

                    classes[info.name] = info
                }
            }
        }

        return classes
    }

    /**
     * Returns the entries for all overrides of [widenedMethods] declared by their
     * subtypes in [classes], in the entry format used above. Matching ignores the
     * return type to include covariant overrides; compiler-generated
     * (bridge/synthetic) methods are skipped because they do not exist in source.
     */
    private fun collectOverrides(
        widenedMethods: Map<String, Set<MethodKey>>,
        classes: Map<String, ClassInfo>,
    ): List<String> {
        if (widenedMethods.isEmpty()) {
            return emptyList()
        }

        val subtypes = HashMap<String, MutableList<ClassInfo>>()

        for (info in classes.values) {
            for (parent in info.parents) {
                subtypes.getOrPut(parent, ::ArrayList) += info
            }
        }

        val overrides = sortedSetOf<String>()

        for ((owner, methods) in widenedMethods) {
            val queue = ArrayDeque(subtypes[owner].orEmpty())
            val visited = HashSet<String>()

            while (true) {
                val subtype = queue.removeFirstOrNull() ?: break
                if (!visited.add(subtype.name)) {
                    continue
                }
                queue += subtypes[subtype.name].orEmpty()

                subtype.methods
                    .filter { method -> methods.any { it.matches(method) } }
                    .mapTo(overrides) { "${subtype.name.toBinaryName()} ${it.name}${it.desc}" }
            }
        }

        return overrides.toList()
    }

    private data class MethodKey(val name: String, val args: String) {
        fun matches(method: MethodInfo) = method.name == name && method.desc.startsWith(args)
    }

    private class MethodInfo(val name: String, val desc: String)

    private class RecordComponentInfo(val name: String, val desc: String)

    private class ClassInfo(
        val name: String,
        val parents: List<String>,
        val methods: List<MethodInfo>,
        val recordComponents: List<RecordComponentInfo>,
    )

    private class ClassInfoCollector : ClassVisitor(Opcodes.ASM9) {

        private lateinit var name: String
        private val parents = ArrayList<String>()
        private val methods = ArrayList<MethodInfo>()
        private val recordComponents = ArrayList<RecordComponentInfo>()

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<String>?,
        ) {
            this.name = name
            superName?.let(parents::add)
            interfaces?.let(parents::addAll)
        }

        override fun visitRecordComponent(
            name: String,
            descriptor: String,
            signature: String?,
        ): RecordComponentVisitor? {
            recordComponents += RecordComponentInfo(name, descriptor)
            return null
        }

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<String>?,
        ): MethodVisitor? {
            if (access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PRIVATE or Opcodes.ACC_BRIDGE or Opcodes.ACC_SYNTHETIC) == 0) {
                methods += MethodInfo(name, descriptor)
            }
            return null
        }

        fun toClassInfo() = ClassInfo(name, parents, methods, recordComponents)

    }

    private fun String.toBinaryName() = replace('/', '.')

    private companion object {
        private val WHITESPACE = Regex("\\s+")
    }

}

/**
 * Verifies that the loader-agnostic sources (src/main) do not import loader-specific
 * packages. This keeps the Fabric/NeoForge source split honest without requiring a
 * NeoForge build for fast feedback.
 */
abstract class CheckLoaderPurityTask : DefaultTask() {

    @get:InputFiles
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val forbiddenPackages: ListProperty<String>

    @TaskAction
    fun run() {
        val forbidden = forbiddenPackages.get()
        val violations = mutableListOf<String>()

        for (file in sources.asFileTree) {
            if (file.extension != "kt" && file.extension != "java") {
                continue
            }

            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    val import = when {
                        trimmed.startsWith("import static ") -> trimmed.removePrefix("import static ")
                        trimmed.startsWith("import ") -> trimmed.removePrefix("import ")
                        else -> return@forEachIndexed
                    }

                    if (forbidden.any { import.startsWith(it) }) {
                        violations += "${file.path}:${index + 1}: $trimmed"
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Loader-specific imports found in loader-agnostic sources " +
                    "(move the code to a loader source set or behind the Platform interface):\n" +
                    violations.joinToString("\n")
            )
        }
    }

}
