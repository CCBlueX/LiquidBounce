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

import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.features.client.ClientManager
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.imageio.ImageIO

private const val VIEW_TIMEOUT_TICKS = 60 * 20
private const val FRAME_INTERVAL_MILLIS = 100L

/**
 * The view of [viewer], a client in the background, shows its own world and keeps up with it: a red wall built in
 * front of it while it is watched shows up, while the window's client looks the other way. The active client has
 * no view, it is the window.
 */
internal fun checkClientView(context: ClientGameTestContext, server: TestDedicatedServerContext, viewer: Client) {
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

