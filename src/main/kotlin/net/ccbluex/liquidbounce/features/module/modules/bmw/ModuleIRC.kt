package net.ccbluex.liquidbounce.features.module.modules.bmw

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.bmw.*
import net.ccbluex.liquidbounce.event.events.ChatSendEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.dropPort
import net.ccbluex.liquidbounce.utils.client.inGame
import okhttp3.*

object ModuleIRC : Module("IRC", Category.BMW) {

    private val reconnectForFailureDelay by int("ReconnectForFailureDelay", 20, 1..100, "ticks")

    private var webSocket: WebSocket? = null
    private var connected = false
    private var ticks = -1

    val gameTickEventHandler = handler<GameTickEvent> {
        if (connected || !inGame || mc.currentScreen == null) {
            return@handler
        }
        if (ticks != -1 && ticks < reconnectForFailureDelay) {
            ticks++
            return@handler
        }
        ticks = 0
        connected = true

        val client = OkHttpClient()
        val request = Request.Builder().url(BMW_IP).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                notifyAsMessage("IRC连接服务器成功")
                var messageJson = JsonObject()
                messageJson.addProperty("func", "create_user")
                messageJson.addProperty("server", network.connection.address.toString().dropPort())
                messageJson.addProperty("name", player.name.literalString!!)
                webSocket.send(messageJson.toString())

                messageJson = JsonObject()
                messageJson.addProperty("func", "get_free")
                webSocket.send(messageJson.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val messageJson = JsonParser.parseString(text).asJsonObject
                when (messageJson.get("func").asString) {
                    "get_free" -> {
                        LiquidBounce.free = messageJson.get("free").asBoolean
                    }
                    "send_msg" -> {
                        notifyAsMessage("§aIRC §f| §a${messageJson.get("name").asString}§f: ${messageJson.get("msg").asString}")
                    }
                    "create_user" -> {
                        val name = messageJson.get("name").asString
                        FriendManager.friends.add(FriendManager.Friend(name, "§a[BMW] §f${name}"))
                    }
                    "remove_user" -> {
                        val name = messageJson.get("name").asString
                        FriendManager.friends.remove(FriendManager.Friend(name, "§a[BMW] §f${name}"))
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                connected = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (response == null) {
                    notifyAsMessage("IRC连接服务器失败")
                } else {
                    notifyAsMessage("IRC与服务器断开连接，状态码：${response.code}")
                }
                connected = false
            }
        })
    }

    val chatSendEventHandler = handler<ChatSendEvent> { event ->
        if (event.message.trimStart()[0] != '#') {
            return@handler
        }
        event.cancelEvent()
        if (webSocket == null) {
            notifyAsMessage("IRC发送消息失败，原因：暂未连接服务器")
            return@handler
        }
        val msg = event.message.trimStart().substring(1).trim()
        if (msg.isEmpty()) {
            notifyAsMessage("IRC发送消息失败，原因：内容为空")
            return@handler
        }
        val messageJson = JsonObject()
        messageJson.addProperty("func", "send_msg")
        messageJson.addProperty("msg", msg)
        webSocket!!.send(messageJson.toString())
    }

    override fun enable() {
        connected = false
        ticks = -1
    }

    override fun disable() {
        notifyAsMessage("请勿关闭IRC，否则会重置连接")
        if (webSocket != null) {
            webSocket!!.close(1000, null)
            webSocket = null
        }
    }

}
