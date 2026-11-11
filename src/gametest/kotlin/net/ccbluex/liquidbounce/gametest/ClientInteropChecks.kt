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
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentLinkedQueue

private const val ANSWER_TIMEOUT_TICKS = 10 * 20
private const val QUIET_TICKS = 40

/**
 * The ClickGUI edits the modules of [background], a client in the background, through the interop server, and
 * follows them over the web socket. Sockets that did not ask for every client hear nothing of it.
 */
internal fun checkClientInterop(context: ClientGameTestContext, main: Client, background: Client) {
    val interop = Interop(context)
    val everyClient = interop.socket(everyClient = true)
    val activeOnly = interop.socket(everyClient = false)

    checkEditingTheBackgroundClient(context, interop, main, background)
    checkSocketEvents(context, interop, main, background, everyClient, activeOnly)

    everyClient.close()
    activeOnly.close()
}

private fun checkEditingTheBackgroundClient(
    context: ClientGameTestContext,
    interop: Interop,
    main: Client,
    background: Client,
) {
    val before = context.sprint(main)

    interop.request("POST", "/modules/toggle?client=${background.id}", """{"name":"Sprint"}""")
    check(context.sprint(background).first) { "Sprint of the background client is still off" }
    check(context.sprint(main) == before) { "Sprint of the main client changed to ${context.sprint(main)}" }

    val modules = JsonParser.parseString(interop.request("GET", "/modules?client=${background.id}")).asJsonArray
    val listed = modules.map { it.asJsonObject }.first { it["name"].asString == "Sprint" }
    check(listed["enabled"].asBoolean) { "GET /modules of the background client lists Sprint as $listed" }

    val settings = interop.request("GET", "/modules/settings?name=Sprint&client=${background.id}")
    val changed = JsonParser.parseString(settings).asJsonObject.apply {
        getAsJsonArray("value").map { it.asJsonObject }.first { it["name"].asString == "Mode" }
            .addProperty("value", "Omnidirectional")
    }
    interop.request("PUT", "/modules/settings?name=Sprint&client=${background.id}", changed.toString())

    check(context.sprint(background) == (true to "Omnidirectional")) {
        "Background client: ${context.sprint(background)}"
    }
    check(context.sprint(main) == before) { "Sprint of the main client changed to ${context.sprint(main)}" }

    // Fly only runs in the main client, so another client has no copy of it to change
    val refused = interop.status("POST", "/modules/toggle?client=${background.id}", """{"name":"Fly"}""")
    check(refused == 400) { "Toggling Fly of the background client answered $refused" }
}

@Suppress("LongParameterList")
private fun checkSocketEvents(
    context: ClientGameTestContext,
    interop: Interop,
    main: Client,
    background: Client,
    everyClient: Socket,
    activeOnly: Socket,
) {
    context.waitFor({ everyClient.has("moduleToggle", background.id, background = true) }, ANSWER_TIMEOUT_TICKS)

    // The main client is the active one, so its toggles reach both sockets as before
    interop.request("POST", "/modules/toggle", """{"name":"Sprint"}""")
    context.waitFor({
        everyClient.has("moduleToggle", main.id, background = false) &&
            activeOnly.has("moduleToggle", main.id, background = false)
    }, ANSWER_TIMEOUT_TICKS)
    interop.request("POST", "/modules/toggle", """{"name":"Sprint"}""")
    context.waitTicks(QUIET_TICKS)

    check(activeOnly.messages.none { it["client"]?.asInt == background.id || it.has("background") }) {
        "A socket that only follows the active client heard of another: ${activeOnly.messages}"
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
        val response = send(method, path, body)
        check(response.statusCode() in 200..299) {
            "$method $path answered ${response.statusCode()}: ${response.body()}"
        }
        return response.body()
    }

    fun status(method: String, path: String, body: String? = null) = send(method, path, body).statusCode()

    private fun send(method: String, path: String, body: String?): HttpResponse<String> {
        val separator = if ('?' in path) '&' else '?'
        val code = "lb_code=${ClientInteropServer.AUTH_CODE}"
        val uri = URI("${ClientInteropServer.url}/api/v1/client$path$separator$code")
        val request = HttpRequest.newBuilder(uri)
            .method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
            .header("Content-Type", "application/json")
            .build()
        return await(http.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
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
