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
package net.ccbluex.liquidbounce.features.command

import com.mojang.brigadier.suggestion.SuggestionsBuilder
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.joinToText
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style

@Suppress("LongParameterList")
class Command(
    val name: String,
    val aliases: List<String>,
    val parameters: List<Parameter<*>>,
    val subcommands: List<Command>,
    val executable: Boolean,
    val handler: Handler?,
    val requiresIngame: Boolean,
) : MinecraftShortcuts, DebuggedOwner {
    var parentCommand: Command? = null
        private set
    var index: Int = -1
        internal set

    val translationBaseKey: String
        get() = "liquidbounce.command.${getParentKeys(this, name)}"

    val description: MutableComponent
        get() = translation("$translationBaseKey.description")

    fun nameAsText(): Component =
        this.name.asPlainText(Style.EMPTY.withHoverEvent(HoverEvent.ShowText(this.description)))

    /**
     * For navigation purposes.
     * Key: name or alias
     * Value: corresponding subcommand
     */
    internal val subcommandMap = Object2ObjectRBTreeMap<String, Command>(String.CASE_INSENSITIVE_ORDER)

    init {
        subcommands.forEachIndexed { i, command ->
            check(command.parentCommand == null) {
                "Subcommand already has parent command"
            }
            subcommandMap.putCommand(command)

            command.index = i
            command.parentCommand = this
        }

        parameters.forEachIndexed { i, param ->
            check(param.command == null) {
                "Parameter already has a command"
            }

            param.index = i
            param.command = this
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

    fun result(key: String, vararg args: Any): MutableComponent {
        return translation("$translationBaseKey.result.$key", args = args)
    }

    /**
     * Sends a styled command result with copyable content
     *
     * @param key Translation key (will be prefixed with command's translation base)
     * @param data Optional data to be displayed and copied
     * @param formatting Function to apply formatting to the text (default: regular)
     * @param hover Optional hover event (defaults to "Click to copy" tooltip)
     * @param click Optional click action type (defaults to [ClickEvent.CopyToClipboard])
     */
    fun printStyledText(
        key: String,
        data: String? = null,
        formatting: (MutableComponent) -> MutableComponent = ::regular,
        hover: HoverEvent? = HoverEvent.ShowText(translation("liquidbounce.tooltip.clickToCopy")),
        click: ClickEvent? = data?.let(ClickEvent::CopyToClipboard)
    ) {
        val content = data?.let(::variable) ?: markAsError("N/A")
        val resultText = formatting(result(key, content))

        chat(resultText.onHover(hover).onClick(click))
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
        textComponent: Component? = null,
        copyContent: String? = null,
        formatting: (MutableComponent) -> MutableComponent = ::regular,
        hover: HoverEvent? = HoverEvent.ShowText(translation("liquidbounce.tooltip.clickToCopy"))
    ) {
        val displayComponent = textComponent ?: markAsError("N/A")
        val content = copyContent ?: displayComponent.string

        chat(formatting(result(key, displayComponent)).copyable(copyContent = content, hover = hover))
    }

    fun resultWithTree(key: String, vararg args: Any): MutableComponent {
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
     * Returns the formatted usage information of this command
     *
     * e.g.
     * ```
     * command_name subcommand_name <required_arg> [[<optional_vararg>]...
     * ```
     */
    fun usage(): List<Component> {
        val output = ArrayList<Component>()

        // Don't show non-executable commands as executable
        if (executable) {
            // Names
            val textParts = ArrayList<Component>()
            generateSequence(this) { it.parentCommand }.mapTo(textParts) { it.nameAsText() }
            textParts.reverse()

            // Params
            textParts.ensureCapacity(textParts.size + parameters.size)
            parameters.mapTo(textParts) { it.nameAsText() }

            output.add(textParts.joinToText(PlainText.SPACE))
        }

        for (subcommand in subcommands) {
            for (subcommandUsage in subcommand.usage()) {
                output.add(subcommandUsage)
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

    fun interface Handler {
        operator fun Context.invoke()

        class Context(@JvmField val command: Command, @JvmField val args: Array<out Any>)

        fun interface Suspend {
            suspend operator fun Context.invoke()
        }
    }

    /**
     * Provides a [Command] to the [CommandManager].
     */
    fun interface Factory {

        /**
         * Creates the [Command] and is run only once by the [CommandManager].
         */
        fun createCommand(): Command
    }

}
