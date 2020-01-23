package net.ccbluex.liquidbounce.utils.misc

import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
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

    private fun make(url: String, method: String,
                     agent: String = DEFAULT_AGENT): HttpURLConnection {
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
    fun request(url: String, method: String,
                agent: String = DEFAULT_AGENT): String {
        val connection = make(url, method, agent)

        return connection.inputStream.reader().readText()
    }

    @Throws(IOException::class)
    @JvmStatic
    fun get(url: String) = request(url, "GET")

    @Throws(IOException::class)
    @JvmStatic
    fun download(url: String, file: File) = FileUtils.copyInputStreamToFile(make(url, "GET").inputStream, file)

}