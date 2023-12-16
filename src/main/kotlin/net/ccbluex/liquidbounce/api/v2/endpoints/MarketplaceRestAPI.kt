package net.ccbluex.liquidbounce.api.v2.endpoints

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.api.v2.ClientApiV2

/**
 * Represents the marketplace rest api.
 *
 * GET /api/v2/marketplace/items/<id> -> responses with a marketplace item
 * GET /api/v2/marketplace/items/<id>/versions -> responses with a list of versions for the marketplace item
 * GET /api/v2/marketplace/items/<id>/versions/<version> -> responses with a marketplace item version
 * GET /api/v2/marketplace/subscribed -> responses with a list of subscribed marketplace items (REQUIRES AUTH)
 */
class MarketplaceRestAPI(private val api: ClientApiV2) {

    fun getItem(id: Int) = api.get<MarketplaceItem>("/marketplace/items/$id")

    fun getItemVersions(id: Int) = api.get<List<MarketplaceItemVersion>>("/marketplace/items/$id/versions")

    fun getItemVersion(id: Int, version: Int) =
        api.get<MarketplaceItemVersion>("/marketplace/items/$id/versions/$version")

    /**
     * Subscribed items are items that the user has installed or needs to be installed.
     *
     * This should be frequently called to check if the user wants to install a new item.
     */
    fun getSubscribedItems() = api.get<List<MarketplaceItem>>("/marketplace/subscribed")

}

//pub id: u32,
//pub name: String,
//pub author_id: u32,
//pub short_description: String,
//pub readme: String,
//pub banner_id: u32,
//pub category: MarketplaceCategory,
//#[new(value = "MarketplaceStatus::Pending")]
//pub status: MarketplaceStatus,
//#[new(value = "chrono::Utc::now()")]
//pub created_at: DateTime<Utc>,
data class MarketplaceItem(
    val id: Int,
    val name: String,
    val author_id: Int,
    val short_description: String,
    val readme: String,
    val banner_id: Int,
    val category: MarketplaceCategory,
    val status: MarketplaceStatus,
    val created_at: String
)

//#[strum(ascii_case_insensitive)]
//#[serde(rename = "script")]
//Script = 0, // Custom user Script
//#[strum(ascii_case_insensitive)]
//#[serde(rename = "library")]
//Library = 1, // Script library (can be loaded by scripts)
//#[strum(ascii_case_insensitive)]
//#[serde(rename = "theme")]
//Theme = 3, // Custom user nextgen theme
//#[strum(ascii_case_insensitive)]
//#[serde(rename = "settings")]
//Settings = 4, // Custom user settings (uses settings token)
enum class MarketplaceCategory {
    @SerializedName("script") SCRIPT,
    @SerializedName("library") LIBRARY,
    @SerializedName("theme") THEME,
    @SerializedName("settings") SETTINGS
}

//#[derive(Type, Serialize, Deserialize, EnumString)]
//#[repr(u8)]
//pub enum MarketplaceStatus {
//    #[strum(ascii_case_insensitive)]
//    #[serde(rename = "pending")]
//    Pending = 0, // Item is pending approval
//    #[strum(ascii_case_insensitive)]
//    #[serde(rename = "approved")]
//    Approved = 1, // Item is approved
//    #[strum(ascii_case_insensitive)]
//    #[serde(rename = "declined")]
//    Declined = 2, // Item is declined
//}
enum class MarketplaceStatus {
    @SerializedName("pending") PENDING,
    @SerializedName("approved") APPROVED,
    @SerializedName("declined") DECLINED
}

//#[derive(FromRow, Serialize, Deserialize, new)]
//pub struct MarketplaceVersion {
//    pub id: u32, // Version ID
//    pub item_id: u32, // Item ID
//    pub download_id: u32, // Download ID
//    pub version: String, // SemVer
//    pub client_version: String, // Client version, can be either SemVer or "b83, b84..."
//    pub minecraft_version: String, // Minecraft version (SemVer)
//    pub changelog: String, // Changelog
//    #[new(value = "chrono::Utc::now()")]
//    pub created_at: DateTime<Utc>
//}

data class MarketplaceItemVersion(
    val id: Int,
    @SerializedName("item_id")
    val itemId: Int,
    @SerializedName("download_id")
    val downloadId: Int,
    val version: String,
    @SerializedName("client_version")
    val clientVersion: String,
    @SerializedName("minecraft_version")
    val minecraftVersion: String,
    val changelog: String,
    @SerializedName("created_at")
    val createdAt: String
)
