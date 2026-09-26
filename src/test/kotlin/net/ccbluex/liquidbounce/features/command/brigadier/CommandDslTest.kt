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
package net.ccbluex.liquidbounce.features.command.brigadier

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import net.ccbluex.liquidbounce.features.command.arguments.MultiTaggedArgumentType
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.network.chat.contents.TranslatableContents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Verifies the final typed command DSL shape and its declaration-time constraints. */
class CommandDslTest {

    private val dispatcher = CommandDispatcher<ClientCommandSource>()

    private fun execute(input: String): Int {
        val trimmed = input.trim()
        val parse = dispatcher.parse(StringReader(trimmed), ClientCommandSource)
        check(!parse.reader.canRead()) { "Parse failed: $input" }
        val context = parse.context.build(trimmed)
        val executorContext = checkNotNull(context.deepestExecutableContext()) {
            "No executable command for: $input"
        }
        return checkNotNull(executorContext.command) { "No executable command for: $input" }
            .run(executorContext)
    }

    @Test
    fun `required arguments expose typed handles`() {
        dispatcher.register("dslrequired") {
            argument("first", StringArgumentType.word()) { first ->
                argument("second", StringArgumentType.word()) { second ->
                    exec { ctx -> ctx.get(first).length + ctx.get(second).length }
                }
            }
        }

        assertEquals(7, execute("dslrequired ab cdefg"))
    }

    @Test
    fun `nullable optional argument supplies null when omitted`() {
        var observed: String? = "sentinel"
        dispatcher.register("dslnullable") {
            argument("base", StringArgumentType.word()) { base ->
                optional("suffix", StringArgumentType.word(), default = null) { suffix ->
                    exec { ctx ->
                        observed = ctx.get(suffix)
                        ctx.get(base).length
                    }
                }
            }
        }

        assertEquals(2, execute("dslnullable ab"))
        assertNull(observed)
        assertEquals(2, execute("dslnullable ab value"))
        assertEquals("value", observed)
    }

    @Test
    fun `every omitted optional resolves to its declared default`() {
        var observed: Triple<String, Int, String?>? = null
        dispatcher.register("dsldefaults") {
            argument("base", StringArgumentType.word()) { base ->
                optional("page", IntegerArgumentType.integer(1), default = 1) { page ->
                    optional("suffix", StringArgumentType.word(), default = null) { suffix ->
                        exec { ctx ->
                            observed = Triple(ctx.get(base), ctx.get(page), ctx.get(suffix))
                            1
                        }
                    }
                }
            }
        }

        assertEquals(1, execute("dsldefaults value"))
        assertEquals(Triple("value", 1, null), observed)
        assertEquals(1, execute("dsldefaults value 3"))
        assertEquals(Triple("value", 3, null), observed)
        assertEquals(1, execute("dsldefaults value 4 tail"))
        assertEquals(Triple("value", 4, "tail"), observed)
    }

    @Test
    fun `all-optional chain executes at the literal`() {
        dispatcher.register("dslpage") {
            optional("page", IntegerArgumentType.integer(1), default = 1) { page ->
                exec { ctx -> ctx.get(page) }
            }
        }

        assertEquals(1, execute("dslpage"))
        assertEquals(3, execute("dslpage 3"))
    }

    @Test
    fun `literal can execute directly`() {
        dispatcher.register("dslroot") {
            exec { 7 }
        }

        assertEquals(7, execute("dslroot"))
    }

    @Test
    fun `chain without executor fails at declaration`() {
        assertFailsWith<IllegalStateException> {
            literal("dslmissingexec") {
                argument("value", StringArgumentType.word()) { }
            }
        }
    }

    @Test
    fun `literal rejects a duplicate alias`() {
        assertFailsWith<IllegalStateException> {
            literal("dslaliasdup") {
                literal("save", aliases = listOf("create", "Save")) {
                    exec { 1 }
                }
            }
        }
        assertFailsWith<IllegalStateException> {
            literal("dslaliasself") {
                literal("save", aliases = listOf("save")) {
                    exec { 1 }
                }
            }
        }
    }

