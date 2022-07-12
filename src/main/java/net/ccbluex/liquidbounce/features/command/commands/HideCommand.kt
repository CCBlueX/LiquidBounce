/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import java.util.*

class HideCommand : Command("hide")
{

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>)
    {
        val thePlayer = mc.thePlayer

        if (args.size > 1)
        {
            val moduleManager = LiquidBounce.moduleManager
            val modules = moduleManager.modules

            when (args[1].lowercase(Locale.getDefault()))
            {
                "list" ->
                {
                    chat(thePlayer, "\u00A7c\u00A7lHidden")
                    modules.filterNot(Module::array).forEach { ClientUtils.displayChatMessage(thePlayer, "\u00A76> \u00A7c${it.name}") }
                    return
                }

                "clear" ->
                {
                    for (module in modules) module.array = true

                    chat(thePlayer, "Cleared hidden modules.")
                    return
                }

                "reset" ->
                {
                    for (module in modules) module.array = module::class.java.getAnnotation(ModuleInfo::class.java).array

                    chat(thePlayer, "Reset hidden modules.")
                    return
                }

                else ->
                {
                    // Get module by name
                    val module = moduleManager.getModule(args[1])

                    if (module == null)
                    {
                        chat(thePlayer, "Module \u00A7a\u00A7l${args[1]}\u00A73 not found.")
                        return
                    }

                    // Find key by name and change
                    module.array = !module.array

                    // Response to user
                    chat(thePlayer, "Module \u00A7a\u00A7l${module.name}\u00A73 is now \u00A7a\u00A7l${if (module.array) "visible" else "invisible"}\u00A73 on the array list.")
                    playEdit()
                    return
                }
            }
        }

        chatSyntax(thePlayer, "hide <module/list/clear/reset>")
    }

    override fun tabComplete(args: Array<String>): List<String>
    {
        if (args.isEmpty()) return emptyList()

        val moduleName = args[0]

        return when (args.size)
        {
            1 -> LiquidBounce.moduleManager.modules.map(Module::name).filter { it.startsWith(moduleName, true) }.toList()
            else -> emptyList()
        }
    }
}
