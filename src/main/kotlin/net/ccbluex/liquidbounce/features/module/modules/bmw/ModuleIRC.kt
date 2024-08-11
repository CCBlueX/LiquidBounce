package net.ccbluex.liquidbounce.features.module.modules.bmw

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.bmw.*
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.io.HttpClient
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

object ModuleIRC : Module("IRC", Category.BMW) {

    private val headers = arrayOf("Content-Type" to "application/json")
    private var ticks = 0
    private val users = mutableListOf<String>()
    private var ok = false

    private fun getBMWClientUsers() : JsonArray? {
        val inputData = JsonObject()
        inputData.addProperty("address", network.connection.address.toString())
        inputData.addProperty("name", player.name.literalString!!)

        try {
            val (code, result) = HttpClient.requestWithCode(
                "$SHOOTFOREVER_IP/irc",
                "POST",
                headers = headers,
                inputData = inputData.toString().toByteArray()
            )

            if (code != 200) {
                notifyAsMessage("IRC获取BMWClient用户列表失败")
                return null
            }

            return JsonParser.parseString(result).asJsonArray

        } catch (error: SocketTimeoutException) {
            notifyAsMessage("IRC连接服务器失败")
            return null
        }
    }

    val gameTickEventHandler = handler<GameTickEvent> {
        if (ok && ticks >= 20 && inGame && mc.currentScreen != null) {
            ticks = 0
            ok = false
            thread {
                val result = getBMWClientUsers() ?: return@thread
                val usersCopy = users.toMutableList()
                result.forEach {
                    val name = it.asString
                    if (name in users) {
                        usersCopy.remove(name)
                        return@forEach
                    }
                    if (!FriendManager.isFriend(name)) {
                        FriendManager.friends.add(FriendManager.Friend(name, null))
                    }
                    users.add(name)
                }
                usersCopy.forEach {
                    if (FriendManager.isFriend(it)) {
                        FriendManager.friends.remove(FriendManager.Friend(it, null))
                    }
                    users.remove(it)
                }
                ok = true
            }
        }
        ticks++
    }

    override fun enable() {
        ticks = 0
        ok = false
        users.clear()
    }

}
