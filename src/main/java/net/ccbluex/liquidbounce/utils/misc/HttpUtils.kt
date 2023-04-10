/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.utils.misc

import org.apache.commons.io.FileUtils.copyInputStreamToFile
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
object HttpUtils {

    private const val DEFAULT_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0"

    init {
        HttpURLConnection.setFollowRedirects(true)
    }

    private fun make(url: String, method: String, agent: String = DEFAULT_AGENT): HttpURLConnection {
        val httpConnection = URL(url).openConnection() as HttpURLConnection

        httpConnection.requestMethod = method
        httpConnection.connectTimeout = 2000
        httpConnection.readTimeout = 10000

        httpConnection.setRequestProperty("User-Agent", agent)

        httpConnection.instanceFollowRedirects = true
        httpConnection.doOutput = true

        return httpConnection
    }

    @Throws(IOException::class)
    fun request(url: String, method: String, agent: String = DEFAULT_AGENT) =
        requestStream(url, method, agent).reader().readText()

    @Throws(IOException::class)
    fun requestStream(url: String, method: String, agent: String = DEFAULT_AGENT): InputStream =
        make(url, method, agent).inputStream

    @Throws(IOException::class)
    fun get(url: String) = request(url, "GET")

    @Throws(IOException::class)
    fun responseCode(url: String, method: String, agent: String = DEFAULT_AGENT) =
        make(url, method, agent).responseCode

    @Throws(IOException::class)
    fun download(url: String, file: File) =
        copyInputStreamToFile(requestStream(url, "GET"), file)

}
