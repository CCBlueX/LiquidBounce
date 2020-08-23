/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command

class HurtCommand : Command("hurt") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        var damage = 1

        if (args.size > 1) {
            try {
                damage = args[1].toInt()
            } catch (ignored: NumberFormatException) {
                chatSyntaxError()
                return
            }
        }

        // Latest NoCheatPlus damage exploit
        val thePlayer = mc.thePlayer ?: return

        val x = thePlayer.posX
        val y = thePlayer.posY
        val z = thePlayer.posZ

        for (i in 0 until 65 * damage) {
            mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerPosition(x, y + 0.049, z, false))
            mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerPosition(x, y, z, false))
        }

        mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerPosition(x, y, z, true))

        // Output message
        chat("You were damaged.")
    }
}