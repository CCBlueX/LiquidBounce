/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.commands.AutoSettingsCommand
import net.ccbluex.liquidbounce.features.command.commands.BindCommand
import net.ccbluex.liquidbounce.features.command.commands.BindsCommand
import net.ccbluex.liquidbounce.features.command.commands.EnchantCommand
import net.ccbluex.liquidbounce.features.command.commands.FriendCommand
import net.ccbluex.liquidbounce.features.command.commands.GiveCommand
import net.ccbluex.liquidbounce.features.command.commands.HClipCommand
import net.ccbluex.liquidbounce.features.command.commands.HelpCommand
import net.ccbluex.liquidbounce.features.command.commands.HideCommand
import net.ccbluex.liquidbounce.features.command.commands.HoloStandCommand
import net.ccbluex.liquidbounce.features.command.commands.HurtCommand
import net.ccbluex.liquidbounce.features.command.commands.LocalAutoSettingsCommand
import net.ccbluex.liquidbounce.features.command.commands.LoginCommand
import net.ccbluex.liquidbounce.features.command.commands.PanicCommand
import net.ccbluex.liquidbounce.features.command.commands.PingCommand
import net.ccbluex.liquidbounce.features.command.commands.PrefixCommand
import net.ccbluex.liquidbounce.features.command.commands.ReloadCommand
import net.ccbluex.liquidbounce.features.command.commands.RemoteViewCommand
import net.ccbluex.liquidbounce.features.command.commands.RenameCommand
import net.ccbluex.liquidbounce.features.command.commands.SayCommand
import net.ccbluex.liquidbounce.features.command.commands.ScriptManagerCommand
import net.ccbluex.liquidbounce.features.command.commands.ServerInfoCommand
import net.ccbluex.liquidbounce.features.command.commands.ShortcutCommand
import net.ccbluex.liquidbounce.features.command.commands.TacoCommand
import net.ccbluex.liquidbounce.features.command.commands.TargetCommand
import net.ccbluex.liquidbounce.features.command.commands.ToggleCommand
import net.ccbluex.liquidbounce.features.command.commands.UsernameCommand
import net.ccbluex.liquidbounce.features.command.commands.VClipCommand
import net.ccbluex.liquidbounce.features.command.shortcuts.Shortcut
import net.ccbluex.liquidbounce.features.command.shortcuts.ShortcutParser
import net.ccbluex.liquidbounce.features.command.special.ChatAdminCommand
import net.ccbluex.liquidbounce.features.command.special.ChatTokenCommand
import net.ccbluex.liquidbounce.features.command.special.LiquidChatCommand
import net.ccbluex.liquidbounce.features.command.special.PrivateChatCommand
import net.ccbluex.liquidbounce.features.command.special.XrayCommand
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.extensions.withDoubleQuotes

class CommandManager
{
    val commands = mutableListOf<Command>()
    val commandNameSet = mutableSetOf<String>()
    var latestAutoComplete: Array<String> = emptyArray()

    var prefix = '.'

    /**
     * Register all default commands
     */
    fun registerCommands()
    {
        registerCommand(BindCommand())
        registerCommand(VClipCommand())
        registerCommand(HClipCommand())
        registerCommand(HelpCommand())
        registerCommand(SayCommand())
        registerCommand(FriendCommand())
        registerCommand(AutoSettingsCommand())
        registerCommand(LocalAutoSettingsCommand())
        registerCommand(ServerInfoCommand())
        registerCommand(ToggleCommand())
        registerCommand(HurtCommand())
        registerCommand(GiveCommand())
        registerCommand(UsernameCommand())
        registerCommand(TargetCommand())
        registerCommand(TacoCommand())
        registerCommand(BindsCommand())
        registerCommand(HoloStandCommand())
        registerCommand(PanicCommand())
        registerCommand(PingCommand())
        registerCommand(RenameCommand())
        registerCommand(EnchantCommand())
        registerCommand(ReloadCommand())
        registerCommand(LoginCommand())
        registerCommand(ScriptManagerCommand())
        registerCommand(RemoteViewCommand())
        registerCommand(PrefixCommand())
        registerCommand(ShortcutCommand())
        registerCommand(HideCommand())
        registerCommand(XrayCommand())
        registerCommand(LiquidChatCommand())
        registerCommand(PrivateChatCommand())
        registerCommand(ChatTokenCommand())
        registerCommand(ChatAdminCommand())
    }

    /**
     * Execute command by given [input]
     */
    fun executeCommands(input: String)
    {
        val args = input.split(" ").toTypedArray()
        commands.firstOrNull { command -> args[0].equals("$prefix" + command.command, ignoreCase = true) || command.alias.any { args[0].equals("$prefix" + it, ignoreCase = true) } }?.execute(args) ?: ClientUtils.displayChatMessage(mc.thePlayer, "\u00A7cCommand not found. Type ${"${prefix}help".withDoubleQuotes("\u00A73\u00A7l", "\u00A78")}\u00A7c to view all commands.")
    }

    /**
     * Updates the [latestAutoComplete] array based on the provided [input].
     *
     * @param input text that should be used to check for auto completions.
     * @author NurMarvin
     */
    fun autoComplete(input: String): Boolean
    {
        latestAutoComplete = getCompletions(input) ?: emptyArray()
        return input.startsWith(prefix) && latestAutoComplete.isNotEmpty()
    }

    /**
     * Returns the auto completions for [input].
     *
     * @param input text that should be used to check for auto completions.
     * @author NurMarvin
     */
    private fun getCompletions(input: String): Array<String>?
    {
        if (input.isNotEmpty() && input[0] == prefix)
        {
            val args = input.split(" ")

            return (if (args.size > 1)
            {
                val command = getCommand(args[0].substring(1))
                val tabCompletions = command?.tabComplete(args.drop(1).toTypedArray())

                tabCompletions
            }
            else
            {
                val rawInput = input.substring(1)
                commands.filter { command -> command.command.startsWith(rawInput, true) || command.alias.any { it.startsWith(rawInput, true) } }.map { prefix + if (it.command.startsWith(rawInput, true)) it.command else it.alias.first { alias -> alias.startsWith(rawInput, true) } }.toList()
            })?.toTypedArray()
        }

        return null
    }

    /**
     * Get command instance by given [name]
     */
    fun getCommand(name: String): Command? = commands.find { command -> command.command.equals(name, ignoreCase = true) || command.alias.any { it.equals(name, true) } }

    /**
     * Register [command] by just adding it to the commands registry
     */
    fun registerCommand(command: Command)
    {
        if (!commandNameSet.contains(command.command))
        {
            commands.add(command)
            commandNameSet.add(command.command)
        }
    }

    fun registerShortcut(name: String, script: String)
    {
        require(!commandNameSet.contains(name)) { "Command already exists!" }

        registerCommand(Shortcut(name, ShortcutParser.parse(script).map {
            val command = getCommand(it[0]) ?: throw IllegalArgumentException("Command ${it[0]} not found!")

            command to it.toTypedArray()
        }))

        FileManager.saveConfig(LiquidBounce.fileManager.shortcutsConfig)
    }

    fun unregisterShortcut(name: String): Boolean
    {
        val removed = commands.removeIf {
            it is Shortcut && it.command.equals(name, ignoreCase = true)
        }

        if (removed) commandNameSet.remove(name)

        FileManager.saveConfig(LiquidBounce.fileManager.shortcutsConfig)

        return removed
    }

    /**
     * Unregister [command] by just removing it from the commands registry
     */
    fun unregisterCommand(command: Command?)
    {
        commands.remove(command)
        command?.command?.let(commandNameSet::remove)
    }
}
