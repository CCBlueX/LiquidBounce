/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.EntityUtils

class TargetCommand : Command("target") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            when {
                args[1].equals("players", ignoreCase = true) -> {
                    EntityUtils.defaultTargets.players = !EntityUtils.defaultTargets.players
                    chat("§7Target §8players§7 toggled §8${if (EntityUtils.defaultTargets.players) "on" else "off"}§7.")
                    playEdit()
                    return
                }

                args[1].equals("mobs", ignoreCase = true) -> {
                    EntityUtils.defaultTargets.mobs = !EntityUtils.defaultTargets.mobs
                    chat("§7Target §8mobs§7 toggled §8${if (EntityUtils.defaultTargets.mobs) "on" else "off"}§7.")
                    playEdit()
                    return
                }

                args[1].equals("animals", ignoreCase = true) -> {
                    EntityUtils.defaultTargets.animals = !EntityUtils.defaultTargets.animals
                    chat("§7Target §8animals§7 toggled §8${if (EntityUtils.defaultTargets.animals) "on" else "off"}§7.")
                    playEdit()
                    return
                }

                args[1].equals("invisible", ignoreCase = true) -> {
                    EntityUtils.defaultTargets.invisible = !EntityUtils.defaultTargets.invisible
                    chat("§7Target §8invisible§7 toggled §8${if (EntityUtils.defaultTargets.invisible) "on" else "off"}§7.")
                    playEdit()
                    return
                }

                args[1].equals("dead", ignoreCase = true) -> {
                    EntityUtils.defaultTargets.dead = !EntityUtils.defaultTargets.dead
                    chat("§7Target §8dead§7 toggled §8${if (EntityUtils.defaultTargets.dead) "on" else "off"}§7.")
                    playEdit()
                    return
                }
            }
        }

        chatSyntax("target <players/mobs/animals/invisible/dead>")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("players", "mobs", "animals", "invisible", "dead")
                .filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}
