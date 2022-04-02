/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.login

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object UserUtils {

    /**
     * Check if token is valid
     *
     * Exam
     * 7a7c4193280a4060971f1e73be3d9bdb
     * 89371141db4f4ec485d68d1f63d01eec
     */
    fun isValidTokenOffline(token: String) = token.length >= 32

    fun isValidToken(token: String): Boolean {
        val client = HttpClients.createDefault()
        val headers = arrayOf(
                BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        )

        val request = HttpPost("https://authserver.mojang.com/validate")
        request.setHeaders(headers)

        val body = JsonObject()
        body.addProperty("accessToken", token)
        request.entity = StringEntity(Gson().toJson(body))

        val response = client.execute(request)

        return response.statusLine.statusCode == 204
    }

    fun getUsername(uuid: String): String? {
        val client = HttpClients.createDefault()
        val request = HttpGet("https://api.mojang.com/user/profiles/${uuid}/names")
        val response = client.execute(request)

        if (response.statusLine.statusCode != 200) {
            return null
        }

        val names = JsonParser().parse(EntityUtils.toString(response.entity)).asJsonArray

        return names.get(names.size() - 1).asJsonObject.get("name").asString
    }

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
