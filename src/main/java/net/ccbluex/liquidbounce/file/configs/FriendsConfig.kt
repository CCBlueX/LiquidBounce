/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.*
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import java.io.*

class FriendsConfig(file: File) : FileConfig(file) {
    val friends: MutableList<Friend> = ArrayList()

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        clearFriends()
        try {
            val jsonElement = JsonParser().parse(file.bufferedReader())
            if (jsonElement is JsonNull) return
            for (friendElement in jsonElement.asJsonArray) {
                val friendObject = friendElement.asJsonObject
                addFriend(friendObject["playerName"].asString, friendObject["alias"].asString)
            }
        } catch (ex: JsonSyntaxException) {
            //When the JSON Parse fail, the client try to load and update the old config
            LOGGER.info("[FileManager] Try to load old Friends config...")
            val bufferedReader = file.bufferedReader()
            var line: String
            while (bufferedReader.readLine().also { line = it } != null) {
                if ("{" !in line && "}" !in line) {
                    line = line.replace(" ", "").replace("\"", "").replace(",", "")
                    if (":" in line) {
                        val data = line.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        addFriend(data[0], data[1])
                    } else addFriend(line)
                }
            }
            bufferedReader.close()
            LOGGER.info("[FileManager] Loaded old Friends config...")

            //Save the friends into a new valid JSON file
            saveConfig()
            LOGGER.info("[FileManager] Saved Friends to new config...")
        } catch (ex: IllegalStateException) {
            LOGGER.info("[FileManager] Try to load old Friends config...")
            val bufferedReader = file.bufferedReader()
            var line: String
            while (bufferedReader.readLine().also { line = it } != null) {
                if ("{" !in line && "}" !in line) {
                    line = line.replace(" ", "").replace("\"", "").replace(",", "")
                    if (":" in line) {
                        val data = line.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        addFriend(data[0], data[1])
                    } else addFriend(line)
                }
            }
            bufferedReader.close()
            LOGGER.info("[FileManager] Loaded old Friends config...")
            saveConfig()
            LOGGER.info("[FileManager] Saved Friends to new config...")
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig() {
        val jsonArray = JsonArray()
        for (friend in friends) {
            val friendObject = JsonObject()
            friendObject.addProperty("playerName", friend.playerName)
            friendObject.addProperty("alias", friend.alias)
            jsonArray.add(friendObject)
        }
        val printWriter = PrintWriter(FileWriter(file))
        printWriter.println(PRETTY_GSON.toJson(jsonArray))
        printWriter.close()
    }

    /**
     * Add friend to config
     *
     * @param playerName of friend
     * @param alias      of friend
     * @return of successfully added friend
     */
    /**
     * Add friend to config
     *
     * @param playerName of friend
     * @return of successfully added friend
     */

    fun addFriend(playerName: String, alias: String = playerName): Boolean {
        if (isFriend(playerName)) return false

        friends.add(Friend(playerName, alias))
        return true
    }

    /**
     * Remove friend from config
     *
     * @param playerName of friend
     */
    fun removeFriend(playerName: String) = friends.removeIf { it.playerName == playerName }

    /**
     * Check is friend
     *
     * @param playerName of friend
     * @return is friend
     */
    fun isFriend(playerName: String) = friends.any { it.playerName == playerName }

    /**
     * Clear all friends from config
     */
    fun clearFriends() = friends.clear()

    /**
     * @param playerName of friend
     * @param alias      of friend
     */
    class Friend
        internal constructor(
            val playerName: String,
            val alias: String
        )
}