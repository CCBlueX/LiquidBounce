package net.ccbluex.liquidbounce.api

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.post

import net.ccbluex.liquidbounce.utils.misc.HttpUtils.request
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder

/**
 * LiquidBounce Client API
 *
 * This represents all API endpoints of the LiquidBounce API for the usage on the client.
 */
object ClientApi {

    const val API_ENDPOINT = "https://api.liquidbounce.net/api/v1"

    /**
     * This makes sense because we want forks to be able to use this API and not only the official client.
     * It also allows us to use API endpoints for legacy on other branches.
     */
    private const val HARD_CODED_BRANCH = "legacy"

    fun requestNewestBuildEndpoint(branch: String = HARD_CODED_BRANCH, release: Boolean = false) = endpointRequest<Build>("version/newest/$branch${if (release) "/release" else "" }")

    fun requestMessageOfTheDayEndpoint(branch: String = HARD_CODED_BRANCH) = endpointRequest<MessageOfTheDay>("client/$branch/motd")


    fun requestSettingsList(branch: String = HARD_CODED_BRANCH) = endpointRequest<Array<AutoSettings>>("client/$branch/settings")

    fun requestSettingsScript(settingId: String, branch: String = HARD_CODED_BRANCH) = textEndpointRequest("client/$branch/settings/$settingId")

    /**
     * Reports settings for any reason
     *
     * todo: add reason and change to POST instead of GET
     */
    fun reportSettings(settingId: String, branch: String = HARD_CODED_BRANCH) = endpointRequest<ReportResponse>("client/$branch/settings/report/$settingId")

    /**
     * Uploads settings to the API
     */
    fun uploadSettings(name: String, contributors: String, script: String, branch: String = HARD_CODED_BRANCH): UploadResponse {
        val res = textEndpointPost("client/$branch/settings/upload") {
            // Create http entity with settings_file as file, name as string, contributors as string to form body

            val entity = MultipartEntityBuilder.create()
                .addTextBody("name", name)
                .addTextBody("contributors", contributors)
                .addBinaryBody("settings_file", script.toByteArray(), ContentType.APPLICATION_OCTET_STREAM, "settings_file")
                .setLaxMode() // strict mode is not supported by the API, so we have to use lax mode
                .build()
            entity
        }

        return runCatching {
            parse<UploadResponse>(res)
        }.getOrElse {
            UploadResponse(Status.ERROR, res, "none")
        }
    }


    /**
     * Request endpoint and parse JSON to data class
     */
    private inline fun <reified T> endpointRequest(endpoint: String): T = parse(textEndpointRequest(endpoint))

    /**
     * Parse JSON to data class
     */
    private inline fun <reified T> parse(json: String): T = PRETTY_GSON.fromJson(json, T::class.java)

    /**
     * User agent
     * LiquidBounce/<version> (<commit>, <branch>, <build-type>, <platform>)
     */
    private val ENDPOINT_AGENT = "${LiquidBounce.CLIENT_NAME}/${LiquidBounce.clientVersionText} (${LiquidBounce.clientCommit}, ${LiquidBounce.clientBranch}, ${if (LiquidBounce.IN_DEV) "dev" else "release"}, ${System.getProperty("os.name")})"

    /**
     * Session token
     *
     * This is used to identify the client in one session
     */
    private val SESSION_TOKEN = RandomUtils.randomString(16)

    /**
     * Request to endpoint with custom agent and session token
     */
    private fun textEndpointRequest(endpoint: String) = request(
        "$API_ENDPOINT/$endpoint",
        method = "GET",
        agent = ENDPOINT_AGENT,
        headers = arrayOf("X-Session-Token" to SESSION_TOKEN)
    )

    private fun textEndpointPost(endpoint: String, entity: () -> HttpEntity) = post(
        "$API_ENDPOINT/$endpoint",
        agent = ENDPOINT_AGENT,
        headers = arrayOf("X-Session-Token" to SESSION_TOKEN),
        entity = entity
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

/**
 * Upload response
 */
data class UploadResponse(val status: Status, val message: String, val token: String)

/**
 * Report response
 */
data class ReportResponse(val status: Status, val message: String)

enum class Status {
    @SerializedName("success")
    SUCCESS,
    @SerializedName("error")
    ERROR
}