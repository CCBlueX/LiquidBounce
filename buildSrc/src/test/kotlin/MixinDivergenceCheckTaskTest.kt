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
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Exercises [MixinDivergenceCheckTask] against hand-assembled synthetic classes,
 * proving the prefix/suffix capture incompatibility detection - the exact class of
 * bug (ModelBlockRenderer `shouldRenderFace` overload) the task exists to catch.
 */
class MixinDivergenceCheckTaskTest {

    private lateinit var dir: File

    @BeforeTest
    fun setUp() {
        dir = createTempDirectory("mixin-divergence-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `passes when the ModifyReturnValue suffix matches the target`() {
        // Target: forceOpaque(Z, LState;)Z. Handler captures [LState;] as a suffix -> OK.
        val target = classWriter("mc/Target") {
            visitMethod(
                Opcodes.ACC_PUBLIC, "forceOpaque",
                "(ZL${"mc/State"};)Z", null, null,
            ).visitEnd()
        }

        val mixin = mixinClass(
            name = "mixins/MixinTarget",
            target = "mc/Target",
            handlerName = "onForceOpaque",
            handlerDesc = "(ZL${"mc/State"};)Z",
            injectorDesc = "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",
            method = "forceOpaque",
        )

        // Should not throw.
        runTask(targets = mapOf("mc/Target" to target), mixins = mapOf("mixins/MixinTarget" to mixin))
    }

    @Test
    fun `blocks when the ModifyReturnValue suffix is incompatible with an overload`() {
        // Two overloads of shouldRenderFace, mirroring the NeoForge divergence:
        //   5-arg (Level, Pos, State, Direction, Pos) and 4-arg (Level, State, Direction, Pos).
        val target = classWriter("mc/Target") {
            visitMethod(
                Opcodes.ACC_PUBLIC, "shouldRenderFace",
                "(L${"mc/Level"};L${"mc/Pos"};L${"mc/State"};L${"mc/Direction"};L${"mc/Pos"};)Z",
                null, null,
            ).visitEnd()
            visitMethod(
                Opcodes.ACC_PUBLIC, "shouldRenderFace",
                "(L${"mc/Level"};L${"mc/State"};L${"mc/Direction"};L${"mc/Pos"};)Z",
                null, null,
            ).visitEnd()
        }

        // Handler captures the vanilla 4-arg shape as a suffix: matches the 4-arg
        // overload but NOT the 5-arg one -> BLOCKER (incompatible with ANY overload).
        val mixin = mixinClass(
            name = "mixins/MixinTarget",
            target = "mc/Target",
            handlerName = "probe",
            handlerDesc = "(ZL${"mc/Level"};L${"mc/State"};L${"mc/Direction"};L${"mc/Pos"};)Z",
            injectorDesc = "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",
            method = "shouldRenderFace",
        )

        val error = assertFailsWith<GradleException> {
            runTask(targets = mapOf("mc/Target" to target), mixins = mapOf("mixins/MixinTarget" to mixin))
        }
        assertTrue(error.message!!.contains("BLOCKER"), "expected a BLOCKER, got: ${error.message}")
        assertTrue(error.message!!.contains("suffix"), "expected a suffix explanation, got: ${error.message}")
    }

    @Test
    fun `blocks when the Inject prefix is incompatible with the target`() {
        // Target: run(I) - the handler captures [I, J] as a prefix, which is not a
        // prefix of [I] (J is extra) -> BLOCKER.
        val target = classWriter("mc/Target") {
            visitMethod(Opcodes.ACC_PUBLIC, "run", "(I)V", null, null).visitEnd()
        }

        val mixin = mixinClass(
            name = "mixins/MixinTarget",
            target = "mc/Target",
            handlerName = "onRun",
            // [int, long, CallbackInfo] -> captured prefix [int, long].
            handlerDesc = "(IJLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
            injectorDesc = "Lorg/spongepowered/asm/mixin/injection/Inject;",
            method = "run",
        )

        val error = assertFailsWith<GradleException> {
            runTask(targets = mapOf("mc/Target" to target), mixins = mapOf("mixins/MixinTarget" to mixin))
        }
        assertTrue(error.message!!.contains("prefix"), "expected a prefix explanation, got: ${error.message}")
    }

    @Test
    fun `blocks when the selector resolves to zero methods`() {
        val target = classWriter("mc/Target") {
            visitMethod(Opcodes.ACC_PUBLIC, "somethingElse", "()V", null, null).visitEnd()
        }

        val mixin = mixinClass(
            name = "mixins/MixinTarget",
            target = "mc/Target",
            handlerName = "onGone",
            handlerDesc = "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
            injectorDesc = "Lorg/spongepowered/asm/mixin/injection/Inject;",
            method = "renamedAway",
        )

        val error = assertFailsWith<GradleException> {
            runTask(targets = mapOf("mc/Target" to target), mixins = mapOf("mixins/MixinTarget" to mixin))
        }
        assertTrue(error.message!!.contains("zero methods"), "expected zero-methods, got: ${error.message}")
    }

    private fun runTask(targets: Map<String, ByteArray>, mixins: Map<String, ByteArray>) {
        val classesDir = File(dir, "classes/net/ccbluex/liquidbounce/injection/mixins/minecraft")
        classesDir.mkdirs()
        for ((name, bytes) in mixins) {
            File(classesDir, name.substringAfterLast('/') + ".class").writeBytes(bytes)
        }

        val jarFile = File(dir, "minecraft-patched.jar")
        JarOutputStream(jarFile.outputStream()).use { jar ->
            for ((name, bytes) in targets) {
                jar.putNextEntry(JarEntry("$name.class"))
                jar.write(bytes)
                jar.closeEntry()
            }
        }

        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        val task = project.tasks
            .register("checkMixinDivergence", MixinDivergenceCheckTask::class.java)
            .get()
        task.mixinClasses.from(File(dir, "classes"))
        task.targetClasses.from(jarFile)
        task.run()
    }

    /**
     * Builds a mixin class with a single injector-annotated handler method.
     * The `@Mixin(value = target.class)` and injector annotations are emitted as
     * invisible (class-retention) annotations, matching real compiled mixins.
     *
     * @param method the injector's `method` selector value.
     */
    private fun mixinClass(
        name: String,
        target: String,
        handlerName: String,
        handlerDesc: String,
        injectorDesc: String,
        method: String,
    ): ByteArray {
        val writer = ClassWriter(0)
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT, name, null, "java/lang/Object", null)

        // @Mixin(value = target.class)
        writer.visitAnnotation("Lorg/spongepowered/asm/mixin/Mixin;", false).apply {
            visit("value", Type.getObjectType(target))
            visitEnd()
        }

        val handler = writer.visitMethod(Opcodes.ACC_PRIVATE, handlerName, handlerDesc, null, null)

        // @Inject/@ModifyReturnValue(method = "…") as an invisible annotation.
        handler.visitAnnotation(injectorDesc, false).apply {
            visitArray("method").apply {
                visit(null, method)
                visitEnd()
            }
            visitEnd()
        }

        emitBody(handler, handlerDesc)

        writer.visitEnd()
        return writer.toByteArray()
    }

    private fun classWriter(name: String, members: ClassWriter.() -> Unit): ByteArray {
        val writer = ClassWriter(0)
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null)
        writer.members()
        writer.visitEnd()
        return writer.toByteArray()
    }

    private fun emitBody(method: MethodVisitor, desc: String) {
        method.visitCode()
        val returnType = Type.getReturnType(desc)
        when (returnType.sort) {
            Type.VOID -> method.visitInsn(Opcodes.RETURN)
            Type.BOOLEAN, Type.INT -> {
                method.visitInsn(Opcodes.ICONST_0)
                method.visitInsn(Opcodes.IRETURN)
            }
            else -> {
                method.visitInsn(Opcodes.ACONST_NULL)
                method.visitInsn(Opcodes.ARETURN)
            }
        }
        method.visitMaxs(1, Type.getArgumentTypes(desc).sumOf { it.size } + 1)
        method.visitEnd()
    }
}
