/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.special

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.GuiDonatorCape
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.login.UserUtils
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPatch
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.json.JSONObject
import kotlin.concurrent.thread

class DonatorCape : Listenable, MinecraftInstance() {

    @EventTarget
    fun onSession(event: SessionEvent) {
        if (!GuiDonatorCape.capeEnabled || GuiDonatorCape.transferCode.isEmpty() ||
                !UserUtils.isValidTokenOffline(mc.session.token))
            return

        thread {
            val uuid = mc.session.playerId
            val username = mc.session.username

            val httpClient = HttpClients.createDefault()
            val headers = arrayOf(
                    BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
                    BasicHeader(HttpHeaders.AUTHORIZATION, GuiDonatorCape.transferCode)
            )
            val request = HttpPatch("http://capes.liquidbounce.net/api/v1/cape/self")
            request.setHeaders(headers)

            val body = JSONObject()
            body.put("uuid", uuid)
            request.entity = StringEntity(body.toString())

            val response = httpClient.execute(request)
            val statusCode = response.statusLine.statusCode

            ClientUtils.getLogger().info(
                    if(statusCode == HttpStatus.SC_NO_CONTENT)
                        "[Donator Cape] Successfully transferred cape to $uuid ($username)"
                    else
                        "[Donator Cape] Failed to transfer cape ($statusCode)"
            )
        }
    }

    override fun handleEvents() = true
}