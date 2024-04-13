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
        if (args.size > 1) {
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
                    chatSyntax("toggle <module> [on/off]")
                    return
                }
            } else {
                module.toggle()
            }

            chat("${if (module.state) "Enabled" else "Disabled"} module §8${module.getName()}§3.")
            return
        }

        chatSyntax("toggle <module> [on/off]")
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
