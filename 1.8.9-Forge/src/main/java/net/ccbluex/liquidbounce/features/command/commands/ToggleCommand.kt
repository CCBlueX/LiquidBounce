package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class ToggleCommand : Command("toggle", arrayOf("t")) {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            val module = LiquidBounce.moduleManager.getModule(args[1])

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

}
