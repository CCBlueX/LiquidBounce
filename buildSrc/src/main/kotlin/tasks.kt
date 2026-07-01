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
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
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
 *
 * This is a heuristic: it only checks import statements, so fully qualified
 * references without an import pass. Compiling the NeoForge module remains the
 * authoritative check.
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

/**
 * Statically verifies that the shared Sponge/MixinExtras injectors still line up with
 * the shape of the NeoForge-patched Minecraft classes they target.
 *
 * NeoForge routinely reshapes vanilla methods (adds an overload, changes a parameter
 * list, renames a member). When that happens a mixin injector still compiles - the
 * handler only references the mod's own types - but at load time the Mixin subsystem
 * fails to bind it and throws `InvalidInjectionException`, which historically only
 * surfaced as an in-world crash. This task catches that class of divergence at build
 * time by resolving every injector's `method`/`@At` selectors against the actual
 * patched bytecode and modelling the argument-capture conventions.
 *
 * It is deliberately a heuristic (no full Mixin resolver): it aims for zero false
 * BLOCKERs on a healthy tree while reliably catching the real failure modes we hit,
 * most notably the ModelBlockRenderer `shouldRenderFace` overload divergence. Softer,
 * advisory concerns are reported as SUSPECT and only fail the build under [strict].
 */
abstract class MixinDivergenceCheckTask : DefaultTask() {

    /**
     * Compiled class tree of the shared mixins. The whole compile output may be
     * passed; the task filters to the `injection/mixins/minecraft/` package itself.
     */
    @get:InputFiles
    abstract val mixinClasses: ConfigurableFileCollection

    /** NeoForge-patched Minecraft jar(s) to resolve the mixin targets against. */
    @get:Classpath
    abstract val targetClasses: ConfigurableFileCollection

    /** When true, SUSPECT findings also fail the build (default: BLOCKER only). */
    @get:Input
    abstract val strict: Property<Boolean>

    init {
        strict.convention(false)
    }

    private enum class Severity { BLOCKER, SUSPECT }

    private class Finding(
        val severity: Severity,
        val mixinClass: String,
        val handler: String,
        val target: String,
        val explanation: String,
    )

    @TaskAction
    fun run() {
        val mixinFiles = mixinClasses.asFileTree.files.filter { file ->
            file.extension == "class" &&
                file.path.replace(File.separatorChar, '/')
                    .contains("net/ccbluex/liquidbounce/injection/mixins/minecraft/")
        }

        if (mixinFiles.isEmpty()) {
            throw GradleException(
                "No compiled shared mixins found under injection/mixins/minecraft/ in " +
                    "${mixinClasses.files}. Did :compileJava run?"
            )
        }

        if (targetClasses.isEmpty || targetClasses.files.none { it.exists() }) {
            throw GradleException(
                "NeoForge-patched Minecraft jar not found (looked at ${targetClasses.files}). " +
                    "It is produced by ModDevGradle; run the neoforge setup / :compileJava first."
            )
        }

        val mixins = mixinFiles.map { file ->
            file.inputStream().use { stream ->
                ClassNode().also { ClassReader(stream).accept(it, ClassReader.SKIP_FRAMES) }
            }
        }.map(::parseMixin).filter { it.injectors.isNotEmpty() }

        // Only load the target classes actually referenced by a mixin, for speed.
        val referenced = mixins.flatMap { it.targets }.toSet()
        val targets = loadTargets(referenced)

        val findings = ArrayList<Finding>()
        for (mixin in mixins) {
            checkMixin(mixin, targets, findings)
        }

        report(findings)
    }

    // region parsing --------------------------------------------------------

    private class InjectorInfo(
        /** Simple annotation name, e.g. `Inject`, `ModifyReturnValue`. */
        val kind: String,
        val handlerName: String,
        val handlerDesc: String,
        /** `method` selectors, e.g. `["foo", "bar(Lx;)V"]`. */
        val selectors: List<String>,
        /** `@At.target` reference strings gathered from the injector. */
        val atTargets: List<String>,
        /** Handler parameter types (ASM). */
        val paramTypes: List<Type>,
        /** Indices of params carrying a MixinExtras sugar / @Coerce annotation. */
        val sugarParams: Set<Int>,
        /** Name-only `@Local` params (no ordinal/index): index -> declared name. */
        val nameOnlyLocals: Map<Int, String>,
    )

    private class MixinInfo(
        val name: String,
        /** Internal names of the `@Mixin(value = …)` class targets. */
        val targets: List<String>,
        val injectors: List<InjectorInfo>,
    )