    @Test
    fun `required argument cannot follow optional argument`() {
        assertFailsWith<IllegalStateException> {
            literal("dsloptionalrequired") {
                optional("optional", StringArgumentType.word(), default = null) {
                    argument("required", StringArgumentType.word()) {
                        exec { 1 }
                    }
                }
            }
        }
    }

    @Test
    fun `greedy string must be the final argument`() {
        assertFailsWith<IllegalStateException> {
            literal("dslgreedyinvalid") {
                argument("message", StringArgumentType.greedyString()) {
                    argument("tail", StringArgumentType.word()) {
                        exec { 1 }
                    }
                }
            }
        }

        var observed: String? = null
        dispatcher.register("dslgreedy") {
            argument("message", StringArgumentType.greedyString()) { message ->
                exec { ctx -> observed = ctx.get(message); 1 }
            }
        }

        assertEquals(1, execute("dslgreedy hello world"))
        assertEquals("hello world", observed)
    }

    @Test
    fun `greedy multi tagged argument must be final`() {
        val type = MultiTaggedArgumentType(
            "modifiers",
            InputBind.Modifier.entries,
            InputBind.Modifier::tag,
        )
        assertFailsWith<IllegalStateException> {
            literal("dslmultiinvalid") {
                argument("modifiers", type) {
                    argument("tail", StringArgumentType.word()) {
                        exec { 1 }
                    }
                }
            }
        }

        dispatcher.register("dslmulti") {
            argument("modifiers", type) { modifiers ->
                exec { ctx -> ctx.get(modifiers).size }
            }
        }

        assertEquals(2, execute("dslmulti control shift"))
    }

    @Test
    fun `chain declares exactly one final executor`() {
        assertFailsWith<IllegalStateException> {
            literal("dslmultipleexec") {
                argument("value", StringArgumentType.word()) {
                    exec { 1 }
                    exec { 2 }
                }
            }
        }
        assertFailsWith<IllegalStateException> {
            literal("dslnonfinalexec") {
                argument("value", StringArgumentType.word()) {
                    exec { 1 }
                    optional("tail", StringArgumentType.word(), default = null) {
                        exec { 2 }
                    }
                }
            }
        }
    }

    @Test
    fun `literal rejects duplicate direct executors`() {
        assertFailsWith<IllegalStateException> {
            literal("dslduplicate") {
                exec { 1 }
                exec { 2 }
            }
        }
    }

    @Test
    fun `literal rejects direct executor with all-optional chain`() {
        assertFailsWith<IllegalStateException> {
            literal("dsldirectoptional") {
                exec { 1 }
                optional("value", StringArgumentType.word(), default = null) {
                    exec { 2 }
                }
            }
        }
    }

    @Test
    fun `literal rejects multiple all-optional chains`() {
        assertFailsWith<IllegalStateException> {
            literal("dslmultipleoptional") {
                optional("first", StringArgumentType.word(), default = null) {
                    exec { 1 }
                }
                optional("second", StringArgumentType.word(), default = null) {
                    exec { 2 }
                }
            }
        }
    }

    @Test
    fun `literal direct executor may coexist with required chain`() {
        dispatcher.register("dsldirectrequired") {
            exec { 1 }
            argument("value", StringArgumentType.word()) {
                exec { 2 }
            }
        }

        assertEquals(1, execute("dsldirectrequired"))
        assertEquals(2, execute("dsldirectrequired value"))
    }

    @Test
    fun `literal branches own independent executors`() {
        dispatcher.register("dslbranches") {
            literal("set") {
                exec { 1 }
            }
            literal("list") {
                optional("page", IntegerArgumentType.integer(1), default = 1) { page ->
                    exec { ctx -> ctx.get(page) }
                }
            }
        }

        assertEquals(1, execute("dslbranches set"))
        assertEquals(1, execute("dslbranches list"))
        assertEquals(4, execute("dslbranches list 4"))
    }

