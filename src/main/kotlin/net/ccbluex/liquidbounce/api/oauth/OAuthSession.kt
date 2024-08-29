package net.ccbluex.liquidbounce.api.oauth

/**
 * Contains the access token and the refresh token.
 */
data class OAuthSession(
    var accessToken: ExpiryValue<String>,
    val refreshToken: String,
)
