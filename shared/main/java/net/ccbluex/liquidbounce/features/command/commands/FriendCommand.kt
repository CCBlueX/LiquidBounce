/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.misc.StringUtils

class FriendCommand : Command("friend", "friends")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer

		if (args.size > 1)
		{
			val friendsConfig = LiquidBounce.fileManager.friendsConfig

			when (args[1].toLowerCase())
			{
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

						if (if (args.size > 3) friendsConfig.addFriend(name, StringUtils.toCompleteString(args, 3)) else friendsConfig.addFriend(name))
						{
							FileManager.saveConfig(friendsConfig)
							chat(thePlayer, "\u00A7a\u00A7l$name\u00A73 was added to your friend list.")
							playEdit()
						}
						else chat(thePlayer, "The name is already in the list.")
						return
					}
					chatSyntax(thePlayer, "friend add <name> [alias]")
					return
				}

				"remove" ->
				{
					if (args.size > 2)
					{
						val name = args[2]

						if (friendsConfig.removeFriend(name))
						{
							FileManager.saveConfig(friendsConfig)
							chat(thePlayer, "\u00A7a\u00A7l$name\u00A73 was removed from your friend list.")
							playEdit()
						}
						else chat(thePlayer, "This name is not in the list.")
						return
					}
					chatSyntax(thePlayer, "friend remove <name>")
					return
				}

				"clear" ->
				{
					val friends = friendsConfig.friends.size
					friendsConfig.clearFriends()
					FileManager.saveConfig(friendsConfig)
					chat(thePlayer, "Removed $friends friend(s).")
					return
				}

				"list" ->
				{
					chat(thePlayer, "Your Friends:")

					for (friend in friendsConfig.friends) chat(thePlayer, "\u00A77> \u00A7a\u00A7l${friend.playerName} \u00A7c(\u00A77\u00A7l${friend.alias}\u00A7c)")

					chat(thePlayer, "You have \u00A7c${friendsConfig.friends.size}\u00A73 friends.")
					return
				}
			}
		}

		chatSyntax(thePlayer, "friend <add/remove/list/clear>")
	}

	override fun tabComplete(args: Array<String>): List<String>
	{
		if (args.isEmpty()) return emptyList()

		return when (args.size)
		{
			1 -> listOf("add", "remove", "list", "clear").filter { it.startsWith(args[0], true) }

			2 ->
			{
				when (args[0].toLowerCase())
				{
					"add" -> return mc.theWorld!!.playerEntities.asSequence().filter { (it.name?.startsWith(args[1], true) ?: false) }.map { it.name!! }.toList()
					"remove" -> return LiquidBounce.fileManager.friendsConfig.friends.asSequence().map { it.playerName }.filter { it.startsWith(args[1], true) }.toList()
				}
				return emptyList()
			}

			else -> emptyList()
		}
	}
}
