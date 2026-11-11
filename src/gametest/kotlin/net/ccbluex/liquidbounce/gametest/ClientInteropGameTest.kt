/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.gametest

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.features.client.ClientContext
import net.ccbluex.liquidbounce.features.client.ClientManager
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentLinkedQueue

private const val JOIN_TIMEOUT_TICKS = 30 * 20
private const val ANSWER_TIMEOUT_TICKS = 10 * 20
private const val QUIET_TICKS = 40

/**
 * The ClickGUI edits the modules of a client in the background through the interop server, and follows them
 * over the web socket. Sockets that did not ask for every client hear nothing of it.
 */
class ClientInteropGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        context.createMultiClientServer().use { server ->
            server.connect().use { connection ->
                connection.waitForChunksDownload()
                val address = server.address

                val main = ClientManager.main
                val second = context.openClient("Second", address)
                context.waitFor({ second.inGame }, JOIN_TIMEOUT_TICKS)

                val interop = Interop(context)
                val everyClient = interop.socket(everyClient = true)
                val activeOnly = interop.socket(everyClient = false)

                checkEditingTheBackgroundClient(context, interop, main, second)
                checkSocketEvents(context, interop, main, second, everyClient, activeOnly)

                everyClient.close()
                activeOnly.close()

                context.onClient { ClientManager.close(second) }
                server.waitFor({ it.playerList.playerCount == 1 }, JOIN_TIMEOUT_TICKS)
            }
        }
    }

    private fun checkEditingTheBackgroundClient(
        context: ClientGameTestContext,
        interop: Interop,
        main: Client,
        second: Client,
    ) {
        val before = context.sprint(main)

        interop.request("POST", "/modules/toggle?client=${second.id}", """{"name":"Sprint"}""")
        check(context.sprint(second).first) { "Sprint of the second client is still off" }
        check(context.sprint(main) == before) { "Sprint of the main client changed to ${context.sprint(main)}" }

        val modules = JsonParser.parseString(interop.request("GET", "/modules?client=${second.id}")).asJsonArray
        val listed = modules.map { it.asJsonObject }.first { it["name"].asString == "Sprint" }
        check(listed["enabled"].asBoolean) { "GET /modules of the second client lists Sprint as $listed" }

        val settings = interop.request("GET", "/modules/settings?name=Sprint&client=${second.id}")
        val changed = JsonParser.parseString(settings).asJsonObject.apply {
            getAsJsonArray("value").map { it.asJsonObject }.first { it["name"].asString == "Mode" }
                .addProperty("value", "Omnidirectional")
        }
        interop.request("PUT", "/modules/settings?name=Sprint&client=${second.id}", changed.toString())

        check(context.sprint(second) == (true to "Omnidirectional")) { "Second client: ${context.sprint(second)}" }
        check(context.sprint(main) == before) { "Sprint of the main client changed to ${context.sprint(main)}" }
    }

    @Suppress("LongParameterList")
    private fun checkSocketEvents(
        context: ClientGameTestContext,
        interop: Interop,
        main: Client,
        second: Client,
        everyClient: Socket,
        activeOnly: Socket,
    ) {
        context.waitFor({ everyClient.has("moduleToggle", second.id, background = true) }, ANSWER_TIMEOUT_TICKS)

        // The main client is the active one, so its toggles reach both sockets as before
        interop.request("POST", "/modules/toggle", """{"name":"Sprint"}""")
        context.waitFor({
            everyClient.has("moduleToggle", main.id, background = false) &&
                activeOnly.has("moduleToggle", main.id, background = false)
        }, ANSWER_TIMEOUT_TICKS)
        interop.request("POST", "/modules/toggle", """{"name":"Sprint"}""")
        context.waitTicks(QUIET_TICKS)

        check(activeOnly.messages.none { it["client"]?.asInt == second.id || it.has("background") }) {
            "A socket that only follows the active client heard of another: ${activeOnly.messages}"
        }
    }

}

private fun ClientGameTestContext.sprint(client: Client) = fromClient {
    ClientContext.within(client) {
        ModuleSprint.enabled to ModuleSprint.containedValues.first { it.name == "Mode" }.getValue()
    }
}

/**
 * Talks to the interop server like the theme does. The game only answers while the test waits for it.
 */
private class Interop(private val context: ClientGameTestContext) {

    private val http = HttpClient.newHttpClient()

    fun request(method: String, path: String, body: String? = null): String {
        val separator = if ('?' in path) '&' else '?'
        val code = "lb_code=${ClientInteropServer.AUTH_CODE}"
        val uri = URI("${ClientInteropServer.url}/api/v1/client$path$separator$code")
        val request = HttpRequest.newBuilder(uri)
            .method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
            .header("Content-Type", "application/json")
            .build()

        val response = await(http.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
        check(response.statusCode() in 200..299) {
            "$method $path answered ${response.statusCode()}: ${response.body()}"
        }
        return response.body()
    }

    fun socket(everyClient: Boolean): Socket {
        val socket = Socket()
        val query = if (everyClient) "?clients=all" else ""
        socket.webSocket = await(
            http.newWebSocketBuilder()
                .header("Cookie", "lb_auth=${ClientInteropServer.AUTH_CODE}")
                .buildAsync(URI("${ClientInteropServer.url.replace("http", "ws")}/$query"), socket)
        )
        return socket
    }

    private fun <T> await(future: CompletableFuture<T>): T {
        context.waitFor({ future.isDone }, ANSWER_TIMEOUT_TICKS)
        return future.join()
    }

}

private class Socket : WebSocket.Listener {

    lateinit var webSocket: WebSocket
    val messages = ConcurrentLinkedQueue<JsonObject>()
    private val partial = StringBuilder()

    fun has(name: String, client: Int, background: Boolean) = messages.any {
        it["name"].asString == name && it["client"]?.asInt == client && it.has("background") == background
    }

    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
        partial.append(data)
        if (last) {
            messages += JsonParser.parseString(partial.toString()).asJsonObject
            partial.setLength(0)
        }
        webSocket.request(1)
        return null
    }

    fun close() {
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "").join()
    }

}
