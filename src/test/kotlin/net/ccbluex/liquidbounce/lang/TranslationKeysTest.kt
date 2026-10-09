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
package net.ccbluex.liquidbounce.lang

import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class TranslationKeysTest {

    private companion object {
        /**
         * Finds literal `translation("liquidbounce.*")` calls only.
         * Runtime-composed keys from module `message(...)`, command `t(...)`, and setting descriptions
         * are not checked here; this test is not an exhaustive translation audit.
         */
        val TRANSLATION_CALL = Regex("""\btranslation\(\s*"(liquidbounce\.[A-Za-z0-9_.]+)"""")

        val SOURCE_ROOT: Path by lazy {
            val testClasses = Path.of(TranslationKeysTest::class.java.protectionDomain.codeSource.location.toURI())
            generateSequence(testClasses) { it.parent }
                .map { it.resolve("src/main") }
                .firstOrNull { Files.isDirectory(it) }
                ?: error("Cannot locate src/main from test classes at $testClasses")
        }
    }

    private fun englishKeys(): Set<String> {
        val file = checkNotNull(javaClass.getResourceAsStream("/resources/liquidbounce/lang/en_us.json")) {
            "The English language file is missing"
        }

        return file.bufferedReader().use { JsonParser.parseReader(it).asJsonObject.keySet() }
    }

    /**
     * The keys written out in the sources, with the position of their first use.
     */
    private fun usedKeys(): Map<String, String> {
        val used = sortedMapOf<String, String>()
        Files.walk(SOURCE_ROOT).use { paths ->
            paths.filter { it.toString().endsWith(".kt") || it.toString().endsWith(".java") }.forEach { path ->
                val text = Files.readString(path)
                for (match in TRANSLATION_CALL.findAll(text)) {
                    val line = text.take(match.range.first).count { it == '\n' } + 1
                    used.putIfAbsent(match.groupValues[1], "$path:$line")
                }
            }
        }

        return used
    }

    @Test
    fun `every translation key written out in the sources has an English translation`() {
        val english = englishKeys()
        val used = usedKeys()
        assertTrue(used.isNotEmpty(), "No translation keys were found in the sources")

        val missing = used.filterKeys { it !in english }
        assertTrue(
            missing.isEmpty(),
            "These keys would show up untranslated, add them to en_us.json:\n" +
                missing.entries.joinToString("\n") { (key, position) -> "$key ($position)" },
        )
    }

}
