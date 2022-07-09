/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.utils.misc

import com.google.common.io.ByteStreams
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
object HttpUtils
{
	private const val DEFAULT_USERAGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36"

	init
	{
		HttpURLConnection.setFollowRedirects(true)
	}

	private fun openURLConnection(url: String, method: String, agent: String = DEFAULT_USERAGENT): URLConnection
	{
		val connection = URL(url).openConnection()

		connection.readTimeout = 10000
		connection.connectTimeout = 2000
		connection.doInput = true
		connection.doOutput = true
		connection.setRequestProperty("User-Agent", agent)

		if (connection is HttpURLConnection)
		{
			connection.requestMethod = method
			connection.instanceFollowRedirects = true
		}

		return connection
	}

	@Throws(IOException::class)
	fun requestStream(url: String, method: String, useragent: String = DEFAULT_USERAGENT): InputStream = openURLConnection(url, method, useragent).inputStream

	@Throws(IOException::class)
	fun request(url: String, method: String, useragent: String = DEFAULT_USERAGENT): String = requestStream(url, method, useragent).reader().readText()

	@Throws(IOException::class)
	@JvmStatic
	operator fun get(url: String) = request(url, "GET")

	@Throws(IOException::class)
	@JvmStatic
	fun download(url: String, file: File) = FileOutputStream(file).use {
		@Suppress("UnstableApiUsage") ByteStreams.copy(openURLConnection(url, "GET").inputStream, it)
	}
}
