/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2020 CCBlueX
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

package net.ccbluex.liquidbounce.command

import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.commands.FriendCommand
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class CommandManagerTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            CommandManager.addCommand(
                CommandBuilder.begin("test1")
                    .description("Does nothing. Doesn't even have a handler.")
                    .hub()
                    .subcommand(CommandBuilder.begin("yeet")
                        .handler { throw SuccessException("yeet ran") }
                        .build())
                    .build()
            )

            CommandManager.addCommand(
                CommandBuilder.begin("test2")
                    .alias("test_ee")
                    .description("Throws an exception, but takes no parameters")
                    .handler { throw SuccessException("test1 ran") }
                    .build()
            )

            CommandManager.addCommand(
                CommandBuilder.begin("test3")
                    .description("Throws an exception, but takes no parameters")
                    .handler { throw SuccessException("test3 ran (${(it[0] as Int).toString(16)})") }
                    .parameter(
                        ParameterBuilder.begin<Int>("damage")
                            .description("Positive Integer parameter")
                            .verifiedBy(ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)
                            .required()
                            .build()
                    )
                    .build()
            )

            CommandManager.addCommand(
                CommandBuilder.begin("test4")
                    .description("Throws an exception, but takes no parameters")
                    .handler { throw SuccessException("test4 ran (${(it[0] as String)};${(it[1] as Int).toString(16)})") }
                    .parameter(
                        ParameterBuilder.begin<String>("without_verifier")
                            .description("Just a string")
                            .required()
                            .build()
                    )
                    .parameter(
                        ParameterBuilder.begin<Int>("damage")
                            .description("Positive Integer parameter")
                            .verifiedBy(ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)
                            .required()
                            .build()
                    )
                    .build()
            )

            CommandManager.addCommand(
                CommandBuilder.begin("test5")
                    .description("Throws an exception, but takes no parameters")
                    .handler {
                        throw SuccessException(
                            "test5 ran (${(it[0] as String)};[${
                                (it[1] as Array<*>).joinToString(
                                    ","
                                ) { int -> (int as Int).toString(16) }
                            }])"
                        )
                    }
                    .parameter(
                        ParameterBuilder.begin<String>("without_verifier")
                            .description("Just a string")
                            .required()
                            .build()
                    )
                    .parameter(
                        ParameterBuilder.begin<Int>("damages")
                            .description("Positive Integer parameter")
                            .verifiedBy(ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)
                            .vararg()
                            .optional()
                            .build()
                    )
                    .build()
            )

            CommandManager.addCommand(
                CommandBuilder.begin("test6")
                    .description("Throws an exception, but takes no parameters")
                    .handler { throw SuccessException("test5 ran ([${((it[0] as Array<*>)).joinToString(",") { int -> "\"${int as String}\"" }}])") }
                    .parameter(
                        ParameterBuilder.begin<String>("without_verifiers")
                            .description("Positive Integer parameter")
                            .vararg()
                            .optional()
                            .build()
                    )
                    .build()
            )

            CommandManager.addCommand(FriendCommand.createCommand())
        }
    }

    class SuccessException(message: String) : Exception(message)

    @Test
    fun getSubCommand() {
        assertEquals("friend", CommandManager.getSubCommand("friend")?.first?.name)
        assertEquals("friend", CommandManager.getSubCommand("friend sfgh")?.first?.name)
        assertEquals("add", CommandManager.getSubCommand("friend add")?.first?.name)
        assertEquals("add", CommandManager.getSubCommand("friend add dfsg")?.first?.name)
        assertEquals("add", CommandManager.getSubCommand("friend add dfsg fsdsa")?.first?.name)
        assertEquals("add", CommandManager.getSubCommand("friend add dfsg add fsdsa")?.first?.name)
        assertEquals("friend add", CommandManager.getSubCommand("friend add dfg")?.first?.getFullName())

        assertEquals(0, CommandManager.getSubCommand("friend")?.second)
        assertEquals(0, CommandManager.getSubCommand("friend sfgh")?.second)
        assertEquals(1, CommandManager.getSubCommand("friend add")?.second)
        assertEquals(1, CommandManager.getSubCommand("friend add dfsg")?.second)
        assertEquals(1, CommandManager.getSubCommand("friend add dfsg fsdsa")?.second)
        assertEquals(1, CommandManager.getSubCommand("friend add dfsg add fsdsa")?.second)
    }

    @Test
    fun tokenizeTest() {
        assertEquals(listOf<String>(), CommandManager.tokenize(""))
        assertEquals(listOf(".friend"), CommandManager.tokenize(".friend"))
        assertEquals(listOf(".friend", "add"), CommandManager.tokenize(".friend add"))
        assertEquals(listOf(".friend", "add"), CommandManager.tokenize(".friend  add"))
        assertEquals(listOf(".friend", "add"), CommandManager.tokenize(".friend     add"))
        assertEquals(listOf(".friend", "add"), CommandManager.tokenize(".friend     add   "))
        assertEquals(listOf(".friend", "add"), CommandManager.tokenize(".friend     add "))
        assertEquals(listOf(".friend", "add", "SenkJu"), CommandManager.tokenize(".friend     add SenkJu"))
        assertEquals(listOf(".friend", "add", "SenkJu"), CommandManager.tokenize(".friend     add SenkJu"))
        assertEquals(listOf(".friend", "add", "\"SenkJu"), CommandManager.tokenize(".friend     add \"SenkJu"))
        assertEquals(listOf(".friend", "add", "\"Senk Ju"), CommandManager.tokenize(".friend     add \"Senk Ju"))
        assertEquals(listOf(".friend", "add", "Senk Ju"), CommandManager.tokenize(".friend     add \"Senk Ju\""))
        assertEquals(
            listOf(".friend", "add", "Senk Ju", "a s d f"),
            CommandManager.tokenize(".friend     add \"Senk Ju\" \"a s d f\"")
        )
        assertEquals(
            listOf(".friend", "add", "SenkJu", "\"Mensch\""),
            CommandManager.tokenize(".friend add SenkJu \\\"Mensch\\\"")
        )
        assertEquals(
            listOf(".friend", "add", "SenkJu", "\\\"Mensch\\\""),
            CommandManager.tokenize(".friend add SenkJu \\\\\\\"Mensch\\\\\\\"")
        )
        assertEquals(
            listOf(".friend", "add", "SenkJu", "\\Mensch\\"),
            CommandManager.tokenize(".friend add SenkJu \\\\\"Mensch\\\\\"")
        )
        assertEquals(
            listOf(".friend", "add", "SenkJu", "xX_Mensch_Xx"),
            CommandManager.tokenize(".friend add SenkJu xX_\"Mensch\"_Xx")
        )
        assertEquals(listOf(".friend", "add", "SenkJu", "sr+"), CommandManager.tokenize(".friend add SenkJu \\s\\r\\+"))
    }

    @Test
    fun executeTest() {
        assertThrows<CommandException> { CommandManager.execute("test1") }
        assertThrows<SuccessException> { CommandManager.execute("test1 yeet") }
        assertThrows<CommandException> { CommandManager.execute("test1 beet") }

        assertThrows<CommandException> { CommandManager.execute("test2 param1") }
        assertThrows<CommandException> { CommandManager.execute("test2 param1 param2") }
        assertThrows<CommandException> { CommandManager.execute("test_ee param1") }
        assertThrows<CommandException> { CommandManager.execute("test_ee param1 param2") }
        assertThrows<SuccessException> { CommandManager.execute("test2") }
        assertThrows<SuccessException> { CommandManager.execute("test_ee") }

        assertThrows<CommandException> { CommandManager.execute("test3") }
        assertThrows<CommandException> { CommandManager.execute("test3 abcdef") }
        assertThrows<CommandException> { CommandManager.execute("test3 -10") }

        try {
            CommandManager.execute("test3 10")
        } catch (e: SuccessException) {
            assertEquals(e.message, "test3 ran (a)")
        }
        try {
            CommandManager.execute("test3 \"1337\"")
        } catch (e: SuccessException) {
            assertEquals(e.message, "test3 ran (539)")
        }

        assertThrows<CommandException> { CommandManager.execute("test4") }
        assertThrows<CommandException> { CommandManager.execute("test4 abcdef") }
        assertThrows<CommandException> { CommandManager.execute("test4 -10") }
        assertThrows<CommandException> { CommandManager.execute("test4 -10 -10") }

        try {
            CommandManager.execute("test4 \"He lo\" 1337")
        } catch (e: SuccessException) {
            assertEquals(e.message, "test4 ran (He lo;539)")
        }

        assertThrows<CommandException> { CommandManager.execute("test5") }
        assertThrows<CommandException> { CommandManager.execute("test5 11 abcdef") }

        try {
            CommandManager.execute("test5 Yeee 1337 10 11")
        } catch (e: SuccessException) {
            assertEquals(e.message, "test5 ran (Yeee;[539,a,b])")
        }
        try {
            CommandManager.execute("test5 Yeee")
        } catch (e: SuccessException) {
            assertEquals(e.message, "test5 ran (Yeee;[])")
        }
    }

    private inline fun <reified T : Throwable> assertThrows(executable: () -> Unit): T {
        val throwable: Throwable? = try {
            executable()
        } catch (caught: Throwable) {
            caught
        } as? Throwable

        return Assertions.assertThrows(T::class.java) {
            if (throwable != null) {
                throw throwable
            }
        }
    }
}