    private companion object {
        private const val MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;"

        // Injector annotation descriptor -> simple name.
        private val INJECTOR_ANNOTATIONS = mapOf(
            "Lorg/spongepowered/asm/mixin/injection/Inject;" to "Inject",
            "Lorg/spongepowered/asm/mixin/injection/Redirect;" to "Redirect",
            "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;" to "ModifyVariable",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArg;" to "ModifyArg",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;" to "ModifyArgs",
            "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;" to "ModifyConstant",
            "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;" to "ModifyExpressionValue",
            "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;" to "ModifyReturnValue",
            "Lcom/llamalad7/mixinextras/injector/ModifyReceiver;" to "ModifyReceiver",
            "Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;" to "WrapWithCondition",
            "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;" to "WrapOperation",
            "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;" to "WrapMethod",
        )

        // Injectors whose leading params must be a PREFIX of the target args.
        private val PREFIX_CAPTURE = setOf("Inject")

        // Injectors whose trailing params (after the modified value) must be a
        // SUFFIX of the target args. @ModifyVariable is deliberately excluded: its
        // capture convention (argsOnly/ordinal/index/prefix semantics) is too fiddly
        // to model reliably, and it is not the failure mode this check targets.
        private val SUFFIX_CAPTURE = setOf("ModifyReturnValue", "ModifyExpressionValue")

        // Sugar / coercion annotations: an annotated param is a captured local, not a
        // target arg, so it is excluded from the prefix/suffix compatibility check.
        private val SUGAR_ANNOTATIONS = setOf(
            "Lcom/llamalad7/mixinextras/sugar/Local;",
            "Lcom/llamalad7/mixinextras/sugar/Share;",
            "Lcom/llamalad7/mixinextras/sugar/Cancellable;",
            "Lorg/spongepowered/asm/mixin/injection/Coerce;",
        )
        private const val LOCAL_DESC = "Lcom/llamalad7/mixinextras/sugar/Local;"

        // Callback types that Mixin appends/allows outside the captured target args
        // (trailing for @Inject, and a @Cancellable CallbackInfo tail for MixinExtras).
        private val CALLBACK_TYPES = setOf(
            "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;",
            "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;",
        )
    }

    private fun parseMixin(node: ClassNode): MixinInfo {
        val targets = (node.invisibleAnnotations.orEmpty() + node.visibleAnnotations.orEmpty())
            .firstOrNull { it.desc == MIXIN_DESC }
            ?.let(::readMixinTargets)
            .orEmpty()

        val injectors = node.methods.mapNotNull { method -> parseInjector(method) }

        return MixinInfo(node.name, targets, injectors)
    }

    /** Reads the `value` (class targets) of a `@Mixin` annotation. String `targets` are skipped. */
    private fun readMixinTargets(annotation: AnnotationNode): List<String> {
        val value = annotation.values ?: return emptyList()
        val result = ArrayList<String>()
        var i = 0
        while (i < value.size) {
            val key = value[i] as String
            val v = value[i + 1]
            if (key == "value") {
                // Either a single Type or a List<Type> for value = {A.class, B.class}.
                when (v) {
                    is Type -> result += v.internalName
                    is List<*> -> v.filterIsInstance<Type>().mapTo(result) { it.internalName }
                }
            }
            i += 2
        }
        return result
    }

    private fun parseInjector(method: MethodNode): InjectorInfo? {
        // Sponge/MixinExtras injector annotations have RUNTIME retention (visible),
        // whereas @Mixin itself is CLASS-retention (invisible); check both lists.
        val annotation = (method.visibleAnnotations.orEmpty() + method.invisibleAnnotations.orEmpty())
            .firstOrNull { it.desc in INJECTOR_ANNOTATIONS } ?: return null
        val kind = INJECTOR_ANNOTATIONS.getValue(annotation.desc)

        val selectors = ArrayList<String>()
        val atTargets = ArrayList<String>()

        val values = annotation.values.orEmpty()
        var i = 0
        while (i < values.size) {
            val key = values[i] as String
            val v = values[i + 1]
            when (key) {
                "method" -> collectStrings(v, selectors)
                "at" -> collectAtTargets(v, atTargets)
                // slice(from/to) can carry @At too, but is rarely the divergence source.
                "slice" -> collectAtTargets(v, atTargets)
            }
            i += 2
        }

        val paramTypes = Type.getArgumentTypes(method.desc).toList()
        val sugarParams = HashSet<Int>()
        val nameOnlyLocals = HashMap<Int, String>()
        // @Local is CLASS-retention (invisible), but merge both lists defensively.
        val paramCount = paramTypes.size
        for (index in 0 until paramCount) {
            val paramAnnotations = method.invisibleParameterAnnotations?.getOrNull(index).orEmpty() +
                method.visibleParameterAnnotations?.getOrNull(index).orEmpty()
            for (paramAnnotation in paramAnnotations) {
                if (paramAnnotation.desc in SUGAR_ANNOTATIONS) {
                    sugarParams += index
                }
                if (paramAnnotation.desc == LOCAL_DESC && isNameOnlyLocal(paramAnnotation)) {
                    readLocalName(paramAnnotation)?.let { nameOnlyLocals[index] = it }
                }
            }
        }

        return InjectorInfo(
            kind = kind,
            handlerName = method.name,
            handlerDesc = method.desc,
            selectors = selectors,
            atTargets = atTargets,
            paramTypes = paramTypes,
            sugarParams = sugarParams,
            nameOnlyLocals = nameOnlyLocals,
        )
    }

