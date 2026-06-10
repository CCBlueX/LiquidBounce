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
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ConvertAccessWidenerTaskTest {

    private lateinit var dir: File

    @BeforeTest
    fun setUp() {
        dir = createTempDirectory("aw-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `converts the access widener entry kinds`() {
        val output = convert(
            """
            accessWidener v1 official
            accessible method test/Base update ()V
            accessible field test/Base speed F
            mutable field test/Base health F
            """
        )

        assertContains(output, "public test.Base update()V")
        assertContains(output, "public test.Base speed")
        assertContains(output, "public-f test.Base health")
    }

    @Test
    fun `merges accessible and mutable widenings of the same field`() {
        val output = convert(
            """
            accessWidener v1 official
            accessible field test/Base speed F
            mutable field test/Base speed F
            """
        )

        assertContains(output, "public-f test.Base speed")
        assertEquals(1, output.lines().count { "test.Base speed" in it })
    }

    @Test
    fun `propagates method widenings to overrides with weaker access`() {
        val output = convert(
            """
            accessWidener v1 official
            accessible method test/Base update ()V
            """
        )

        // Sub keeps the protected access level and is recompiled by NeoForge, so
        // javac requires the override to be widened along with the base method.
        assertContains(output, "public test.Sub update()V")
        // SubSub inherits through Sub and must be found transitively.
        assertContains(output, "public test.SubSub update()V")
        // Other already overrides publicly and needs no widening.
        assertFalse(output.lines().any { "test.Other" in it })
    }

    @Test
    fun `does not duplicate overrides already widened by the access widener`() {
        val output = convert(
            """
            accessWidener v1 official
            accessible method test/Base update ()V
            accessible method test/Sub update ()V
            """
        )

        assertEquals(1, output.lines().count { it == "public test.Sub update()V" })
    }

    @Test
    fun `omits record component fields and widens the canonical constructor`() {
        val output = convert(
            """
            accessWidener v1 official
            accessible class test/Rec
            accessible field test/Rec value I
            """
        )

        assertContains(output, "public test.Rec")
        assertContains(output, "public test.Rec <init>(I)V")
        assertContains(output, "# omitted record component field: test.Rec value")
        assertFalse(output.lines().any { it == "public test.Rec value" })
    }

    @Test
    fun `rejects mutable widenings of record component fields`() {
        assertFailsWith<GradleException> {
            convert(
                """
                accessWidener v1 official
                mutable field test/Rec value I
                """
            )
        }
    }

    @Test
    fun `rejects an unsupported header`() {
        assertFailsWith<GradleException> {
            convert("accessWidener v2 intermediary")
        }
    }

    private fun convert(accessWidener: String): String {
        val awFile = File(dir, "test.accesswidener")
        awFile.writeText(accessWidener.trimIndent())

        val jarFile = File(dir, "hierarchy.jar")
        JarOutputStream(jarFile.outputStream()).use { jar ->
            for ((name, bytes) in hierarchyClasses()) {
                jar.putNextEntry(JarEntry("$name.class"))
                jar.write(bytes)
                jar.closeEntry()
            }
        }

        val outputFile = File(dir, "accesstransformer.cfg")

        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        val task = project.tasks
            .register("convertAccessWidener", ConvertAccessWidenerTask::class.java)
            .get()
        task.accessWidener.set(awFile)
        task.hierarchyClasspath.from(jarFile)
        task.output.set(outputFile)
        task.run()

        return outputFile.readText()
    }

    /**
     * `Base` declares a protected `update()V`, overridden with protected access by
     * `Sub` and (transitively) `SubSub`, and with public access by `Other`. `Rec`
     * is a record with the single component `value`.
     */
    private fun hierarchyClasses(): Map<String, ByteArray> {
        val base = classWriter("test/Base") {
            visitMethod(Opcodes.ACC_PROTECTED, "update", "()V", null, null).visitEnd()
            visitField(0, "speed", "F", null, null).visitEnd()
            visitField(0, "health", "F", null, null).visitEnd()
        }

        val sub = classWriter("test/Sub", superName = "test/Base") {
            visitMethod(Opcodes.ACC_PROTECTED, "update", "()V", null, null).visitEnd()
        }

        val subSub = classWriter("test/SubSub", superName = "test/Sub") {
            visitMethod(Opcodes.ACC_PROTECTED, "update", "()V", null, null).visitEnd()
        }

        val other = classWriter("test/Other", superName = "test/Base") {
            visitMethod(Opcodes.ACC_PUBLIC, "update", "()V", null, null).visitEnd()
        }

        val rec = classWriter("test/Rec", superName = "java/lang/Record", access = RECORD_ACCESS) {
            visitRecordComponent("value", "I", null).visitEnd()
            visitField(Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL, "value", "I", null, null).visitEnd()
            visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null).visitEnd()
            visitMethod(Opcodes.ACC_PUBLIC, "value", "()I", null, null).visitEnd()
        }

        return mapOf(
            "test/Base" to base,
            "test/Sub" to sub,
            "test/SubSub" to subSub,
            "test/Other" to other,
            "test/Rec" to rec,
        )
    }

    private fun classWriter(
        name: String,
        superName: String = "java/lang/Object",
        access: Int = Opcodes.ACC_PUBLIC,
        members: ClassWriter.() -> Unit,
    ): ByteArray {
        val writer = ClassWriter(0)
        writer.visit(Opcodes.V21, access, name, null, superName, null)
        writer.members()
        writer.visitEnd()
        return writer.toByteArray()
    }

    private companion object {
        private const val RECORD_ACCESS = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_RECORD
    }

}
