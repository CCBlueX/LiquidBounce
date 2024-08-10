package net.ccbluex.liquidbounce.features.module.modules.bmw

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.bmw.*
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.io.HttpClient
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

object ModuleIRC : Module("IRC", Category.BMW) {

    private val headers = arrayOf("Content-Type" to "application/json")
    private var ticks = 0

    fun getBMWClientUsers() : JsonArray? {
        val serverAddress = network.connection.address.toString()

        val inputData = JsonObject()
        inputData.addProperty("serverAddress", serverAddress)
        inputData.addProperty("displayName", "ShootForever")

        try {
            val (code, result) = HttpClient.requestWithCode(
                "$SHOOTFOREVER_IP/getBMWClientUsers",
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
        if (ticks % 20 == 0 && inGame && mc.currentScreen != null) {
            thread {
                getBMWClientUsers() ?: return@thread
                notifyAsMessage("别做梦了，IRC还没写完，LLL")
            }
        }
        ticks++
    }

    override fun enable() {
        ticks = 0
    }

}
