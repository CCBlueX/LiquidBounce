/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.cape

import com.google.gson.JsonParser
import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.login.UserUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get
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
import java.util.concurrent.atomic.AtomicLong

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
    private const val REFRESH_DELAY = 300000L // Every 5 minutes should update

    /**
     * Collection of all cape carriers on the API.
     * We start with an empty list, which will be updated by the refreshCapeCarriers function frequently based on the REFRESH_DELAY.
     */
    internal var capeCarriers = emptyList<CapeCarrier>()
    private val lastUpdate = AtomicLong(0L)
    private var refreshJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Refresh cape carriers, capture from the API.
     * It will take a list of (uuid, cape_name) tuples.
     */
    fun refreshCapeCarriers(force: Boolean = false, done: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate.get() > REFRESH_DELAY || force) {
            if (refreshJob?.isActive != true) {
                refreshJob = scope.launch {
                    runCatching {
                        // Capture data from API and parse JSON
                        val (json, code) = get(CAPE_CARRIERS_URL)
                        if (code != 200) throw RuntimeException("Failed to get cape carriers. Status code: $code")

                        val parsedJson = JsonParser().parse(json)

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

                        capeCarriers = jsonCapeCarriers
                        lastUpdate.set(currentTime)
                        done()
                    }.onFailure {
                        LOGGER.error("Failed to refresh cape carriers due to error.", it)
                    }
                }
            }
        } else {
            // Call out done immediate because there is no refresh required at the moment
            done()
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

        scope.launch {
            runCatching {
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
                    LOGGER.info("Logged in successfully. Cape: $capeName")
                } else {
                    throw RuntimeException("Failed to get self cape. Status code: $statusCode")
                }
            }.onFailure {
                LOGGER.error("Failed to login due to error.", it)
            }
        }
    }

    fun logout() {
        clientCapeUser = null
        knownToken = ""
        LOGGER.info("Logged out successfully.")
    }

    /**
     * Update the cape state of the user
     */
    fun toggleCapeState(done: (Boolean, Boolean, Int) -> Unit) {
        val capeUser = clientCapeUser ?: return

        scope.launch {
            runCatching {
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
                    LOGGER.info("Cape state toggled successfully.")
                }

                capeUser.enabled = !capeUser.enabled
                done(capeUser.enabled, statusCode == HttpStatus.SC_NO_CONTENT, statusCode)
            }.onFailure {
                LOGGER.error("Failed to toggle cape state due to error.", it)
            }
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

        scope.launch {
            runCatching {
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

                if (statusCode == HttpStatus.SC_NO_CONTENT) {
                    capeUser.uuid = uuid
                    LOGGER.info("[Donator Cape] Successfully transferred cape to $uuid ($username)")
                } else {
                    LOGGER.info("[Donator Cape] Failed to transfer cape ($statusCode)")
                }

                // Refresh cape carriers
                refreshCapeCarriers(force = true) {
                    LOGGER.info("Cape carriers refreshed after session change.")
                }
            }.onFailure {
                LOGGER.error("Failed to handle new session due to error.", it)
            }
        }
    }

    override fun handleEvents() = true

}

data class CapeSelfUser(val token: String, var enabled: Boolean, var uuid: String, val capeName: String)

data class CapeCarrier(val uuid: UUID, val capeName: String)