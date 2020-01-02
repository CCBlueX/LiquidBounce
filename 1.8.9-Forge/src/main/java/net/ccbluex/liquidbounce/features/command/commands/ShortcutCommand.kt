package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command

class ShortcutCommand: Command("shortcut", arrayOf()) {

    override fun execute(args: Array<String>) {
        when {
            args.size > 3 && args[1] == "add" -> {
                try {
                    LiquidBounce.CLIENT.commandManager.registerShortcut(args[2], args.sliceArray(3 until args.size))
                } catch (e: IllegalArgumentException) {
                    chat(e.message!!)
                }
            }
            args.size == 3 && args[1] == "remove" -> {
                LiquidBounce.CLIENT.commandManager.unregisterCommand(args[2])

                chat("Removed shortcut.")
            }
            else -> chatSyntax("add <shortcut_name> <script> OR remove <shortcut_name>")
        }
    }

}
