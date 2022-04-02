/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command

class RemoteViewCommand : Command("remoteview", "rv") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            if (mc.renderViewEntity != mc.thePlayer) {
                mc.renderViewEntity = mc.thePlayer
                return
            }
            chatSyntax("remoteview <username>")
            return
        }

        val targetName = args[1]

        for (entity in mc.theWorld!!.loadedEntityList) {
            if (targetName == entity.name) {
                mc.renderViewEntity = entity
                chat("Now viewing perspective of §8${entity.name}§3.")
                chat("Execute §8${LiquidBounce.commandManager.prefix}remoteview §3again to go back to yours.")
                break
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> return mc.theWorld!!.playerEntities
                    .filter { it.name != null && it.name!!.startsWith(args[0], true) }
                    .map { it.name!! }
            else -> emptyList()
        }
    }
}