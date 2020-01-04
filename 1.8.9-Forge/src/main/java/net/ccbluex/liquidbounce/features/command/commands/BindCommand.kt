package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import org.lwjgl.input.Keyboard

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class BindCommand : Command("bind", emptyArray()) {

    override fun execute(args: Array<String>) {
        if (args.size > 2) {
            val module = ModuleManager.getModule(args[1])

            if (module == null) {
                chat("Module §a§l" + args[1] + "§3 not found.")
                return
            }

            val key = Keyboard.getKeyIndex(args[2].toUpperCase())
            module.keyBind = key
            chat("Bound module §a§l${module.name}§3 to key §a§l${Keyboard.getKeyName(key)}§3.")
            LiquidBounce.CLIENT.hud.addNotification(Notification("Bound ${module.name} to ${Keyboard.getKeyName(key)}"))
            playEdit()
            return
        }

        chatSyntax(arrayOf("<module> <key>", "<module> none"))
    }

}