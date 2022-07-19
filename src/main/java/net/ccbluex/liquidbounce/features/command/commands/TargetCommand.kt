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
                    chat("Target 'players' toggled ${if (EntityUtils.defaultTargets.players) "on" else "off"}.")
                    playEdit()
                    return
                }

                args[1].equals("mobs", ignoreCase = true) -> {
                    EntityUtils.defaultTargets.mobs = !EntityUtils.defaultTargets.mobs
                    chat("Target 'mobs' toggled ${if (EntityUtils.defaultTargets.mobs) "on" else "off"}.")
                    playEdit()
                    return
                }

                args[1].equals("animals", ignoreCase = true) -> {
                    EntityUtils.defaultTargets.animals = !EntityUtils.defaultTargets.animals
                    chat("Target 'animals' toggled ${if (EntityUtils.defaultTargets.animals) "on" else "off"}.")
                    playEdit()
                    return
                }

                args[1].equals("invisible", ignoreCase = true) -> {
                    EntityUtils.defaultTargets.invisible = !EntityUtils.defaultTargets.invisible
                    chat("Target 'invisible' toggled ${if (EntityUtils.defaultTargets.invisible) "on" else "off"}.")
                    playEdit()
                    return
                }

                args[1].equals("dead", ignoreCase = true) -> {
                    EntityUtils.defaultTargets.dead = !EntityUtils.defaultTargets.dead
                    chat("Target 'dead' toggled ${if (EntityUtils.defaultTargets.dead) "on" else "off"}.")
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
