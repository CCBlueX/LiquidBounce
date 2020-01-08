package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class VClipCommand : Command("vclip", emptyArray()) {
    /**
     * Execute command
     *
     * @param args arguments by user
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            try {
                val y = args[1].toDouble()
                val entity = if(mc.thePlayer.isRiding) mc.thePlayer.ridingEntity else mc.thePlayer

                entity.setPosition(entity.posX, entity.posY + y, entity.posZ)
                chat("You were teleported.")
            } catch (ex: NumberFormatException) {
                chatSyntaxError()
            }

            return
        }

        chatSyntax("vclip <value>")
    }
}
