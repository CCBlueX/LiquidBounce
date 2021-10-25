package net.mcleaks

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.utils.runAsync
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets

object MCLeaks
{
	@JvmStatic
	var session: Session? = null
		private set

	@JvmStatic
	val isAltActive: Boolean
		get() = session != null

	// private val EXECUTOR_SERVICE = Exec utors.newCachedThreadPool() // Share LiquidBounce workers

	private val gson = Gson()
	private const val REDEEM_URL = "https://auth.mcleaks.net/v1/redeem"

	private const val REQUEST_USERAGENT = "Mozilla/5.0 (Windows; U; Windows NT 6.1; rv:2.2) Gecko/20110201"

	fun refresh(session: Session?)
	{
		MCLeaks.session = session
	}

	fun remove()
	{
		session = null
	}

	fun redeem(token: String, callback: (Any) -> Unit)
	{
		runAsync {
			val connection = preparePostRequest("{\"token\":\"$token\"}")
			if (connection == null)
			{
				callback("An error occurred! [Redeem - 0x1] - Failed to prepare request!")
				return@runAsync
			}

			val result = getResult(connection)
			if (result is String)
			{
				callback(result)
				return@runAsync
			}

			val jsonObject = result as? JsonObject? ?: return@runAsync
			if (!jsonObject.has("mcname") || !jsonObject.has("session"))
			{
				callback("An error occurred! [Redeem - 0x2] - Responce doesn't have 'mcname' or 'session' member!")
				return@runAsync
			}

			callback(RedeemResponse(jsonObject["mcname"].asString, jsonObject["session"].asString))
		}
	}

	private fun preparePostRequest(body: String): URLConnection?
	{
		return try
		{
			val connection = URL(REDEEM_URL).openConnection()
			connection.connectTimeout = 10000
			connection.readTimeout = 10000
			connection.setRequestProperty("User-Agent", REQUEST_USERAGENT)
			connection.doOutput = true

			if (connection is HttpURLConnection) connection.requestMethod = "POST"

			val dos = DataOutputStream(connection.outputStream)
			dos.write(body.toByteArray(StandardCharsets.UTF_8))
			dos.flush()
			dos.close()

			connection
		}
		catch (e: Exception)
		{
			e.printStackTrace()
			null
		}
	}

	private fun getResult(urlConnection: URLConnection): Any?
	{
		return try
		{
			val bufferedReader = BufferedReader(InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8))
			val readData = bufferedReader.readLines().joinToString()
			bufferedReader.close()

			val jsonElement = gson.fromJson(readData, JsonElement::class.java)

			if (!jsonElement.isJsonObject || !jsonElement.asJsonObject.has("success")) return "An error occurred! [Result Analysis - 0x1] - Responce is not JsonObject or Responce doesn't have 'success' member!"

			if (!jsonElement.asJsonObject["success"].asBoolean) return if (jsonElement.asJsonObject.has("errorMessage")) "An error occurred! [Result Analysis - 0x4] - ${jsonElement.asJsonObject["errorMessage"].asString}" else "An error occurred! [Result Analysis - 0x4]"

			if (!jsonElement.asJsonObject.has("result")) return "An error occurred! [Result Analysis - 0x3] - Responce doesn't have 'result' member!"

			if (jsonElement.asJsonObject["result"].isJsonObject) jsonElement.asJsonObject["result"].asJsonObject else null
		}
		catch (e: Exception)
		{
			e.printStackTrace()
			"An error occurred! [Result Analysis - 0x2] - Unexpected exception ($e) thrown while processing responce!"
		}
	}
}

data class RedeemResponse internal constructor(val username: String, val token: String)

data class Session(val username: String, val token: String)
