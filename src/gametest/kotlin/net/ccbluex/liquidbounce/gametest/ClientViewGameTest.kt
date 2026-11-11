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

import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.features.account.CrackedAccount
import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.features.client.ClientManager
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Properties
import javax.imageio.ImageIO

private const val JOIN_TIMEOUT_TICKS = 30 * 20
private const val VIEW_TIMEOUT_TICKS = 60 * 20

/**
 * A background client's view shows its own world: it stands in front of a red wall, the window's client does not.
 */
class ClientViewGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        val properties = Properties().apply {
            setProperty("online-mode", "false")
            setProperty("white-list", "false")
            setProperty("max-players", "8")
        }
        context.worldBuilder().createServer(properties).use { server ->
            server.connect().use { connection ->
                connection.waitForChunksDownload()
                val y = server.groundY()
                val address = "localhost:${server.computeOnServer<Int, RuntimeException> { it.port }}"

                server.runCommand("time set noon")
                server.runCommand("fill 20 $y -6 20 ${y + 12} 6 red_concrete")
                server.runCommand("tp @a -20.5 $y 0.5 90 0")

                val viewer = context.openClient("Viewer", address)
                server.waitFor({ it.playerList.playerCount == 2 }, JOIN_TIMEOUT_TICKS)
                context.waitFor({ viewer.inGame }, JOIN_TIMEOUT_TICKS)
                server.runCommand("tp Viewer 17.5 $y 0.5 -90 0")

                val id = context.fromClient { viewer.id }
                FrameStream(id).use { frames ->
                    repeat(VIEW_TIMEOUT_TICKS) {
                        if (frames.latest?.isMostlyRed() == true) {
                            return@use
                        }
                        context.waitTick()
                    }
                    frames.lastJpeg?.let { File("view-last.jpg").writeBytes(it) }
                    error("The view never showed the red wall, ${frames.count} frames, last ${frames.latest}")
                }

                val status = HttpClient.newHttpClient()
                    .send(viewRequest(ClientManager.main.id), HttpResponse.BodyHandlers.discarding())
                    .statusCode()
                check(status == 404) { "The active client answered its view with $status" }

                context.onClient { ClientManager.close(viewer) }
                server.waitFor({ it.playerList.playerCount == 1 }, JOIN_TIMEOUT_TICKS)
            }
        }
    }

}

private class Rgb(val red: Int, val green: Int, val blue: Int) {
    fun isMostlyRed() = red > 2 * green && red > 2 * blue && red > 40

    override fun toString() = "rgb($red, $green, $blue)"
}

/**
 * Reads the stream of [id]'s view on a thread of its own, the game only runs while the test thread waits for it.
 */
private class FrameStream(id: Int) : AutoCloseable {

    private val input: InputStream

    @Volatile
    var latest: Rgb? = null
        private set

    @Volatile
    var count = 0
        private set

    @Volatile
    var lastJpeg: ByteArray? = null
        private set

    init {
        val response = HttpClient.newHttpClient().send(viewRequest(id), HttpResponse.BodyHandlers.ofInputStream())
        check(response.statusCode() == 200) { "The view answered ${response.statusCode()}" }
        check(response.headers().firstValue("Content-Encoding").isEmpty) { "The view stream got compressed" }
        input = response.body()
        Thread({
            runCatching {
                while (true) {
                    latest = next() ?: break
                }
            }
        }, "View stream reader").apply { isDaemon = true }.start()
    }

    private fun next(): Rgb? {
        var length = -1
        while (true) {
            val line = readLine() ?: return null
            if (line.startsWith("Content-Length:", ignoreCase = true)) {
                length = line.substringAfter(':').trim().toInt()
            } else if (line.isEmpty() && length >= 0) {
                break
            }
        }

        val jpeg = input.readNBytes(length)
        lastJpeg = jpeg
        count++
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

    private fun readLine(): String? {
        val line = StringBuilder()
        while (true) {
            when (val byte = input.read()) {
                -1 -> return null
                '\n'.code -> return line.toString().trimEnd('\r')
                else -> line.append(byte.toChar())
            }
        }
    }

    override fun close() = input.close()

}

private fun viewRequest(id: Int): HttpRequest = HttpRequest.newBuilder()
    .uri(URI("${ClientInteropServer.url}/api/v1/client/clients/$id/view?lb_code=${ClientInteropServer.AUTH_CODE}"))
    .header("Accept-Encoding", "gzip")
    .build()

private fun ClientGameTestContext.openClient(name: String, address: String): Client = fromClient {
    val account = CrackedAccount(name, false).also { it.refresh() }
    ClientManager.open(AccountManager.authenticate(account)).also { ClientManager.connect(it, address) }
}
