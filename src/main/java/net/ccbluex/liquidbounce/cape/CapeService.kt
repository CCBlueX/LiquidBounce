/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.cape

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.api.ClientApi.API_ENDPOINT
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.login.UserUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.thread

/**
 * A more reliable and stress reduced cape service
 *
 * It will frequently update all carriers of capes into a map with the described cape name.
 * This allows to cache already known capes and store them locally and will more quickly load them.
 *
 * We know this might cause sometimes users to not have their capes shown immediately when account switches, but we can reduce the stress
 * on the API and the connection of the user.
 */
object CapeService : Listenable, MinecraftInstance() {

    /**
     * The client cape user
     */
    var knownToken = ""
        get() = clientCapeUser?.token ?: field

    var clientCapeUser: CapeSelfUser? = null

    /**
     * I would prefer to use CLIENT_API but due to Cloudflare causing issues with SSL and their browser integrity check,
     * we have a separate domain.
     */
    private const val CAPE_API = "http://capes.liquidbounce.net/api/v1/cape"

    /**
     * The API URL to get all cape carriers.
     * Format: [["8f617b6abea04af58e4bd026d8fa9de8", "marco"], ...]
     */
    private const val CAPE_CARRIERS_URL = "$CAPE_API/carriers"

    private const val SELF_CAPE_URL = "$CAPE_API/self"

    @Deprecated("Use CAPE_CARRIERS_URL instead.")
    private const val CAPE_UUID_DL_BASE_URL = "$CAPE_API/uuid/%s"
    private const val CAPE_NAME_DL_BASE_URL = "$CAPE_API/name/%s"

    private const val REFRESH_DELAY = 60000L // Every minute should update

    /**
     * Collection of all cape carriers on the API.
     * We start with an empty list, which will be updated by the refreshCapeCarriers function frequently based on the REFRESH_DELAY.
     */
    internal var capeCarriers = emptyList<CapeCarrier>()
    private var lastUpdate = MSTimer()
    private var task: Thread? = null

    /**
     * Refresh cape carriers, capture from the API.
     * It will take a list of (uuid, cape_name) tuples.
     */
    fun refreshCapeCarriers(force: Boolean = false, done: () -> Unit) {
        // Check if there is not another task running which could conflict.
        if (task == null) {
            // Check if the required time in milliseconds has passed of the REFRESH_DELAY
            if (lastUpdate.hasTimePassed(REFRESH_DELAY) || force) {
                task = thread(name = "UpdateCarriersTask") {
                    runCatching {
                        // Capture data from API and parse JSON
                        val parsedJson = JsonParser().parse(get(CAPE_CARRIERS_URL))

                        // Should be a JSON Array. It will fail if not.
                        // Format: [["8f617b6abea04af58e4bd026d8fa9de8", "marco"], ...]
                        val jsonCapeCarriers = parsedJson.asJsonArray.map { objInArray ->
                            // Should be a JSON Array. It will fail if not.
                            val arrayInArray = objInArray.asJsonArray
                            // 1. is UUID 2. is name of cape
                            val (uuid, name) = arrayInArray[0].asString to arrayInArray[1].asString

                            val dashedUuid = Regex("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)")
                                .replace(uuid, "$1-$2-$3-$4-$5")
                            CapeCarrier(UUID.fromString(dashedUuid), name)
                        }

                        // Update to new carriers
                        capeCarriers = jsonCapeCarriers

                        task = null

                        // Reset timer and start once again
                        lastUpdate.reset()

                        // Call out done
                        mc.addScheduledTask {
                            done()
                        }
                    }.onFailure {
                        LOGGER.error("Failed to refresh cape carriers due to error.", it)
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

        if (uuid == mc.session.profile.id && clientCapeUser != null) {
            // If the UUID is the same as the current user, we can use the clientCapeUser
            val capeName = clientCapeUser.capeName
            return capeName to String.format(CAPE_NAME_DL_BASE_URL, capeName)
        }

        // Lookup cape carrier by UUID, if UUID is matching
        val capeCarrier = capeCarriers.find { it.uuid == uuid } ?: return null

        return capeCarrier.capeName to String.format(CAPE_NAME_DL_BASE_URL, capeCarrier.capeName)
    }

    fun login(token: String) {
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
            val json = JSONObject(EntityUtils.toString(response.entity))
            val capeName = json.getString("cape")
            val enabled = json.getBoolean("enabled")
            val uuid = json.getString("uuid")

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
                LOGGER.info("Successfully loaded ${CapeService.capeCarriers.count()} cape carriers.")
            }

            capeUser.enabled = !capeUser.enabled
            done(capeUser.enabled, statusCode == HttpStatus.SC_NO_CONTENT, statusCode)
        }
    }

    /**
     * We want to immediately update the owner of the cape and refresh the cape carriers
     */
    @EventTarget
    fun handleNewSession(sessionEvent: SessionEvent) {
        // Check if donator cape is actually enabled and has a transfer code, also make sure the account used is premium.
        val capeUser = clientCapeUser ?: return

        if (!UserUtils.isValidTokenOffline(mc.session.token))
            return

        thread(name = "CapeUpdate") {
            // Apply cape to new account
            val uuid = mc.session.playerID
            val username = mc.session.username

            val httpClient = HttpClients.createDefault()
            val headers = arrayOf(
                BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
                BasicHeader(HttpHeaders.AUTHORIZATION, capeUser.token)
            )
            val request = HttpPatch(SELF_CAPE_URL)
            request.setHeaders(headers)

            val body = JSONObject()
            body.put("uuid", uuid)
            request.entity = StringEntity(body.toString())

            val response = httpClient.execute(request)
            val statusCode = response.statusLine.statusCode

            LOGGER.info(
                if(statusCode == HttpStatus.SC_NO_CONTENT) {
                    capeUser.uuid = uuid
                    "[Donator Cape] Successfully transferred cape to $uuid ($username)"
                } else {
                    "[Donator Cape] Failed to transfer cape ($statusCode)"
                }
            )

            // Refresh cape carriers
            refreshCapeCarriers(force = true) {
                LOGGER.info("Successfully loaded ${capeCarriers.count()} cape carriers.")
            }
        }
    }

    override fun handleEvents() = true

}

data class CapeSelfUser(val token: String, var enabled: Boolean, var uuid: String, val capeName: String)

data class CapeCarrier(val uuid: UUID, val capeName: String)