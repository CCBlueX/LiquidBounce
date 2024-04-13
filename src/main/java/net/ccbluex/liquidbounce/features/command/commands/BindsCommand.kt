/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import org.lwjgl.input.Keyboard

object BindsCommand : Command("binds") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            if (args[1].equals("clear", true)) {
                for (module in moduleManager.modules)
                    module.keyBind = Keyboard.KEY_NONE

                chat("Removed all binds.")
                return
            }
        }

        chat("§c§lBinds")
        moduleManager.modules.filter { it.keyBind != Keyboard.KEY_NONE }.forEach {
            displayChatMessage("§6> §c${it.getName()}: §a§l${Keyboard.getKeyName(it.keyBind)}")
        }
        chatSyntax("binds clear")
    }
}