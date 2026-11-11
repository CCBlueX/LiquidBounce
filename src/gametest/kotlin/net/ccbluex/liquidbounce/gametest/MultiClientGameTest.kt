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
import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.features.account.CrackedAccount
import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.features.client.ClientContext
import net.ccbluex.liquidbounce.features.client.ClientManager
import net.ccbluex.liquidbounce.features.client.ClientOverview
import net.ccbluex.liquidbounce.features.client.ClientSnapshot
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext
import net.minecraft.server.MinecraftServer
import net.minecraft.world.phys.Vec3
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.util.Properties
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentLinkedQueue
import javax.imageio.ImageIO

private const val JOIN_TIMEOUT_TICKS = 30 * 20
private const val WALK_TICKS = 60
private const val SWITCHES = 20
private const val SOAK_TICKS = 200
private const val VIEW_TIMEOUT_TICKS = 60 * 20
private const val FRAME_INTERVAL_MILLIS = 100L
private const val ANSWER_TIMEOUT_TICKS = 10 * 20
private const val QUIET_TICKS = 40

private const val WALKED_DISTANCE = 2.0
private const val STOOD_DISTANCE = 0.1

/**
 * Three accounts play on one server from one client. Each is a client with its own connection and its own
 * modules, the window goes from one to the other, and none of them loses its connection over it. A client in the
 * background shows what it sees, and the ClickGUI edits its modules over the interop server.
 */
class MultiClientGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        context.createServer().use { server ->
            server.connect().use { connection ->
                connection.waitForChunksDownload()
                val address = server.address

                val main = ClientManager.main
                val second = context.openClient("Second", address)
                val third = context.openClient("Third", address)
                server.waitFor({ it.playerList.playerCount == 3 }, JOIN_TIMEOUT_TICKS)
                context.waitFor({ second.inGame && third.inGame }, JOIN_TIMEOUT_TICKS)

                // Players standing in each other would be pushed around
                server.runCommand("execute as Second at @s run tp @s ~6 ~ ~")
                server.runCommand("execute as Third at @s run tp @s ~-6 ~ ~")
                context.waitTicks(20)

                checkModulesBelongToTheirClient(context, main, second, third)
                checkOnlyTheWalkingClientMoves(context, server, second)
                checkSwitching(context, listOf(main, second, third))
                checkView(context, server, third)
                checkInterop(context, main, third)
                checkStillPlaying(context, server, listOf(main, second, third))
                checkClosing(context, server, second)
            }
        }
    }

    private fun checkModulesBelongToTheirClient(
        context: ClientGameTestContext,
        main: Client,
        second: Client,
        third: Client,
    ) {
        val before = context.sprint(main)
        context.onClient {
            ClientContext.within(second) {
                ModuleSprint.mode().setByString("Omnidirectional")
                ModuleSprint.enabled = true
            }
        }

        check(context.sprint(second) == (true to "Omnidirectional")) {
            "Sprint of the second client: ${context.sprint(second)}"
        }
        check(context.sprint(main) == before) {
            "Sprint of the main client changed from $before to ${context.sprint(main)}"
        }
        check(!context.sprint(third).first) { "Sprint of the third client was turned on with the second" }
    }

    private fun checkOnlyTheWalkingClientMoves(
        context: ClientGameTestContext,
        server: TestDedicatedServerContext,
        walking: Client,
    ) {
        val start = server.positions()
        context.onClient { ClientContext.within(walking) { ModuleAutoWalk.enabled = true } }
        context.waitTicks(WALK_TICKS)

        val snapshot = context.fromClient { ClientOverview.snapshot(walking) }
        check("AutoWalk" in snapshot.modules && snapshot.activity == ClientSnapshot.Activity.MOVING) {
            "The walking client is described as ${snapshot.activity} with ${snapshot.modules}"
        }
        context.onClient { ClientContext.within(walking) { ModuleAutoWalk.enabled = false } }

        val walker = context.name(walking)
        for ((name, position) in server.positions()) {
            val moved = position.distanceTo(start.getValue(name))
            if (name == walker) {
                check(moved > WALKED_DISTANCE) { "$name only walked $moved blocks" }
            } else {
                check(moved < STOOD_DISTANCE) { "$name moved $moved blocks although only $walker walks" }
            }
        }
    }

    private fun checkSwitching(context: ClientGameTestContext, clients: List<Client>) {
        // Ends on the main client, which is the one the test framework disconnects
        for (switch in SWITCHES downTo 0) {
            val client = clients[switch % clients.size]
            context.onClient { ClientManager.activate(client) }
            context.waitFor { it.player != null && it.player === client.player }

            val name = context.name(client)
            val shown = context.fromClient { it.player!!.gameProfile.name to it.user.name }
            check(client.isActive && shown == (name to name)) { "The window shows $shown after switching to $name" }
        }
    }

    /**
     * The view of [viewer], a client in the background, shows its own world and keeps up with it: a red wall built in
     * front of it while it is watched shows up, while the window's client looks the other way. The active client has
     * no view, it is the window.
     */
    private fun checkView(context: ClientGameTestContext, server: TestDedicatedServerContext, viewer: Client) {
        val y = server.groundY()
        val name = context.fromClient { viewer.user.name }
        server.runCommand("time set noon")
        server.runCommand("execute as @a[name=!$name] at @s run tp @s ~ ~ ~ 90 0")
        server.runCommand("tp $name 17.5 $y 0.5 -90 0")

        val id = context.fromClient { viewer.id }
        FrameStream(id).use { frames ->
            context.waitFor({ frames.count > 0 }, VIEW_TIMEOUT_TICKS)
            server.runCommand("fill 20 $y -6 20 ${y + 12} 6 red_concrete")

            repeat(VIEW_TIMEOUT_TICKS) {
                if (frames.latest?.isMostlyRed() == true) {
                    return@use
                }
                context.waitTick()
            }
            error("The view never showed the red wall, ${frames.count} frames, last ${frames.latest}")
        }

        val status = HttpClient.newHttpClient()
            .send(viewRequest(ClientManager.main.id), HttpResponse.BodyHandlers.discarding())
            .statusCode()
        check(status == 404) { "The active client answered its view with $status" }
    }

    /**
     * The ClickGUI edits the modules of [background], a client in the background, through the interop server, and
     * follows them over the web socket. Sockets that did not ask for every client hear nothing of it.
     */
    private fun checkInterop(context: ClientGameTestContext, main: Client, background: Client) {
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

    private fun checkStillPlaying(
        context: ClientGameTestContext,
        server: TestDedicatedServerContext,
        clients: List<Client>,
    ) {
        context.waitTicks(SOAK_TICKS)
        check(server.positions().size == clients.size && context.fromClient { clients.all { it.inGame } }) {
            "Not every client is still playing: ${server.positions().keys}"
        }
    }

    private fun checkClosing(
        context: ClientGameTestContext,
        server: TestDedicatedServerContext,
        client: Client,
    ) {
        context.onClient { ClientManager.close(client) }
        val name = context.name(client)
        server.waitFor({ it.playerList.getPlayerByName(name) == null }, JOIN_TIMEOUT_TICKS)

        check(context.fromClient { client !in ClientManager.clients && !client.inGame }) {
            "The closed client is still around"
        }
    }

}

