/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.misc.AutoDisable

object AutoDisableCommand : Command("autodisable") {

    /**
     * Execute commands with provided [args]
     */
    override suspend fun execute(args: Array<String>) {
        if (args.size < 2) {
            chatSyntax("autodisable <add/remove/list>")
            return
        }

        when (args[1].lowercase()) {
            "add" -> {
                if (args.size < 3) {
                    chatSyntax("autodisable add <module>")
                    return
                }

                val moduleName = args[2]
                val module = ModuleManager.getModule(moduleName)

                if (module != null) {
                    if (AutoDisable.getModules().contains(module)) {
                        chat("§cModule §b$moduleName §cis already in the auto-disable list.")
                    } else {
                        AutoDisable.addModule(module)
                        chat("§b$moduleName §ahas been added to the auto-disable list.")
                    }
                } else {
                    chat("§cModule §b$moduleName §cnot found.")
                }
            }
            "remove" -> {
                if (args.size < 3) {
                    chatSyntax("autodisable remove <module>")
                    return
                }

                val moduleName = args[2]
                val module = ModuleManager.getModule(moduleName)

                if (module != null) {
                    if (AutoDisable.getModules().contains(module)) {
                        AutoDisable.removeModule(module)
                        chat("§b$moduleName §6has been removed from the auto-disable list.")
                    } else {
                        chat("§cModule §b$moduleName §cis not in the auto-disable list.")
                    }
                } else {
                    chat("§cModule §b$moduleName §cnot found.")
                }
            }
            "list" -> {
                val modules = AutoDisable.getModules()
                chat("Modules in the auto-disable list:")
                modules.forEach { chat(it.name) }
            }
            else -> chatSyntax("autodisable <add/remove/list>")
        }
    }

    override suspend fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) {
            return emptyList()
        }

        return when (args.size) {
            1 -> listOf("add", "remove", "list").filter { it.startsWith(args[0], true) }
            2 -> {
                when (args[0].lowercase()) {
                    "add" -> {
                        val input = args[1].lowercase()
                        ModuleManager.modules.filter { it.name.lowercase().startsWith(input) }.map { it.name }
                    }
                    "remove" -> {
                        val input = args[1].lowercase()
                        AutoDisable.getModules().filter { it.name.lowercase().startsWith(input) }.map { it.name }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}