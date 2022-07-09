/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.misc.StringUtils

class ShortcutCommand : Command("shortcut")
{
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>)
    {
        val thePlayer = mc.thePlayer

        when
        {
            args.size > 3 && args[1].equals("add", true) ->
            {
                try
                {
                    LiquidBounce.commandManager.registerShortcut(args[2], StringUtils.toCompleteString(args, 3))

                    chat(thePlayer, "Successfully added shortcut.")
                }
                catch (e: IllegalArgumentException)
                {
                    chat(thePlayer, "${e.message}")
                }
            }

            args.size >= 3 && args[1].equals("remove", true) ->
            {
                if (LiquidBounce.commandManager.unregisterShortcut(args[2])) chat(thePlayer, "Successfully removed shortcut.")
                else chat(thePlayer, "Shortcut does not exist.")
            }

            else -> chat(thePlayer, "shortcut <add <shortcut_name> <script>/remove <shortcut_name>>")
        }
    }
}
