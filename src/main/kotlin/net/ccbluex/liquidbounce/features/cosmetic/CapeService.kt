/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.cosmetic

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.authlib.utils.boolean
import net.ccbluex.liquidbounce.authlib.utils.string
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.HttpClient
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPut
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import java.util.*
import kotlin.concurrent.thread

/**
 * A more reliable and stress reduced cape service
 *
 * It will frequently update all carriers of capes into a map with the described cape name.
 * This allows to cache already known capes and store them locally and will more quickly load them.
 *
 * We know this might cause sometimes users to not have their capes
 * shown immediately when account switches, but we can reduce the stress
 * on the API and the connection of the user.
 */
object CapeService : Listenable, Configurable("Cape") {

    /**
     * The client cape user
     */
    var knownToken by text("Token", "")
        .doNotIncludeAlways()

    var clientCapeUser: CapeSelfUser? = null

    /**
     * I would prefer to use CLIENT_API but due to Cloudflare causing issues with SSL and their browser integrity check,
     * we have a separate domain.
     */
    internal const val CAPE_API = "http://capes.liquidbounce.net/api/v1/cape"

    /**
     * The API URL to get all cape carriers.
     * Format: [["8f617b6abea04af58e4bd026d8fa9de8", "marco"], ...]
     */
    private const val CAPE_CARRIERS_URL = "$CAPE_API/carriers"
    const val SELF_CAPE_URL = "$CAPE_API/self"
    private const val CAPE_NAME_DL_BASE_URL = "$CAPE_API/name/%s"

    private const val REFRESH_DELAY = 60000L // Every minute should update

    /**
     * Collection of all cape carriers on the API.
     * We start with an empty list, which will be updated by the refreshCapeCarriers
     * function frequently based on the REFRESH_DELAY.
     */
    internal var capeCarriers = emptyList<CapeCarrier>()
    private val lastUpdate = Chronometer()
    private var task: Thread? = null

    /**
     * Refresh cape carriers, capture from the API.
     * It will take a list of (uuid, cape_name) tuples.
     */
    fun refreshCapeCarriers(force: Boolean = false, done: () -> Unit) {
        // Check if there is not another task running which could conflict.
        if (task == null) {
            // Check if the required time in milliseconds has passed of the REFRESH_DELAY
            if (lastUpdate.hasElapsed(REFRESH_DELAY) || force) {
                task = thread(name = "UpdateCarriersTask") {
                    runCatching {
                        // Capture data from API and parse JSON
                        val json = HttpClient.get(CAPE_CARRIERS_URL)
                        val parsedJson = JsonParser.parseString(json)

                        // Should be a JSON Array. It will fail if not.
                        // Format: [["8f617b6abea04af58e4bd026d8fa9de8", "marco"], ...]
                        val jsonCapeCarriers = parsedJson.asJsonArray.map { objInArray ->
                            // Should be a JSON Array. It will fail if not.
                            val arrayInArray = objInArray.asJsonArray
                            // 1. is UUID 2. is name of cape
                            val (uuid, name) = arrayInArray[0].asString to arrayInArray[1].asString

                            val dashedUuid = Regex(
                                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)"
                            ).replace(uuid, "$1-$2-$3-$4-$5")
                            CapeCarrier(UUID.fromString(dashedUuid), name)
                        }

                        // Update to new carriers
                        capeCarriers = jsonCapeCarriers

                        task = null

                        // Reset timer and start once again
                        lastUpdate.reset()

                        // Call out done
                        mc.execute {
                            done()
                        }
                    }.onFailure {
                        logger.error("Failed to refresh cape carriers due to error.", it)
                    }
                }
            } else {
                // Call out done immediate because there is no refresh required at the moment
                done()
            }
        }
    }

    /**
     * Get the download url to cape of UUID
     */
    fun getCapeDownload(uuid: UUID): Pair<String, String>? {
        val clientCapeUser = clientCapeUser

        if (uuid == mc.session.uuidOrNull && clientCapeUser != null) {
            // If the UUID is the same as the current user, we can use the clientCapeUser
            val capeName = clientCapeUser.capeName
            return capeName to String.format(CAPE_NAME_DL_BASE_URL, capeName)
        }

        // Lookup cape carrier by UUID, if UUID is matching
        val capeCarrier = capeCarriers.find { it.uuid == uuid } ?: return null

        return capeCarrier.capeName to String.format(CAPE_NAME_DL_BASE_URL, capeCarrier.capeName)
    }

    fun login(token: String) {
        if (clientCapeUser != null && clientCapeUser?.token == token) {
            return
        }

        val httpClient = HttpClients.createDefault()
        val headers = arrayOf(
            BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
            BasicHeader(HttpHeaders.AUTHORIZATION, token)
        )

        val request = HttpGet(SELF_CAPE_URL)
        request.setHeaders(headers)

        val response = httpClient.execute(request)
        val statusCode = response.statusLine.statusCode

        if (statusCode == HttpStatus.SC_OK) {
            val json = JsonParser.parseString(EntityUtils.toString(response.entity)).asJsonObject
            val capeName = json.string("cape") ?: error("Failed to get self cape. Cape name is null.")
            val enabled = json.boolean("enabled") ?: error("Failed to get self cape. Enabled is null.")
            val uuid = json.string("uuid") ?: error("Failed to get self cape. UUID is null.")

            clientCapeUser = CapeSelfUser(token, enabled, uuid, capeName)
        } else {
            error("Failed to get self cape. Status code: $statusCode")
        }
    }

    fun logout() {
        clientCapeUser = null
        knownToken = "" // Blank token
    }

    /**
     * Update the cape state of the user
     */
    fun toggleCapeState(done: (Boolean, Boolean, Int) -> Unit) {
        thread {
            val capeUser = clientCapeUser ?: return@thread

            val httpClient = HttpClients.createDefault()
            val headers = arrayOf(
                BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
                BasicHeader(HttpHeaders.AUTHORIZATION, capeUser.token)
            )

            val request = if (!capeUser.enabled) {
                HttpPut(SELF_CAPE_URL)
            } else {
                HttpDelete(SELF_CAPE_URL)
            }
            request.setHeaders(headers)
            val response = httpClient.execute(request)
            val statusCode = response.statusLine.statusCode

            // Refresh cape carriers
            refreshCapeCarriers(force = true) {
                logger.info("Successfully loaded ${capeCarriers.count()} cape carriers.")
            }

            capeUser.enabled = !capeUser.enabled
            done(capeUser.enabled, statusCode == HttpStatus.SC_NO_CONTENT, statusCode)
        }
    }



}

data class CapeSelfUser(val token: String, var enabled: Boolean, var uuid: String, val capeName: String)

data class CapeCarrier(val uuid: UUID, val capeName: String)
