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

    private val headers = arrayOf("Content-Type" to "application/json")
    private var ticks = 0
    private var users = listOf<String>()

    private fun connect(
        function: String,
        inputData: JsonElement? = null
    ): JsonElement? {
        try {
            val (code, result) = HttpClient.requestWithCode(
                "$SHOOTFOREVER_IP/$function",
                "POST",
                headers = headers,
                inputData = inputData.toString().toByteArray()
            )

            if (code != 200) {
                notifyAsMessage("IRC获取BMWClient用户列表失败")
                return null
            }

            return JsonParser.parseString(result)

        } catch (error: SocketTimeoutException) {
            notifyAsMessage("IRC连接服务器失败")
            return null
        }
    }

    val gameTickEventHandler = handler<GameTickEvent> {
        if (ticks >= 20 && inGame && mc.currentScreen != null) {
            ticks = 0
            thread {
                // GetUsers
                val getUsersInputData = JsonObject()
                getUsersInputData.addProperty("address", network.connection.address.toString().dropPort())
                getUsersInputData.addProperty("name", player.name.literalString!!)
                val getUsersResult = connect("getUsers", getUsersInputData) ?: return@thread
                val usersCopy = users.toMutableList()
                val deleteUsers = usersCopy.toMutableList()
                getUsersResult.asJsonArray.forEach {
                    val name = it.asString
                    if (name in usersCopy) {
                        deleteUsers.remove(name)
                        return@forEach
                    }
                    if (!FriendManager.isFriend(name)) {
                        FriendManager.friends.add(FriendManager.Friend(name, null))
                    }
                    usersCopy.add(name)
                }
                deleteUsers.forEach {
                    if (FriendManager.isFriend(it)) {
                        FriendManager.friends.remove(FriendManager.Friend(it, null))
                    }
                    usersCopy.remove(it)
                }
                users = usersCopy.toList()

                // GetMessages
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
        // SendMessage
        if (event.message.trimStart()[0] != '#') {
            return@handler
        }
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
        ticks = 0
        users = listOf()
    }

}
