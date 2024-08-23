package net.ccbluex.liquidbounce.api.oauth

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.util.Exclude
import net.ccbluex.liquidbounce.features.cosmetic.Cosmetic
import java.util.UUID

object ClientAccountManager : Configurable("account") {
    var clientAccount by value("account", ClientAccount.EMPTY_ACCOUNT)
}

/**
 * Represents a client account that is used to authenticate with the LiquidBounce API.
 *
 * It might hold additional information that can be obtained from the API.
 */
data class ClientAccount(
    private var session: OAuthSession? = null,
    @Exclude
    var userInformation: UserInformation? = null,
    @Exclude
    var cosmetics: Set<Cosmetic>? = null
) {

    private suspend fun takeSession(): OAuthSession = session?.takeIf { !it.accessToken.isExpired() } ?: run {
        renew()
        session ?: error("No session")
    }

    suspend fun updateInfo(): Unit = withContext(Dispatchers.IO) {
        val info = OAuthClient.getUserInformation(takeSession())
        userInformation = info
    }

    suspend fun updateCosmetics(): Unit = withContext(Dispatchers.IO) {
        cosmetics = OAuthClient.getCosmetics(takeSession())
    }

    suspend fun transferTemporaryOwnership(uuid: UUID): Unit = withContext(Dispatchers.IO) {
        OAuthClient.transferTemporaryOwnership(takeSession(), uuid)
    }

    suspend fun renew() = withContext(Dispatchers.IO) {
        session = OAuthClient.renewToken(takeSession())
    }

    companion object {
        val EMPTY_ACCOUNT = ClientAccount(null, null, null)
    }

}

data class UserInformation(
    @SerializedName("user_id") val userId: String,
    val premium: Boolean
)
