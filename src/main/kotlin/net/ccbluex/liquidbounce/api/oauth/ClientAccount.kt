package net.ccbluex.liquidbounce.api.oauth

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ClientAccount(
    val accessToken: String,
    val expiresAt: Long,
    val refreshToken: String,
    var userInformation: UserInformation? = null
) {
    fun isExpired() = expiresAt < System.nanoTime() / 1000_000_000L

    suspend fun updateInfo(): Unit = withContext(Dispatchers.IO) {
        val info = OAuthClient.getUserInformation(this@ClientAccount)
        userInformation = info
    }

    suspend fun renew(): ClientAccount = withContext(Dispatchers.IO) {
        OAuthClient.renewToken(refreshToken)
    }
}

data class UserInformation(
    @SerializedName("user_id") val userId: String,
    val premium: Boolean
)
