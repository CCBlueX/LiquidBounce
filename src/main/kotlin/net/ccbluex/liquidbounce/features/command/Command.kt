/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.command

import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import java.util.*

fun interface CommandHandler {
    operator fun invoke(command: Command, args: Array<Any>)
}

@Suppress("LongParameterList")
class Command(
    val name: String,
    val aliases: Array<out String>,
    val parameters: Array<Parameter<*>>,
    val subcommands: Array<Command>,
    val executable: Boolean,
    val handler: CommandHandler?,
    val requiresIngame: Boolean,
    private var parentCommand: Command? = null
) : MinecraftShortcuts {
    val translationBaseKey: String
        get() = "liquidbounce.command.${getParentKeys(this, name)}"

    val description: String
        get() = translation("$translationBaseKey.description").convertToString()

    init {
        subcommands.forEach {
            check(it.parentCommand == null) {
                "Subcommand already has parent command"
            }

            it.parentCommand = this
        }

        parameters.forEach {
            check(it.command == null) {
                "Parameter already has a command"
            }

            it.command = this
        }
    }

    private fun getParentKeys(currentCommand: Command?, current: String): String {
        val parentName = currentCommand?.parentCommand?.name

        return if (parentName != null) {
            getParentKeys(currentCommand.parentCommand, "$parentName.subcommand.$current")
        } else {
            current
        }
    }

    fun result(key: String, vararg args: Any): MutableText {
        return translation("$translationBaseKey.result.$key", args = args)
    }

    /**
     * Sends a styled command result with copyable content
     *
     * @param key Translation key (will be prefixed with command's translation base)
     * @param data Optional data to be displayed and copied
     * @param formatting Function to apply formatting to the text (default: regular)
     * @param hover Optional hover event (defaults to "Click to copy" tooltip)
     * @param clickAction Optional click action type (defaults to COPY_TO_CLIPBOARD)
     */
    fun printStyledText(
        key: String,
        data: String? = null,
        formatting: (MutableText) -> MutableText = ::regular,
        hover: HoverEvent? = HoverEvent(HoverEvent.Action.SHOW_TEXT, translation("liquidbounce.tooltip.clickToCopy")),
        clickAction: ClickEvent.Action = ClickEvent.Action.COPY_TO_CLIPBOARD
    ) {
        val content = data?.let(::variable) ?: markAsError("N/A")
        val resultText = formatting(result(key, content))
        val clickEvent = data?.let { ClickEvent(clickAction, it) }

        chat(resultText.onHover(hover).onClick(clickEvent))
    }

    /**
     * Sends a styled command result with copyable content and custom text component
     *
     * @param key Translation key (will be prefixed with command's translation base)
     * @param textComponent Text component to display
     * @param copyContent Optional content to copy when clicked (defaults to text component's string representation)
     * @param formatting Function to apply formatting to the text (default: regular)
     * @param hover Optional hover event (defaults to "Click to copy" tooltip)
     */
    fun printStyledComponent(
        key: String,
        textComponent: MutableText? = null,
        copyContent: String? = null,
        formatting: (MutableText) -> MutableText = ::regular,
        hover: HoverEvent? = HoverEvent(HoverEvent.Action.SHOW_TEXT, translation("liquidbounce.tooltip.clickToCopy"))
    ) {
        val displayComponent = textComponent ?: markAsError("N/A")
        val content = copyContent ?: displayComponent.convertToString()

        chat(formatting(result(key, displayComponent)).copyable(copyContent = content, hover = hover))
    }

    fun resultWithTree(key: String, vararg args: Any): MutableText {
        var parentCommand = this.parentCommand
        if (parentCommand != null) {
            // Keep going until parent command is null
            while (parentCommand?.parentCommand != null) {
                parentCommand = parentCommand.parentCommand
            }

            return parentCommand!!.result(key, args = args)
        }

        return translation("$translationBaseKey.result.$key", args = args)
    }

    /**
     * Returns the name of the command with the name of its parent classes
     */
    private fun getFullName(): String {
        val parent = this.parentCommand

        return if (parent == null) {
            this.name
        } else {
            parent.getFullName() + " " + this.name
        }
    }

    /**
     * Returns the formatted usage information of this command
     *
     * e.g. <code>command_name subcommand_name <required_arg> [[<optional_vararg>]...</code>
     */
    fun usage(): List<String> {
        val output = ArrayList<String>()

        // Don't show non-executable commands as executable
        if (executable) {
            val joiner = StringJoiner(" ")

            for (parameter in parameters) {
                var name = parameter.name

                name = if (parameter.required) {
                    "<$name>"
                } else {
                    "[<$name>]"
                }

                if (parameter.vararg) {
                    name += "..."
                }

                joiner.add(name)
            }

            output.add(this.name + " " + joiner.toString())
        }

        for (subcommand in subcommands) {
            for (subcommandUsage in subcommand.usage()) {
                output.add(this.name + " " + subcommandUsage)
            }
        }

        return output
    }

    fun autoComplete(
        builder: SuggestionsBuilder,
        tokenizationResult: Pair<List<String>, List<Int>>,
        commandIdx: Int,
        isNewParameter: Boolean
    ) {
        val args = tokenizationResult.first

        val offset = args.size - commandIdx - 1

        val isAtSecondParameterBeginning = offset == 0 && isNewParameter
        val isInSecondParameter = offset == 1 && !isNewParameter

        // Handle Subcommands
        if (isAtSecondParameterBeginning || isInSecondParameter) {
            val comparedAgainst = if (!isNewParameter) args[offset] else ""

            this.subcommands.forEach { subcommand ->
                if (subcommand.name.startsWith(comparedAgainst, true)) {
                    builder.suggest(subcommand.name)
                }

                subcommand.aliases.filter { it.startsWith(comparedAgainst, true) }.forEach { builder.suggest(it) }
            }
        }

        var paramIdx = args.size - commandIdx - 2

        if (isNewParameter) {
            paramIdx++
        }

        if (paramIdx < 0) {
            return
        }

        val idx = commandIdx + paramIdx + 1

        val parameter = if (paramIdx >= parameters.size) {
            val lastParameter = this.parameters.lastOrNull()

            if (lastParameter?.vararg != true) {
                return
            }

            lastParameter
        } else {
            this.parameters[paramIdx]
        }

        val handler = parameter.autocompletionHandler ?: return

        val suggestions = handler.autocomplete(begin = args.getOrElse(idx) { "" }, args = args)

        suggestions.forEach(builder::suggest)
    }
}
