/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command

object PingCommand : Command("ping") {

    override fun execute(args: Array<String>) {
        chat("§3Your ping is §a${mc.networkHandler.getPlayerListEntry(mc.player.uniqueID).responseTime}ms§3.")
    }

}