    /** True when a `@Local` provides neither `ordinal` nor `index` (name-only resolution). */
    private fun isNameOnlyLocal(annotation: AnnotationNode): Boolean {
        val values = annotation.values ?: return true
        var i = 0
        while (i < values.size) {
            val key = values[i] as String
            if (key == "ordinal" || key == "index") {
                return false
            }
            i += 2
        }
        return true
    }

    private fun readLocalName(annotation: AnnotationNode): String? {
        val values = annotation.values ?: return null
        var i = 0
        while (i < values.size) {
            if (values[i] == "name") {
                return when (val v = values[i + 1]) {
                    is String -> v
                    is List<*> -> v.filterIsInstance<String>().firstOrNull()
                    else -> null
                }
            }
            i += 2
        }
        return null
    }

    private fun collectStrings(value: Any?, into: MutableList<String>) {
        when (value) {
            is String -> into += value
            is List<*> -> value.filterIsInstance<String>().forEach { into += it }
        }
    }

    /** Extracts `target` strings from an `@At` (single or array) or a `@Slice`. */
    private fun collectAtTargets(value: Any?, into: MutableList<String>) {
        when (value) {
            is AnnotationNode -> {
                val vals = value.values.orEmpty()
                var i = 0
                while (i < vals.size) {
                    val key = vals[i] as String
                    val v = vals[i + 1]
                    when (key) {
                        "target" -> collectStrings(v, into)
                        // @Slice carries from/to that are themselves @At annotations.
                        "from", "to" -> collectAtTargets(v, into)
                    }
                    i += 2
                }
            }
            is List<*> -> value.forEach { collectAtTargets(it, into) }
        }
    }

    // endregion

    // region target loading -------------------------------------------------

