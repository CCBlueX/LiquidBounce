/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.api

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.io.HttpClient.request

import org.apache.commons.lang3.RandomStringUtils

/**
 * LiquidBounce Client API
 *
 * This represents all API endpoints of the LiquidBounce API for the usage on the client.
 */
object ClientApi {

    private const val API_ENDPOINT = "https://api.liquidbounce.net/api/v1"

    /**
     * This makes sense because we want forks to be able to use this API and not only the official client.
     * It also allows us to use API endpoints for legacy on other branches.
     */
    private const val HARD_CODED_BRANCH = "nextgen"

    fun requestNewestBuildEndpoint(branch: String = HARD_CODED_BRANCH, release: Boolean = false) = endpointRequest<Build>("version/newest/$branch${if (release) "/release" else "" }")

    fun requestMessageOfTheDayEndpoint(branch: String = HARD_CODED_BRANCH) = endpointRequest<MessageOfTheDay>("client/$branch/motd")


    fun requestSettingsList(branch: String = HARD_CODED_BRANCH) = endpointRequest<Array<AutoSettings>>("client/$branch/settings")

    fun requestSettingsScript(settingId: String, branch: String = HARD_CODED_BRANCH) = plainEndpointRequest("client/$branch/settings/$settingId")

    /**
     * todo: this was not implemented yet, might be added in future versions
     */
    fun reportSettings(settingId: String, branch: String = HARD_CODED_BRANCH) = endpointRequest<EmptyResponse>("client/$branch/settings/report/$settingId")

    /**
     * todo: this was not implemented yet, might be added in future versions
     */
    fun uploadSettings(settings: String, branch: String = HARD_CODED_BRANCH) = endpointRequest<EmptyResponse>("client/$branch/settings/upload")

    /**
     * Request endpoint and parse JSON to data class
     */
    private inline fun <reified T> endpointRequest(endpoint: String): T = decode(plainEndpointRequest(endpoint))

    /**
     * User agent
     * LiquidBounce/<version> (<commit>, <branch>, <build-type>, <platform>)
     */
    private val ENDPOINT_AGENT = "${LiquidBounce.CLIENT_NAME}/${LiquidBounce.clientVersion} (${LiquidBounce.clientCommit}, ${LiquidBounce.clientBranch}, ${if (LiquidBounce.IN_DEVELOPMENT) "dev" else "release"}, ${System.getProperty("os.name")})"

    /**
     * Session token
     *
     * This is used to identify the client in one session
     */
    private val SESSION_TOKEN = RandomStringUtils.randomAlphanumeric(16)

    /**
     * Request to endpoint with custom agent and session token
     */
    private fun plainEndpointRequest(endpoint: String) = request(
        "$API_ENDPOINT/$endpoint",
        method = "GET",
        agent = ENDPOINT_AGENT,
        headers = arrayOf("X-Session-Token" to SESSION_TOKEN)
    )
}

/**
 * Data classes for the API
 */

data class Build(@SerializedName("build_id")
                 val buildId: Int,
                 @SerializedName("commit_id")
                 val commitId: String,
                 val branch: String,
                 @SerializedName("lb_version")
                 val lbVersion: String,
                 @SerializedName("mc_version")
                 val mcVersion: String,
                 val release: Boolean,
                 val date: String,
                 val message: String,
                 val url: String)

/**
 * Message of the day
 *
 * Contains only a message
 */
data class MessageOfTheDay(val message: String)

/**
 * Settings
 *
 * Settings only stores the setting id, name, type, description, date, contributors and status
 * The setting id will later be used to actually request the setting and load it
 */
data class AutoSettings(
    @SerializedName("setting_id")
    val settingId: String,
    val name: String,
    @SerializedName("setting_type")
    val type: AutoSettingsType,
    val description: String,
    var date: String,
    val contributors: String,
    @SerializedName("status_type")
    val statusType: AutoSettingsStatusType,
    @SerializedName("status_date")
    var statusDate: String
)

/**
 * Settings type
 *
 * Some might prefer RAGE to LEGIT and vice versa
 * Might add more in the future
 */
enum class AutoSettingsType(val displayName: String) {
    @SerializedName("Rage")
    RAGE("Rage"),
    @SerializedName("Legit")
    LEGIT("Legit")
}

/**
 * Status of the settings will allow you to know when if it is bypassing or not
 */
enum class AutoSettingsStatusType(val displayName: String) {
    @SerializedName("NotBypassing")
    NOT_BYPASSING("Not Bypassing"),
    @SerializedName("Bypassing")
    BYPASSING("Bypassing"),
    @SerializedName("Undetectable")
    UNDETECTABLE("Undetectable"),
    @SerializedName("Unknown")
    UNKNOWN("Unknown")
}

/**
 * Empty response
 */
class EmptyResponse
