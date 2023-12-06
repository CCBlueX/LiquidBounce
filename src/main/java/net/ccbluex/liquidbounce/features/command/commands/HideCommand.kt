/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage

object HideCommand : Command("hide") {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            when {
                args[1].equals("list", true) -> {
                    chat("§c§lHidden")
                    moduleManager.modules.filter { !it.inArray }.forEach {
                        displayChatMessage("§6> §c${it.getName()}")
                    }
                    return
                }

                args[1].equals("clear", true) -> {
                    for (module in moduleManager.modules)
                        module.inArray = true

                    chat("Cleared hidden modules.")
                    return
                }

                args[1].equals("reset", true) -> {
                    for (module in moduleManager.modules)
                        module.inArray = module.defaultInArray

                    chat("Reset hidden modules.")
                    return
                }

                else -> {
                    // Get module by name
                    val module = moduleManager[args[1]]

                    if (module == null) {
                        chat("Module §a§l${args[1]}§3 not found.")
                        return
                    }

                    // Find key by name and change
                    module.inArray = !module.inArray

                    // Response to user
                    chat("Module §a§l${module.getName()}§3 is now §a§l${if (module.inArray) "visible" else "invisible"}§3 on the array list.")
                    playEdit()
                    return
                }
            }
        }

        chatSyntax("hide <module/list/clear/reset>")
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