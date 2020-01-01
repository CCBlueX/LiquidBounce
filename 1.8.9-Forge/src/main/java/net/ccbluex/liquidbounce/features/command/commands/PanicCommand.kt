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
        val msg: String

        if (args.size > 1 && args[1].isNotEmpty()) {
            when (args[1].toLowerCase()) {
                "all" -> msg = "all"

                "nonrender" -> {
                    modules = modules.filter { it.category != ModuleCategory.RENDER }
                    msg = "all non-render"
                }

                else -> {
                    val categories = ModuleCategory.values().filter { it.displayName.equals(args[1], true) }

                    if(categories.isEmpty()) {
                        chat("Category ${args[1]} not found")
                        return
                    }

                    val category = categories[0]
                    modules = modules.filter { it.category == category }
                    msg = "all ${category.displayName}"
                }
            }
        } else {
            chatSyntax("panic <all/nonrender/combat/player/movement/render/world/misc/exploit/fun>")
            return
        }

        for (module in modules)
            module.state = false

        chat("Disabled $msg modules.")
    }
}
