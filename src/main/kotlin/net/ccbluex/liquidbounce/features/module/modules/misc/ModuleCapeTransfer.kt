/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.misc

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.cosmetic.CapeService
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.client.util.Session
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPatch
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import kotlin.concurrent.thread

/**
 * Transfers donator capes from one account to another.
 */
object ModuleCapeTransfer : Module("CapeTransfer", Category.MISC) {

    init {
        // Hooks configurable of cape service into the module
        tree(CapeService)
    }

    override fun enable() {
        if (CapeService.clientCapeUser == null) {
            if (CapeService.knownToken.isNotBlank()) {
                // Login into cape service
                CapeService.login(CapeService.knownToken)
            } else {
                chat("§cPlease login into the cape service first.")
                enabled = false
                return
            }
        }

        transferCape()


        super.enable()
    }

    /**
     * We want to immediately update the owner of the cape and refresh the cape carriers
     */
    val onSession = handler<SessionEvent> {
        transferCape()
    }

    private fun transferCape() {
        // Check if donator cape is actually enabled and has a transfer code, also make sure the account
        // used is premium.
        val capeUser = CapeService.clientCapeUser ?: return

        if (mc.session.accountType != Session.AccountType.LEGACY || mc.session.accessToken.length < 2)
            return

        thread(name = "CapeUpdate") {
            // Apply cape to new account
            val uuid = mc.session.uuid
            val username = mc.session.username

            val httpClient = HttpClients.createDefault()
            val headers = arrayOf(
                BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
                BasicHeader(HttpHeaders.AUTHORIZATION, capeUser.token)
            )
            val request = HttpPatch(CapeService.SELF_CAPE_URL)
            request.setHeaders(headers)

            val body = JsonObject()
            body.addProperty("uuid", uuid)
            request.entity = StringEntity(Gson().toJson(body))

            val response = httpClient.execute(request)
            val statusCode = response.statusLine.statusCode

            if (statusCode == HttpStatus.SC_NO_CONTENT) {
                capeUser.uuid = uuid
                logger.info("[Donator Cape] Successfully transferred cape to $uuid ($username)")

                // Refresh cape carriers
                CapeService.refreshCapeCarriers(force = true) {
                    logger.info("Successfully loaded ${CapeService.capeCarriers.count()} cape carriers.")

                    chat("§aSuccessfully transferred cape to your account.")
                    chat("§aPlease rejoin the server to see your cape.")
                }
            } else {
                logger.info("[Donator Cape] Failed to transfer cape ($statusCode)")
                chat("§cFailed to transfer cape to your account.")
            }
        }
    }

}
