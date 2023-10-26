/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc

import org.apache.commons.io.FileUtils.copyInputStreamToFile
import org.apache.http.HttpEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClientBuilder
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

    private fun make(url: String, method: String, agent: String = DEFAULT_AGENT, headers: Array<Pair<String, String>> = emptyArray()) : HttpURLConnection {
        val httpConnection = URL(url).openConnection() as HttpURLConnection

        httpConnection.requestMethod = method
        httpConnection.connectTimeout = 2000
        httpConnection.readTimeout = 10000

        httpConnection.setRequestProperty("User-Agent", agent)

        for ((key, value) in headers) {
            httpConnection.setRequestProperty(key, value)
        }

        httpConnection.instanceFollowRedirects = true
        httpConnection.doOutput = true

        return httpConnection
    }

    @Throws(IOException::class)
    fun request(url: String, method: String, agent: String = DEFAULT_AGENT, headers: Array<Pair<String, String>> = emptyArray()) =
        requestStream(url, method, agent, headers).let { (stream, code) -> stream.reader().readText() to code }

    fun post(url: String, agent: String = DEFAULT_AGENT, headers: Array<Pair<String, String>> = emptyArray(), entity: () -> HttpEntity): String {
        val httpClient = HttpClientBuilder
            .create()
            .setUserAgent(agent)
            .build()

        val httpPost = HttpPost(url)
        httpPost.entity = entity()

        for ((key, value) in headers) {
            httpPost.setHeader(key, value)
        }

        val response = httpClient.execute(httpPost)
        return response.entity.content.reader().readText()
    }

    @Throws(IOException::class)
    fun requestStream(url: String, method: String, agent: String = DEFAULT_AGENT, headers: Array<Pair<String, String>> = emptyArray()) : Pair<InputStream, Int> {
        val conn = make(url, method, agent, headers)
        // Return either the input stream or the error stream
        return (if (conn.responseCode < 400) conn.inputStream else conn.errorStream) to conn.responseCode
    }


    @Throws(IOException::class)
    fun get(url: String) = request(url, "GET")

    @Throws(IOException::class)
    fun responseCode(url: String, method: String, agent: String = DEFAULT_AGENT) =
        make(url, method, agent).responseCode

    @Throws(IOException::class)
    fun download(url: String, file: File) {
        val (stream, code) = requestStream(url, "GET")

        // Check if code is 200
        if (code != 200) {
            error("Response code is $code")
        }

        copyInputStreamToFile(stream, file)
    }

}
