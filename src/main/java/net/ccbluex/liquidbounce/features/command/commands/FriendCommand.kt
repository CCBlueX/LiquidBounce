/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.file.FileManager.friendsConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.utils.misc.StringUtils

object FriendCommand : Command("friend", "friends") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <add/remove/list/clear>")
            return
        }

        val friendsConfig = friendsConfig

        when (args[1].lowercase()) {
            "add" -> {
                if (args.size <= 2) {
                    chatSyntax("$usedAlias add <name> [alias]")
                    return
                }

                val name = args[2]

                if (name.isEmpty()) {
                    chat("The name is empty.")
                    return
                }

                if (if (args.size > 3) friendsConfig.addFriend(name, StringUtils.toCompleteString(args, 3)) else friendsConfig.addFriend(name)) {
                    saveConfig(friendsConfig)
                    chat("§a§l$name§3 was added to your friend list.")
                    playEdit()
                } else {
                    chat("The name is already in the list.")
                }
            }

            "remove" -> {
                if (args.size <= 2) {
                    chatSyntax("$usedAlias remove <name>")
                    return
                }

                val name = args[2]

                if (friendsConfig.removeFriend(name)) {
                    saveConfig(friendsConfig)
                    chat("§a§l$name§3 was removed from your friend list.")
                    playEdit()
                } else {
                    chat("This name is not in the list.")
                }
                    
            }

            "clear" -> {
                val friends = friendsConfig.friends.size
                friendsConfig.clearFriends()
                saveConfig(friendsConfig)
                chat("Removed $friends friend(s).")
            }

            "list" -> {
                chat("Your Friends:")

                for (friend in friendsConfig.friends)
                    chat("§7> §a§l${friend.playerName} §c(§7§l${friend.alias}§c)")

                chat("You have §c${friendsConfig.friends.size}§3 friends.")
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("add", "remove", "list", "clear").filter { it.startsWith(args[0], true) }
            2 -> {
                when (args[0].lowercase()) {
                    "add" -> {
                        return mc.world.playerEntities
                                .filter { (it.name?.startsWith(args[1], true) ?: false) }
                                .map { it.name }
                    }
                    "remove" -> {
                        return friendsConfig.friends
                                .map { it.playerName }
                                .filter { it.startsWith(args[1], true) }
                    }
                }
                return emptyList()
            }
            else -> emptyList()
        }
    }
}