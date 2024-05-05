/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.command.Command

object ToggleCommand : Command("toggle", "t") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <module> [on/off]")
            return
        }

        val module = moduleManager[args[1]]

        if (module == null) {
            chat("Module '${args[1]}' not found.")
            return
        }

        if (args.size > 2) {
            val newState = args[2].lowercase()

            if (newState == "on" || newState == "off") {
                module.state = newState == "on"
            } else {
                chatSyntax("$usedAlias <module> [on/off]")
            }
        } else {
            module.toggle()
        }

        chat("${if (module.state) "Enabled" else "Disabled"} module §8${module.getName()}§3.")

    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        val moduleName = args[0]

        return when (args.size) {
            1 -> moduleManager.modules
                    .map { it.name }
                    .filter { it.startsWith(moduleName, true) }
                    .toList()
            else -> emptyList()
        }
    }

}
