package net.ccbluex.liquidbounce.utils.login

import com.google.gson.JsonParser
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
object UserUtils {

    /**
     * Check if token is valid
     *
     * Exam
     * 7a7c4193280a4060971f1e73be3d9bdb
     * 89371141db4f4ec485d68d1f63d01eec
     */
    fun isValidToken(token: String) = token.length >= 32

    /**
     * Get UUID of username
     */
    fun getUUID(username : String) : String {
        try {
            // Make a http connection to Mojang API and ask for UUID of username
            val httpConnection = URL("https://api.mojang.com/users/profiles/minecraft/$username").openConnection() as HttpsURLConnection
            httpConnection.connectTimeout = 2000
            httpConnection.readTimeout = 2000
            httpConnection.requestMethod = "GET"
            httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0")
            HttpURLConnection.setFollowRedirects(true)
            httpConnection.doOutput = true

            if(httpConnection.responseCode != 200)
                return ""

            // Read response content and get id from json
            InputStreamReader(httpConnection.inputStream).use {
                val jsonElement = JsonParser().parse(it)

                if(jsonElement.isJsonObject) {
                    return jsonElement.asJsonObject.get("id").asString
                }
            }
        } catch(ignored : Throwable) {
        }

        return ""
    }

}