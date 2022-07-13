/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.minecraft.entity.player.EntityPlayer

class TargetCommand : Command("target")
{
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>)
    {
        val thePlayer = mc.thePlayer

        if (args.size > 1)
        {
            val targetsConfig = LiquidBounce.fileManager.targetsConfig
            val targetListSize = targetsConfig.targets.size

            when (args[1].lowercase())
            {
                "players" ->
                {
                    EntityUtils.targetPlayer = !EntityUtils.targetPlayer
                    chat(thePlayer, "\u00A77Target player toggled ${if (EntityUtils.targetPlayer) "\u00A7aon" else "\u00A7coff"}.")
                    playEdit()
                    return
                }

                "mobs" ->
                {
                    EntityUtils.targetMobs = !EntityUtils.targetMobs
                    chat(thePlayer, "\u00A77Target mobs toggled ${if (EntityUtils.targetMobs) "\u00A7on" else "\u00A7coff"}.")
                    playEdit()
                    return
                }

                "animals" ->
                {
                    EntityUtils.targetAnimals = !EntityUtils.targetAnimals
                    chat(thePlayer, "\u00A77Target animals toggled ${if (EntityUtils.targetAnimals) "\u00A7on" else "\u00A7coff"}.")
                    playEdit()
                    return
                }

                "invisible" ->
                {
                    EntityUtils.targetInvisible = !EntityUtils.targetInvisible
                    chat(thePlayer, "\u00A77Target Invisible toggled ${if (EntityUtils.targetInvisible) "\u00A7on" else "\u00A7coff"}.")
                    playEdit()
                    return
                }

                "add" ->
                {
                    if (args.size > 2)
                    {
                        val name = args[2]

                        if (name.isEmpty())
                        {
                            chat(thePlayer, "The name is empty.")
                            return
                        }

                        if (targetsConfig.addTarget(name))
                        {
                            FileManager.saveConfig(targetsConfig)
                            chat(thePlayer, "\u00A7a\u00A7l$name\u00A73 was added to your target list.")
                            playEdit()
                        }
                        else chat(thePlayer, "The name is already in the list.")
                        return
                    }
                    chatSyntax(thePlayer, "target add <name>")
                    return
                }

                "remove" ->
                {
                    if (args.size > 2)
                    {
                        val name = args[2]

                        if (targetsConfig.removeTarget(name))
                        {
                            FileManager.saveConfig(targetsConfig)
                            chat(thePlayer, "\u00A7a\u00A7l$name\u00A73 was removed from your target list.")
                            playEdit()
                        }
                        else chat(thePlayer, "This name is not in the list.")
                        return
                    }
                    chatSyntax(thePlayer, "target remove <name>")
                    return
                }

                "clear" ->
                {
                    targetsConfig.clearTargets()
                    FileManager.saveConfig(targetsConfig)
                    chat(thePlayer, "Removed $targetListSize target(s).")
                    return
                }

                "list" ->
                {
                    chat(thePlayer, "Your targets:")

                    for (target in targetsConfig.targets) chat(thePlayer, "\u00A77> \u00A7a\u00A7l$target \u00A7c")

                    chat(thePlayer, "You have \u00A7c$targetListSize\u00A73 targets.")
                    return
                }
            }
        }

        chatSyntax(thePlayer, "target <players/mobs/animals/invisible or add/remove/list/clear>")
    }

    override fun tabComplete(args: Array<String>): List<String>
    {
        if (args.isEmpty()) return emptyList()

        val theWorld = mc.theWorld ?: return emptyList()

        return when (args.size)
        {
            1 -> listOf("players", "mobs", "animals", "invisible", "add", "remove", "list", "clear").filter { it.startsWith(args[0], true) }

            2 ->
            {
                val prefix = args[1]
                when (args[0].lowercase())
                {
                    "add" -> return theWorld.playerEntities.map(EntityPlayer::getName).filter { it.startsWith(prefix, true) }.toList()
                    "remove" -> return LiquidBounce.fileManager.targetsConfig.targets.filter { it.startsWith(prefix, true) }.toList()
                }

                return emptyList()
            }

            else -> emptyList()
        }
    }
}
