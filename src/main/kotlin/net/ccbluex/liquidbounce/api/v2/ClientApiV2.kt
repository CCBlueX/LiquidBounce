package net.ccbluex.liquidbounce.api.v2

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.v2.endpoints.*
import net.ccbluex.liquidbounce.config.util.encode
import net.ccbluex.liquidbounce.utils.io.HttpClient
import org.apache.commons.lang3.RandomStringUtils

/**
 * User agent
 * LiquidBounce/<version> (<commit>, <branch>, <build-type>, <platform>)
 *
 * This format is mandatory for all requests and therefore should not be changed (!!!).
 */
internal val ENDPOINT_AGENT =
    "${LiquidBounce.CLIENT_NAME}/${LiquidBounce.clientVersion} (${LiquidBounce.clientCommit}, ${LiquidBounce.clientBranch}, ${if (LiquidBounce.IN_DEVELOPMENT) "dev" else "release"}, ${
        System.getProperty("os.name")
    })"

/**
 * Session token
 *
 * This is used to identify the client in one session and is mandatory for all requests.
 */
internal val CLIENT_TOKEN = RandomStringUtils.randomAlphanumeric(16)

internal const val API_ENDPOINT = "https://api.liquidbounce.net/api/v2"

/**
 * ClientAPI v2 is only to be used for Nextgen, as it is an entirely different API
 * that is not compatible with the old one.
 * That does not mean we can't use the old one for Nextgen, but it is not recommended.
*/
class ClientApiV2(val jwtToken: String? = null) {

    companion object {

        fun withoutCredentials() = ClientApiV2()

        /**
         * If we have a JWT, we can use it to authenticate with the API.
         * This does not require any additional login as the JWT is already there.
         *
         * But it is recommmended to use the [loginWithCredentials] method instead for each start-up, as the JWT might
         * expire, and we need to re-login.
         */
        fun withCredentials(authJwt: String) = ClientApiV2(authJwt)

        fun loginWithCredentials(username: String, password: String): ClientApiV2 {
            val jwt = withoutCredentials().user().login(username, password)
            return withCredentials(jwt)
        }

    }

    fun user() = UserRestAPI(this)

    fun client() = ClientRestAPI(this)

    fun proxy() = ProxyRestAPI(this)

    fun marketplace() = MarketplaceRestAPI(this)

    fun file() = FileRestAPI(this)

    internal inline fun <reified T> get(endpoint: String): T = decode(request(endpoint, method = "GET"))

    internal inline fun <reified T, reified E> post(endpoint: String, body: E): T
        = decode(request(endpoint, method = "POST", body = encode(body)))

    internal inline fun <reified T, reified E> put(endpoint: String, body: E): T
        = decode(request(endpoint, method = "PUT", body = encode(body)))


    internal inline fun <reified T> delete(endpoint: String): T = decode(request(endpoint, method = "DELETE"))

    /**
     * This method is used to request the API.
     *
     * If the request fails, it will throw an exception with the error message.
     */
    internal fun request(endpoint: String, method: String, body: String? = null) = HttpClient.request(
        "$API_ENDPOINT/$endpoint",
        method = method,
        agent = ENDPOINT_AGENT,
        headers = arrayOf(
            "X-Client-Token" to CLIENT_TOKEN,
            "X-Auth-Token" to (jwtToken ?: "Guest")
        ),
        inputData = body?.toByteArray()
    )

    private inline fun <reified T> decode(stringJson: String): T =
        Gson().fromJson(stringJson, object : TypeToken<T>() {}.type)

}

