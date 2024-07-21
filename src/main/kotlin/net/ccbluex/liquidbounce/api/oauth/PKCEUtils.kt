package net.ccbluex.liquidbounce.api.oauth

import java.security.MessageDigest
import java.util.*

object PKCEUtils {
    fun generatePKCE(): Pair<String, String> {
        val codeVerifier = UUID.randomUUID().toString().replace("-", "")
        val codeChallenge = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        ).replace("=", "").replace("+", "-").replace("/", "_")
        return codeVerifier to codeChallenge
    }
}