/**
 * A dedicated server that further clients of this game can join as offline accounts.
 */
private fun ClientGameTestContext.createServer(): TestDedicatedServerContext =
    worldBuilder().createServer(Properties().apply {
        setProperty("online-mode", "false")
        setProperty("white-list", "false")
        setProperty("max-players", "8")
    })

private val TestDedicatedServerContext.address: String
    get() = "localhost:${computeOnServer<Int, RuntimeException> { it.port }}"

/**
 * Opens a client playing as the offline account [name] and lets it join [address].
 */
private fun ClientGameTestContext.openClient(name: String, address: String): Client = fromClient {
    val account = CrackedAccount(name, false).also { it.refresh() }
    ClientManager.open(AccountManager.authenticate(account)).also { ClientManager.connect(it, address) }
}

private fun ClientGameTestContext.sprint(client: Client) = fromClient {
    ClientContext.within(client) { ModuleSprint.enabled to ModuleSprint.mode().getValue() }
}

private fun ModuleSprint.mode() = containedValues.first { it.name == "Mode" }

private fun ClientGameTestContext.name(client: Client): String = fromClient { client.user.name }

private fun <T> TestDedicatedServerContext.fromServer(block: (MinecraftServer) -> T): T =
    computeOnServer<T, RuntimeException> { block(it) }

private fun TestDedicatedServerContext.positions(): Map<String, Vec3> =
    fromServer { server -> server.playerList.players.associate { it.gameProfile.name to it.position() } }

private class Rgb(val red: Int, val green: Int, val blue: Int) {
    fun isMostlyRed() = red > 2 * green && red > 2 * blue && red > 40

    override fun toString() = "rgb($red, $green, $blue)"
}

/**
 * Asks for frames of [id]'s view on a thread of its own, like a theme page does. The game only runs while the
 * test thread waits for it.
 */
private class FrameStream(id: Int) : AutoCloseable {

    @Volatile
    var latest: Rgb? = null
        private set

    @Volatile
    var count = 0
        private set

    @Volatile
    private var running = true

    init {
        val http = HttpClient.newHttpClient()
        Thread({
            while (running) {
                val response = http.send(viewRequest(id), HttpResponse.BodyHandlers.ofByteArray())
                when (response.statusCode()) {
                    200 -> {
                        check(response.headers().firstValue("Content-Encoding").isEmpty) { "The view got compressed" }
                        latest = averageOf(response.body())
                        count++
                    }

                    204 -> Unit
                    else -> error("The view answered ${response.statusCode()}")
                }
                Thread.sleep(FRAME_INTERVAL_MILLIS)
            }
        }, "View reader").apply { isDaemon = true }.start()
    }

    private fun averageOf(jpeg: ByteArray): Rgb {
        val image = ImageIO.read(ByteArrayInputStream(jpeg))
        var red = 0L
        var green = 0L
        var blue = 0L
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                red += rgb shr 16 and 0xFF
                green += rgb shr 8 and 0xFF
                blue += rgb and 0xFF
            }
        }
        val pixels = image.width * image.height
        return Rgb((red / pixels).toInt(), (green / pixels).toInt(), (blue / pixels).toInt())
    }

    override fun close() {
        running = false
    }

}

private fun viewRequest(id: Int): HttpRequest = HttpRequest.newBuilder()
    .uri(URI("${ClientInteropServer.url}/api/v1/client/clients/$id/view?lb_code=${ClientInteropServer.AUTH_CODE}"))
    .header("Accept-Encoding", "gzip")
    .build()

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
