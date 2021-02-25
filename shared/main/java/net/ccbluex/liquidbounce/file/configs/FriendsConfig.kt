/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.*
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import java.io.File
import java.io.IOException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Constructor of config
 *
 * @param file
 * of config
 */
class FriendsConfig(file: File) : FileConfig(file)
{
	val friends: MutableList<Friend> = ArrayList()

	/**
	 * Load config from file
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun loadConfig()
	{
		clearFriends()
		try
		{
			val jsonElement = JsonParser().parse(MiscUtils.createBufferedFileReader(file))

			if (jsonElement is JsonNull) return

			jsonElement.asJsonArray.map { it.asJsonObject }.forEach { addFriend(it["playerName"].asString, it["alias"].asString) }
		}
		catch (ex: JsonSyntaxException)
		{
			// When the JSON Parse fail, the client try to load and update the old config

			logger.info("[FileManager] Try to load old Friends config...")

			val bufferedReader = MiscUtils.createBufferedFileReader(file)
			val emptyReplacement = Matcher.quoteReplacement("")
			var line: String

			while (bufferedReader.readLine().also { line = it } != null) if (!line.contains("{") && !line.contains("}"))
			{
				line = BLANK.matcher(line).replaceAll(emptyReplacement)
				line = QUOTE.matcher(line).replaceAll(emptyReplacement)
				line = COMMA.matcher(line).replaceAll(emptyReplacement)
				if (line.contains(":"))
				{
					val data = line.split(":")
					addFriend(data[0], data[1])
				}
				else addFriend(line)
			}

			bufferedReader.close()
			logger.info("[FileManager] Loaded old Friends config...")

			// Save the friends into a new valid JSON file
			saveConfig()
			logger.info("[FileManager] Saved Friends to new config...")
		}
		catch (ex: IllegalStateException)
		{
			logger.info("[FileManager] Try to load old Friends config...")

			val bufferedReader = MiscUtils.createBufferedFileReader(file)
			val emptyReplacement = Matcher.quoteReplacement("")
			var line: String

			while (bufferedReader.readLine().also { line = it } != null) if (!line.contains("{") && !line.contains("}"))
			{
				line = BLANK.matcher(line).replaceAll(emptyReplacement)
				line = QUOTE.matcher(line).replaceAll(emptyReplacement)
				line = COMMA.matcher(line).replaceAll(emptyReplacement)
				if (line.contains(":"))
				{
					val data = line.split(":")
					addFriend(data[0], data[1])
				}
				else addFriend(line)
			}

			bufferedReader.close()

			logger.info("[FileManager] Loaded old Friends config...")
			saveConfig()
			logger.info("[FileManager] Saved Friends to new config...")
		}
	}

	/**
	 * Save config to file
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun saveConfig()
	{
		val jsonArray = JsonArray()

		for (friend in friends)
		{
			val friendObject = JsonObject()

			friendObject.addProperty("playerName", friend.playerName)
			friendObject.addProperty("alias", friend.alias)

			jsonArray.add(friendObject)
		}

		val writer = MiscUtils.createBufferedFileWriter(file)
		writer.write(FileManager.PRETTY_GSON.toJson(jsonArray) + System.lineSeparator())
		writer.close()
	}
	/**
	 * Add friend to config
	 *
	 * @param  playerName
	 * of friend
	 * @param  alias
	 * of friend
	 * @return            of successfully added friend
	 */
	/**
	 * Add friend to config
	 *
	 * @param  playerName
	 * of friend
	 * @return            of successfully added friend
	 */
	@JvmOverloads
	fun addFriend(playerName: String, alias: String = playerName): Boolean
	{
		if (isFriend(playerName)) return false

		friends.add(Friend(playerName, alias))

		return true
	}

	/**
	 * Remove friend from config
	 *
	 * @param playerName
	 * of friend
	 */
	fun removeFriend(playerName: String): Boolean
	{
		if (!isFriend(playerName)) return false

		friends.removeIf { friend: Friend -> friend.playerName == playerName }

		return true
	}

	/**
	 * Check is friend
	 *
	 * @param  playerName
	 * of friend
	 * @return            is friend
	 */
	fun isFriend(playerName: String): Boolean = friends.any { it.playerName == playerName }

	/**
	 * Clear all friends from config
	 */
	fun clearFriends()
	{
		friends.clear()
	}

	class Friend
	/**
	 * @param playerName
	 * of friend
	 * @param alias
	 * of friend
	 */ internal constructor(
		/**
		 * @return name of friend
		 */
		val playerName: String,
		/**
		 * @return alias of friend
		 */
		val alias: String
	)

	companion object
	{
		private val BLANK = Pattern.compile(" ", Pattern.LITERAL)
		private val QUOTE = Pattern.compile("\"", Pattern.LITERAL)
		private val COMMA = Pattern.compile(",", Pattern.LITERAL)
	}
}
