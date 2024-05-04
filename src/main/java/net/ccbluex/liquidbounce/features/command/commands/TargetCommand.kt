/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.EntityUtils.targetAnimals
import net.ccbluex.liquidbounce.utils.EntityUtils.targetInvisible
import net.ccbluex.liquidbounce.utils.EntityUtils.targetMobs
import net.ccbluex.liquidbounce.utils.EntityUtils.targetPlayer

object TargetCommand : Command("target") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size <= 1) {
            chatSyntax("target <players/mobs/animals/invisible>")
        }

        when (args[1].lowercase()) {
            "players" -> {
                targetPlayer = !targetPlayer
                chat("§7Target player toggled ${if (targetPlayer) "on" else "off"}.")
                playEdit()
            }

            "mobs" -> {
                targetMobs = !targetMobs
                chat("§7Target mobs toggled ${if (targetMobs) "on" else "off"}.")
                playEdit()
            }

            "animals" -> {
                targetAnimals = !targetAnimals
                chat("§7Target animals toggled ${if (targetAnimals) "on" else "off"}.")
                playEdit()
            }

            "invisible" -> {
                targetInvisible = !targetInvisible
                chat("§7Target Invisible toggled ${if (targetInvisible) "on" else "off"}.")
                playEdit()
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("players", "mobs", "animals", "invisible")
                .filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}