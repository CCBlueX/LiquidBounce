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
package net.ccbluex.liquidbounce.script.bindings.features

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.deepestExecutableContext
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Script command trees compile to the same optional-tail / alias shape as the in-tree DSL.
 */
class ScriptCommandBuilderTest {

    class ArgSink {
        val values = mutableListOf<Any?>()
        fun add(value: Any?) {
            values += value
        }
    }

    private fun withScript(js: String, block: (CommandDispatcher<ClientCommandSource>) -> Unit) {
        withScript(js, ArgSink()) { dispatcher, _ -> block(dispatcher) }
    }

    /** Evaluates [js] without registering it; the block receives the raw command object. */
    private fun withRawCommand(js: String, block: (Value) -> Unit) {
        Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .build()
            .use { graal ->
                block(graal.eval("js", js))
            }
    }

    private fun withScript(
        js: String,
        sink: ArgSink,
        block: (CommandDispatcher<ClientCommandSource>, ArgSink) -> Unit,
    ) {
        Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .build()
            .use { graal ->
                graal.getBindings("js").putMember("sink", sink)
                val commandObject: Value = graal.eval("js", js)
                val dispatcher = CommandDispatcher<ClientCommandSource>()
                ScriptCommandBuilder(commandObject).build().forEach { dispatcher.root.addChild(it) }
                block(dispatcher, sink)
            }
    }

    private fun CommandDispatcher<ClientCommandSource>.execute(input: String): Int {
        val parse = parse(StringReader(input), ClientCommandSource)
        check(!parse.reader.canRead()) { "Parse failed: $input remaining=${parse.reader.remaining}" }
        val context = parse.context.build(input)
        val executorContext = checkNotNull(context.deepestExecutableContext()) {
            "No executable command for: $input"
        }
        return checkNotNull(executorContext.command).run(executorContext)
    }

    private fun CommandDispatcher<ClientCommandSource>.parses(input: String) {
        val parse = parse(StringReader(input), ClientCommandSource)
        assertTrue(
            !parse.reader.canRead() && hasExecutableCommand(parse),
            "Expected '$input' to fully parse (remaining: '${parse.reader.remaining}')"
        )
    }

    private fun CommandDispatcher<ClientCommandSource>.doesNotParse(input: String) {
        val parse = parse(StringReader(input), ClientCommandSource)
        assertFalse(
            !parse.reader.canRead() && parse.exceptions.isEmpty() && hasExecutableCommand(parse),
            "Expected '$input' to fail parsing"
        )
    }

    private fun hasExecutableCommand(parse: com.mojang.brigadier.ParseResults<ClientCommandSource>): Boolean {
        var context = parse.context
        while (context != null) {
            if (context.command != null) return true
            context = context.child
        }
        return false
    }

    @Test
    fun `required plus optional executes when the optional is omitted`() {
        withScript(
            """
            ({
                name: "sfriend",
                parameters: [
                    { name: "player", required: true },
                    { name: "alias", required: false }
                ],
                onExecute: function(player, alias) {}
            })
            """.trimIndent()
        ) { dispatcher ->
            dispatcher.parses("sfriend Steve")
            dispatcher.parses("sfriend Steve buddy")
            dispatcher.doesNotParse("sfriend")
        }
    }

    @Test
    fun `all-optional chain executes at the literal`() {
        withScript(
            """
            ({
                name: "spage",
                parameters: [
                    { name: "page", required: false }
                ],
                onExecute: function(page) {}
            })
            """.trimIndent()
        ) { dispatcher ->
            dispatcher.parses("spage")
            dispatcher.parses("spage 2")
        }
    }

    @Test
    fun `missing or undefined required is optional`() {
        withScript(
            """
            ({
                name: "sdefault",
                parameters: [
                    { name: "player" },
                    { name: "alias", required: undefined }
                ],
                onExecute: function(player, alias) {}
            })
            """.trimIndent()
        ) { dispatcher ->
            dispatcher.parses("sdefault")
            dispatcher.parses("sdefault Steve")
            dispatcher.parses("sdefault Steve buddy")
        }
    }

    @Test
    fun `subcommand aliases attach under the parent`() {
        withScript(
            """
            ({
                name: "sroot",
                subcommands: [
                    {
                        name: "add",
                        aliases: ["a"],
                        onExecute: function() {}
                    }
                ]
            })
            """.trimIndent()
        ) { dispatcher ->
            dispatcher.parses("sroot add")
            dispatcher.parses("sroot a")
            dispatcher.doesNotParse("a")
        }
    }

    @Test
    fun `alias of optional parameter forwards the value to onExecute`() {
        val sink = ArgSink()
        withScript(
            """
            ({
                name: "spage",
                aliases: ["sp"],
                parameters: [
                    { name: "page", required: false }
                ],
                onExecute: function(page) { sink.add(page); }
            })
            """.trimIndent(),
            sink,
        ) { dispatcher, captured ->
            dispatcher.execute("spage")
            dispatcher.execute("sp")
            dispatcher.execute("spage 2")
            dispatcher.execute("sp 2")

            assertEquals(4, captured.values.size)
            assertNull(captured.values[0])
            assertNull(captured.values[1])
            assertEquals("2", captured.values[2]?.toString())
            assertEquals("2", captured.values[3]?.toString())
        }
    }

    @Test
    fun `alias of executable parent dispatches to the subcommand handler`() {
        val sink = ArgSink()
        withScript(
            """
            ({
                name: "slist",
                aliases: ["sl"],
                subcommands: [
                    {
                        name: "add",
                        onExecute: function() { sink.add("add"); }
                    }
                ],
                onExecute: function() { sink.add("list"); }
            })
            """.trimIndent(),
            sink,
        ) { dispatcher, captured ->
            dispatcher.execute("slist")
            dispatcher.execute("sl")
            dispatcher.execute("slist add")
            dispatcher.execute("sl add")

            assertEquals(listOf("list", "list", "add", "add"), captured.values.map { it.toString() })
        }
    }

    @Test
    fun `duplicate parameter names fail at build time`() {
        withRawCommand(
            """
            ({
                name: "sdupparam",
                parameters: [
                    { name: "value" },
                    { name: "value" }
                ],
                onExecute: function(a, b) {}
            })
            """.trimIndent()
        ) { commandObject ->
            assertFailsWith<IllegalStateException> {
                ScriptCommandBuilder(commandObject).build()
            }
        }
    }

    @Test
    fun `duplicate sibling subcommand names fail at build time`() {
        withRawCommand(
            """
            ({
                name: "sdupsub",
                subcommands: [
                    { name: "add", onExecute: function() {} },
                    { name: "add", onExecute: function() {} }
                ]
            })
            """.trimIndent()
        ) { commandObject ->
            assertFailsWith<IllegalStateException> {
                ScriptCommandBuilder(commandObject).build()
            }
        }
    }

    @Test
    fun `subcommand alias colliding with a sibling name fails at build time`() {
        withRawCommand(
            """
            ({
                name: "saliassub",
                subcommands: [
                    { name: "remove", aliases: ["rm"], onExecute: function() {} },
                    { name: "rm", onExecute: function() {} }
                ]
            })
            """.trimIndent()
        ) { commandObject ->
            assertFailsWith<IllegalStateException> {
                ScriptCommandBuilder(commandObject).build()
            }
        }
    }
}
