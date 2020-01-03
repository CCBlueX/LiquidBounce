package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.misc.StringUtils

class ShortcutCommand: Command("shortcut", arrayOf()) {

    override fun execute(args: Array<String>) {
        when {
            args.size > 3 && args[1].equals("add", true) -> {
                try {
                    LiquidBounce.CLIENT.commandManager.registerShortcut(args[2], StringUtils.toCompleteString(args, 3))
                } catch (e: IllegalArgumentException) {
                    chat(e.message!!)
                }
            }

            args.size >= 3 && args[1].equals("remove", true) -> {
                LiquidBounce.CLIENT.commandManager.unregisterCommand(args[2])

                chat("Removed shortcut.")
            }

            else -> chat("shortcut <add <shortcut_name> <script>/remove <shortcut_name>>")
        }
    }

}
