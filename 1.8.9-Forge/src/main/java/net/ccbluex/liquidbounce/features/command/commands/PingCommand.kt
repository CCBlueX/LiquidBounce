package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class PingCommand : Command("ping", emptyArray()) {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) = chat("§3Your ping is §a${mc.netHandler.getPlayerInfo(mc.thePlayer.uniqueID).responseTime}§3ms.")

}