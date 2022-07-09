/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command

class VClipCommand : Command("vclip")
{
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>)
    {
        val thePlayer = mc.thePlayer ?: return

        if (args.size > 1)
        {
            try
            {
                val y = args[1].toDouble()

                val entity = thePlayer.ridingEntity ?: thePlayer

                entity.setPosition(entity.posX, entity.posY + y, entity.posZ)
                chat(thePlayer, "You were teleported.")
            }
            catch (ex: NumberFormatException)
            {
                chatSyntaxError(thePlayer)
            }

            return
        }

        chatSyntax(thePlayer, "vclip <value>")
    }
}
