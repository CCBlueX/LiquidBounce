package net.ccbluex.liquidbounce.api.oauth

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.cosmetic.Cosmetic

object ClientAccountManager : Configurable("account") {
    var account by value("account", ClientAccount.EMPTY_ACCOUNT)
}

data class ClientAccount(
    val accessToken: String,
    val expiresAt: Long,
    val refreshToken: String,
    var userInformation: UserInformation? = null,
    var cosmetics: Set<Cosmetic>? = null
) {

    fun isExpired() = expiresAt < System.nanoTime() / 1000_000_000L

    suspend fun updateInfo(): Unit = withContext(Dispatchers.IO) {
        if (isExpired()) {
            renew()
        }

        val info = OAuthClient.getUserInformation(this@ClientAccount)
        userInformation = info
    }

    suspend fun updateCosmetics(): Unit = withContext(Dispatchers.IO) {
        if (isExpired()) {
            renew()
        }

        cosmetics = OAuthClient.getCosmetics(this@ClientAccount)
    }

    suspend fun renew(): ClientAccount = withContext(Dispatchers.IO) {
        OAuthClient.renewToken(refreshToken)
    }

    companion object {
        val EMPTY_ACCOUNT = ClientAccount("", 0, "")
    }

}

data class UserInformation(
    @SerializedName("user_id") val userId: String,
    val premium: Boolean
)
