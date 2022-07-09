/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.extensions.forward

class HClipCommand : Command("hclip")
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
                thePlayer.forward(args[1].toDouble())
                chat(thePlayer, "You were teleported.")
            }
            catch (exception: NumberFormatException)
            {
                chatSyntaxError(thePlayer)
            }
            return
        }

        chatSyntax(thePlayer, "hclip <value>")
    }
}
