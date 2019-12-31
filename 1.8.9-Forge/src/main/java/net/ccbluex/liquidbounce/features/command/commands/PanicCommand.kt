package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleManager

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class PanicCommand : Command("panic", emptyArray()) {

    override fun execute(args: Array<String>) {
        var modules = ModuleManager.getModules().filter { it.state }
        var currType = ""

        if (args.size > 1) {
            if (!args[1].equals("all", true)) {
                try {
                    val category = ModuleCategory.values().first { it.displayName.equals(args[1], true) }
                    modules = modules.filter { it.category == category }
                    currType = " ${category.displayName.toLowerCase()}"
                } catch (noSuchElementException: NoSuchElementException) {
                    chat("§cThe category does not exist!")
                    return
                }
            }
        } else {
            modules = modules.filter { it.category != ModuleCategory.RENDER }

            currType = " non-render"
        }

        for (module in modules)
            module.state = false

        chat("Disabled all$currType modules.")
    }

}