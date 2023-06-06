/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import java.io.*

class FriendsConfig(file: File) : FileConfig(file) {
    val friends = mutableListOf<Friend>()

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        clearFriends()

        friends.addAll(PRETTY_GSON.fromJson(file.bufferedReader(), object : TypeToken<List<Friend>>() {}.type))
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig() = file.writeText(PRETTY_GSON.toJson(friends))

    /**
     * Add friend to config
     *
     * @param playerName of friend
     * @param alias      of friend
     * @return of successfully added friend
     */
    fun addFriend(playerName: String, alias: String = playerName): Boolean {
        if (isFriend(playerName)) return false

        friends += Friend(playerName, alias)
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
    data class Friend(val playerName: String, val alias: String)
}