    @Test
    fun `argument continuation is materialized once`() {
        var declarations = 0
        dispatcher.register("dslonce") {
            argument("value", StringArgumentType.word()) { value ->
                declarations++
                exec { ctx -> ctx.get(value).length }
            }
        }

        assertEquals(1, declarations)
        assertEquals(3, execute("dslonce abc"))
        assertEquals(1, declarations)
    }

    @Test
    fun `optional prefixes reuse one native executor`() {
        val root = dispatcher.register("dslexecutorreuse") {
            optional("first", StringArgumentType.word(), default = null) {
                optional("second", StringArgumentType.word(), default = null) {
                    exec { 1 }
                }
            }
        }

        val first = checkNotNull(root.getChild("first"))
        val second = checkNotNull(first.getChild("second"))
        assertSame(root.command, first.command)
        assertSame(first.command, second.command)
    }

    @Test
    fun `t uses the nested literal path`() {
        lateinit var nested: String
        lateinit var chain: String
        dispatcher.register("friend") {
            literal("clear") {
                exec {
                    nested = (t("clear.noFriends").contents as TranslatableContents).key
                    1
                }
            }
            argument("name", StringArgumentType.word()) {
                exec {
                    chain = (t("added").contents as TranslatableContents).key
                    1
                }
            }
        }

        execute("friend clear")
        execute("friend Steve")
        assertEquals("liquidbounce.command.friend.clear.noFriends", nested)
        assertEquals("liquidbounce.command.friend.added", chain)
    }

    @Test
    fun `alias of optional chain receives the provided argument`() {
        dispatcher.register("dslaliasopt", aliases = listOf("dao")) {
            optional("page", StringArgumentType.word(), default = null) { page ->
                exec { ctx -> ctx.get(page)?.length ?: 0 }
            }
        }

        assertEquals(0, execute("dslaliasopt"))
        assertEquals(0, execute("dao"))
        assertEquals(2, execute("dslaliasopt 22"))
        assertEquals(2, execute("dao 22"))
    }

    @Test
    fun `alias of executable parent runs the matching subcommand`() {
        dispatcher.register("dslaliashub", aliases = listOf("dah")) {
            exec { 1 }
            literal("add") {
                exec { 2 }
            }
        }

        assertEquals(1, execute("dslaliashub"))
        assertEquals(1, execute("dah"))
        assertEquals(2, execute("dslaliashub add"))
        assertEquals(2, execute("dah add"))
    }

    @Test
    fun `nested literal aliases redirect to the target`() {
        dispatcher.register("dsllocal") {
            literal("save", aliases = listOf("create")) {
                argument("name", StringArgumentType.word()) { name ->
                    exec { ctx -> ctx.get(name).length }
                }
            }
            literal("browse", aliases = listOf("open")) {
                exec { 4 }
            }
        }

        assertEquals(3, execute("dsllocal save abc"))
        assertEquals(3, execute("dsllocal create abc"))
        assertEquals(4, execute("dsllocal browse"))
        assertEquals(4, execute("dsllocal open"))
    }

    @Test
    fun `root register aliases share the command tree`() {
        dispatcher.register("dslrootalias", aliases = listOf("dra")) {
            argument("name", StringArgumentType.word()) { name ->
                exec { ctx -> ctx.get(name).length }
            }
        }

        assertEquals(3, execute("dslrootalias abc"))
        assertEquals(3, execute("dra abc"))

        val byName = dispatcher.root.children
            .filterIsInstance<LiteralCommandNode<ClientCommandSource>>()
            .associateBy { it.name }
        assertNull(byName.getValue("dslrootalias").redirect)
        assertEquals(byName.getValue("dslrootalias"), byName.getValue("dra").redirect)
    }
}