    private fun loadTargets(referenced: Set<String>): Map<String, ClassNode> {
        val classes = HashMap<String, ClassNode>()
        for (jar in targetClasses.files) {
            if (!jar.exists()) {
                continue
            }
            JarFile(jar).use { jarFile ->
                for (entry in jarFile.entries()) {
                    if (!entry.name.endsWith(".class")) {
                        continue
                    }
                    val internal = entry.name.removeSuffix(".class")
                    if (internal !in referenced || internal in classes) {
                        continue
                    }
                    jarFile.getInputStream(entry).use { stream ->
                        classes[internal] = ClassNode().also {
                            // Method bodies are needed for the @At reference scan.
                            ClassReader(stream).accept(it, ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
                        }
                    }
                }
            }
        }
        return classes
    }

    // endregion

    // region checking -------------------------------------------------------

    private fun checkMixin(mixin: MixinInfo, targets: Map<String, ClassNode>, findings: MutableList<Finding>) {
        for (injector in mixin.injectors) {
            // Advisory: name-only @Local resolves against stripped local names on the
            // patched jar and only binds when its type is unique in scope.
            for ((index, localName) in injector.nameOnlyLocals) {
                val type = injector.paramTypes.getOrNull(index)?.className ?: "?"
                findings += Finding(
                    Severity.SUSPECT, mixin.name, injector.handlerName,
                    injector.selectors.joinToString(","),
                    "@Local(name=\"$localName\") param (type $type) has no ordinal/index; local " +
                        "variable names are stripped on the patched jar, so it only binds if that " +
                        "type is unique in scope. Prefer ordinal/index or confirm uniqueness.",
                )
            }

            for (selector in injector.selectors) {
                for (targetInternal in mixin.targets) {
                    val targetClass = targets[targetInternal] ?: continue
                    checkSelector(mixin, injector, selector, targetInternal, targetClass, findings)
                }
            }
        }
    }

    private fun checkSelector(
        mixin: MixinInfo,
        injector: InjectorInfo,
        selector: String,
        targetInternal: String,
        targetClass: ClassNode,
        findings: MutableList<Finding>,
    ) {
        val (name, desc) = splitSelector(selector)
        // Mixin selectors may use `*`/`?` wildcards on the name; match with a glob.
        val isGlob = '*' in name || '?' in name
        val nameRegex = if (isGlob) globToRegex(name) else null
        val matches = targetClass.methods.filter { method ->
            val nameMatch = if (nameRegex != null) nameRegex.matches(method.name) else method.name == name
            nameMatch && (desc == null || method.desc == desc)
        }

        val where = "$targetInternal#$selector"

        if (matches.isEmpty()) {
            findings += Finding(
                Severity.BLOCKER, mixin.name, injector.handlerName, where,
                "selector resolves to zero methods in the patched target class. " +
                    "The vanilla method was renamed/reshaped or removed by NeoForge.",
            )
            return
        }

        // SUSPECT: a bare-name selector hitting more than one overload may double-inject
        // (e.g. NeoForge added an overload that vanilla didn't have). A glob selector
        // hitting many methods is intentional, so it is not flagged here.
        if (desc == null && !isGlob && matches.size > 1) {
            findings += Finding(
                Severity.SUSPECT, mixin.name, injector.handlerName, where,
                "bare-name selector matches ${matches.size} overloads " +
                    "(${matches.joinToString { it.name + it.desc }}); it will inject into all of them. " +
                    "Qualify the selector if only one is intended.",
            )
        }

        // BLOCKER: @At member reference must exist in the body of at least one match.
        for (atTarget in injector.atTargets) {
            val ref = parseMemberRef(atTarget) ?: continue
            val present = matches.any { method -> bodyReferences(method, ref) }
            if (!present) {
                findings += Finding(
                    Severity.BLOCKER, mixin.name, injector.handlerName, where,
                    "@At target \"$atTarget\" references ${ref.owner}#${ref.member}, which does not " +
                        "appear in any matched overload's body. The call/field site was moved or " +
                        "removed by NeoForge.",
                )
            }
        }

        // BLOCKER: captured-arg incompatibility (the ModelBlockRenderer failure mode).
        // Checked against EACH overload: if incompatible with ANY, Mixin throws.
        // Skipped for glob selectors, where the intended overload set is ambiguous.
        if (!isGlob && (injector.kind in PREFIX_CAPTURE || injector.kind in SUFFIX_CAPTURE)) {
            val captured = capturedParams(injector)
            for (match in matches) {
                val targetArgs = Type.getArgumentTypes(match.desc).toList()
                val compatible = if (injector.kind in PREFIX_CAPTURE) {
                    isPrefix(captured, targetArgs)
                } else {
                    isSuffix(captured, targetArgs)
                }
                if (!compatible) {
                    val relation = if (injector.kind in PREFIX_CAPTURE) "prefix" else "suffix"
                    findings += Finding(
                        Severity.BLOCKER, mixin.name, injector.handlerName, where,
                        "@${injector.kind} captured args ${captured.map { it.className }} are not a " +
                            "$relation of matched overload ${match.name}${match.desc} " +
                            "(target args ${targetArgs.map { it.className }}). This is what triggers " +
                            "InvalidInjectionException at load time.",
                    )
                }
            }
        }
    }

    /**
     * The target arguments the handler captures, with sugar/callback params removed.
     *
     * - `@Inject`: strip the trailing CallbackInfo/CIR and any sugar params; the rest is
     *   the captured PREFIX of the target args.
     * - `@ModifyReturnValue`/`@ModifyExpressionValue`/`@ModifyVariable`: the first param is
     *   the modified value; the remaining non-sugar params are the captured SUFFIX.
     */
    private fun capturedParams(injector: InjectorInfo): List<Type> {
        val params = injector.paramTypes
        return if (injector.kind in PREFIX_CAPTURE) {
            params.filterIndexed { index, type ->
                index !in injector.sugarParams && type.descriptor !in CALLBACK_TYPES
            }
        } else {
            // Drop the leading modified value; then drop sugar params and any trailing
            // CallbackInfo/CIR (@Cancellable) - none of these are captured target args.
            params.filterIndexed { index, type ->
                index != 0 && index !in injector.sugarParams && type.descriptor !in CALLBACK_TYPES
            }
        }
    }

    private fun isPrefix(captured: List<Type>, target: List<Type>): Boolean =
        captured.size <= target.size && captured.indices.all { captured[it] == target[it] }

    private fun isSuffix(captured: List<Type>, target: List<Type>): Boolean {
        if (captured.size > target.size) {
            return false
        }
        val offset = target.size - captured.size
        return captured.indices.all { captured[it] == target[offset + it] }
    }

    private fun report(findings: List<Finding>) {
        val blockers = findings.filter { it.severity == Severity.BLOCKER }
        val suspects = findings.filter { it.severity == Severity.SUSPECT }

        for (finding in findings) {
            val line = "[${finding.severity}] ${finding.mixinClass}#${finding.handler} -> " +
                "${finding.target}: ${finding.explanation}"
            if (finding.severity == Severity.BLOCKER) {
                logger.error(line)
            } else {
                logger.warn(line)
            }
        }

        val summary = "Mixin divergence check: ${blockers.size} blocker(s), ${suspects.size} suspect(s)."
        if (blockers.isNotEmpty() || (strict.get() && suspects.isNotEmpty())) {
            throw GradleException(
                "$summary\n" +
                    "The shared mixins have diverged from the NeoForge-patched Minecraft classes. " +
                    "Each finding above would break mixin application at load time.\n" +
                    (blockers + if (strict.get()) suspects else emptyList()).joinToString("\n") {
                        "  [${it.severity}] ${it.mixinClass}#${it.handler} -> ${it.target}: ${it.explanation}"
                    }
            )
        }
        logger.lifecycle(summary + if (suspects.isNotEmpty()) " (suspects are advisory; passing)" else " Passed.")
    }

    // endregion

    // region selector / reference helpers -----------------------------------

    /**
     * Splits `"name"` or `"name(desc)ret"` into name + optional full descriptor.
     * Handles an optional owner qualifier before the name (`Lowner;name…` or
     * `owner.name…`), without corrupting a descriptor that itself contains `;`.
     */
    private fun splitSelector(selector: String): Pair<String, String?> {
        val s = selector.trim()
        val paren = s.indexOf('(')
        val namePart = if (paren < 0) s else s.substring(0, paren)
        val descPart = if (paren < 0) null else s.substring(paren)

        // The name is everything after an owner qualifier. An owner is written as a
        // field-descriptor prefix `Lpkg/Owner;` or a dotted `pkg.Owner.`; the method
        // name is the segment after the last such separator in the name part only.
        val name = when {
            namePart.contains(';') -> namePart.substringAfterLast(';')
            namePart.contains('.') -> namePart.substringAfterLast('.')
            else -> namePart
        }
        return name to descPart
    }

    /** Converts a Mixin name glob (`*` / `?`) to an anchored regex. */
    private fun globToRegex(glob: String): Regex {
        val pattern = buildString {
            for (ch in glob) {
                when (ch) {
                    '*' -> append(".*")
                    '?' -> append('.')
                    else -> append(Regex.escape(ch.toString()))
                }
            }
        }
        return Regex(pattern)
    }

    private class MemberRef(val owner: String, val member: String)

    /**
     * Parses an `@At` target string like `Lowner;name(desc)ret` or `Lowner;field:desc`
     * into owner + member name. The descriptor tail is intentionally ignored to keep
     * matching robust across trivial descriptor churn.
     */
    private fun parseMemberRef(target: String): MemberRef? {
        val t = target.trim()
        if (!t.startsWith("L")) {
            return null
        }
        val semi = t.indexOf(';')
        if (semi < 0) {
            return null
        }
        val owner = t.substring(1, semi)
        val rest = t.substring(semi + 1)
        val member = rest.substringBefore('(').substringBefore(':').trim()
        if (member.isEmpty()) {
            return null
        }
        return MemberRef(owner, member)
    }

    /** True if [method]'s body contains a method/field/NEW insn matching [ref] by owner+name. */
    private fun bodyReferences(method: MethodNode, ref: MemberRef): Boolean {
        for (insn in method.instructions) {
            val matches = when (insn) {
                is MethodInsnNode -> insn.owner == ref.owner && insn.name == ref.member
                is FieldInsnNode -> insn.owner == ref.owner && insn.name == ref.member
                // NEW targets carry only the owner type; the member is the type name.
                is TypeInsnNode -> insn.desc == ref.owner || insn.desc == ref.member
                else -> false
            }
            if (matches) {
                return true
            }
        }
        return false
    }

    // endregion

}
