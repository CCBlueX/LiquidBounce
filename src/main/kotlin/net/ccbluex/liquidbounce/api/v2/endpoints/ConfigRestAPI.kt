package net.ccbluex.liquidbounce.api.v2.endpoints

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.api.v1.AutoSettingsStatusType
import net.ccbluex.liquidbounce.api.v1.AutoSettingsType
import net.ccbluex.liquidbounce.api.v2.ClientApiV2
import net.minecraft.util.Formatting
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Represents the config rest api.
 * Linked with Marketplace - maybe?
 *
 * In the new V2 we are referring to configs instead of settings, as settings is a reserved word in the API V1.
 * GET /api/v2/client/configs -> responses with a list of settings, these can be the global defined or the user owns
 * GET /api/v2/client/configs/<settingId> -> responses with the script of the setting
 * GET /api/v2/client/configs/server/<serverAddress> -> responses with a list of settings,
 * these can be the global defined or the user owns
 *
 * POST /api/v2/client/configs/report/<settingId> -> responses with a boolean, if the report was successful (REQUIRES AUTH)
 * PUT /api/v2/client/configs -> responses with a setting_id, if the upload was successful (REQUIRES AUTH)
 * DELETE /api/v2/client/configs/<settingId> -> responses with a boolean, if the deletion was successful (REQUIRES AUTH)
 * POST /api/v2/client/configs/<settingId> -> updates the setting (REQUIRES AUTH)
 * POST /api/v2/client/configs/contribute -> responses with a boolean, if the contribution was successful (REQUIRES AUTH)
 *
 * @since API version 2
 */
class ConfigRestAPI(private val api: ClientApiV2) {
}


/**
 * Settings
 *
 * Settings only store the setting id, name, type, description, date, contributors and status
 * The setting id will later be used to actually request the setting and load it
 */
data class AutoConfig(
    @SerializedName("setting_id") val settingId: String,
    val name: String,
    @SerializedName("server_address")
    val serverAddress: String,
    @SerializedName("setting_type") val type: AutoSettingsType,
    val description: String,
    var date: String,
    val owner: ConfigOwner,
    val contributors: String,
    @SerializedName("status_type") val statusType: AutoSettingsStatusType,
    @SerializedName("status_date") var statusDate: String
) {

    val dateFormatted: String
        get() = DateFormat.getDateInstance().format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(date))

}

enum class ConfigOwner {
    @SerializedName("Featured")
    FEATURED,

    @SerializedName("Contributor")
    CONTRIBUTOR,

    @SerializedName("User")
    USER
}

/**
 * Settings type
 *
 * Some might prefer RAGE to LEGIT and vice versa
 * Might add more in the future
 */
enum class ConfigType(val displayName: String) {
    @SerializedName("Rage")
    RAGE("Rage"),

    @SerializedName("Legit")
    LEGIT("Legit")
}

/**
 * Status of the settings will allow you to know when if it is bypassing or not
 */
enum class ConfigStatusType(val displayName: String, val formatting: Formatting) {
    @SerializedName("NotBypassing")
    NOT_BYPASSING("Not Bypassing", Formatting.RED),
    @SerializedName("Bypassing")
    BYPASSING("Bypassing", Formatting.GREEN),
    @SerializedName("Undetectable")
    UNDETECTABLE("Undetectable", Formatting.BLUE),
    @SerializedName("Unknown")
    UNKNOWN("Unknown", Formatting.GOLD)
}
