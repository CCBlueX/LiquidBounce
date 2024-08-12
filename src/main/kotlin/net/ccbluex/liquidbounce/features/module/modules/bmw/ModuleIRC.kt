package net.ccbluex.liquidbounce.features.module.modules.bmw

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.bmw.*
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.ChatSendEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.dropPort
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.io.HttpClient
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

object ModuleIRC : Module("IRC", Category.BMW) {

    private val connectDelay by float("ConnectDelay", 1f, 0.5f..5f, "seconds")

    private val headers = arrayOf("Content-Type" to "application/json")
    private var ticks = 0
    private var users = listOf<String>()

    fun connect(
        function: String,
        inputData: JsonElement = JsonObject(),
        notify: Boolean = true
    ): JsonElement? {
        try {
            val (code, result) = HttpClient.requestWithCode(
                "$SHOOTFOREVER_IP/$function",
                "POST",
                headers = headers,
                inputData = inputData.toString().toByteArray()
            )
            val resultJson = JsonParser.parseString(result)
            if (code != 200) {
                if (notify) {
                    notifyAsMessage("IRC连接服务器失败，状态码：$code，原因：${
                        if (resultJson.asJsonObject.isEmpty) { "未知" }
                        else { resultJson.asJsonObject.get("reason").asString }
                    }")
                }
                return null
            }
            return resultJson

        } catch (error: SocketTimeoutException) {
            if (notify) {
                notifyAsMessage("IRC连接服务器失败，原因：服务器未开启")
            }
            return null
        }
    }

    val gameTickEventHandler = handler<GameTickEvent> {
        if (ticks >= connectDelay && inGame && mc.currentScreen != null) {
            ticks = 0
            thread {
                val getUsersInputData = JsonObject()
                getUsersInputData.addProperty("address", network.connection.address.toString().dropPort())
                getUsersInputData.addProperty("name", player.name.literalString!!)
                val getUsersResult = connect("getUsers", getUsersInputData) ?: return@thread
                val usersCopy = users.toMutableList()
                getUsersResult.asJsonArray.forEach {
                    val name = it.asString
                    if (!FriendManager.isFriend(name)) {
                        FriendManager.friends.add(FriendManager.Friend(name, null))
                    }
                    if (!usersCopy.contains(name)) {
                        usersCopy.add(name)
                    }
                }
                users = usersCopy.toList()

                val getMessagesInputData = JsonObject()
                getMessagesInputData.addProperty("name", player.name.literalString!!)
                val getMessagesResult = connect("getMessages", getMessagesInputData) ?: return@thread
                getMessagesResult.asJsonArray.forEach {
                    val message = it.asJsonObject
                    notifyAsMessage("§3IRC §f| §3${message.get("name").asString}§f: ${message.get("content").asString}")
                }
            }
        }
        ticks++
    }

    val chatSendEventHandler = handler<ChatSendEvent> { event ->
        if (event.message.trimStart()[0] != '#') {
            return@handler
        }
        event.cancelEvent()
        val message = event.message.trimStart().substring(1).trim()
        if (message.isEmpty()) {
            return@handler
        }
        val inputData = JsonObject()
        inputData.addProperty("name", player.name.literalString!!)
        inputData.addProperty("content", message)
        connect("sendMessage", inputData)
    }

    override fun enable() {
        ticks = 10
        users = listOf()
    }

}
