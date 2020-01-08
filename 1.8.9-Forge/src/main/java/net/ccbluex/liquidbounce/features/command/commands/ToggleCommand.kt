package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.minecraft.util.EnumChatFormatting

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class ToggleCommand : Command("toggle", arrayOf("t")) {

    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            val module = ModuleManager.getModule(args[1])

            if (module == null) {
                chat("Module '${args[1]}' not found.")
                return
            }

            module.toggle()
            chat("${if (module.state) "Enabled" else "Disabled"} module §8${module.name}§3.")
            return
        }

        chatSyntax("toggle <module>")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        val moduleName = args[0]

        return when (args.size) {
            1 -> ModuleManager.getModules()
                .map { it.name }
                .filter { it.startsWith(moduleName, true) }
                .toList()
            else -> emptyList()
        }
    }

}
