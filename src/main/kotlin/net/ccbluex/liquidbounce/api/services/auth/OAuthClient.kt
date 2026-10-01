/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.api.services.auth

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AUTH_AUTHORIZE_URL
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AUTH_CLIENT_ID
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.client.logger
import java.util.UUID
import java.util.function.Consumer
import kotlin.time.Duration.Companion.minutes

/**
 * OAuth client for handling the authentication flow
 */
object OAuthClient : EventListener {

    private val AUTH_TIMEOUT = 5.minutes

    private class PendingAuth(val url: String, val account: Deferred<ClientAccount>)

    private val mutex = Mutex()

    @Volatile
    private var pendingAuth: PendingAuth? = null

    /**
     * Start the OAuth authentication flow. While one is pending, this opens its URL again and waits for the
     * same result, and one that is not finished within [AUTH_TIMEOUT] fails.
     *
     * @param onUrl Callback for when the authorization URL is ready
     * @return Client account with the authenticated session
     */
    suspend fun startAuth(onUrl: Consumer<String>): ClientAccount {
        val auth = mutex.withLock {
            pendingAuth?.takeIf { it.account.isActive } ?: beginAuth().also { pendingAuth = it }
        }

        onUrl.accept(auth.url)
        return auth.account.await()
    }

    /**
     * Renew an expired session using its refresh token
     */
    suspend fun renewToken(session: OAuthSession): OAuthSession {
        val tokenResponse = AuthenticationApi.refreshToken(AUTH_CLIENT_ID, session.refreshToken)
        return tokenResponse.toAuthSession()
    }

    private suspend fun beginAuth(): PendingAuth {
        val (codeVerifier, codeChallenge) = PKCEUtils.generatePKCE()
        val state = UUID.randomUUID().toString()
        val code = CompletableDeferred<String>()

        val server = startKtorServer(state, code)
        val port = server.engine.resolvedConnectors().first().port
        val redirectUri = "http://127.0.0.1:$port/"
        logger.info("OAuth server started on port $port.")

        val account = ioScope.async {
            try {
                val authCode = withTimeoutOrNull(AUTH_TIMEOUT) { code.await() } ?: error("The login timed out")
                val tokenResponse = AuthenticationApi.exchangeToken(AUTH_CLIENT_ID, authCode, codeVerifier, redirectUri)
                ClientAccount(session = tokenResponse.toAuthSession())
            } finally {
                withContext(NonCancellable) {
                    server.stopSuspend(gracePeriodMillis = 1000, timeoutMillis = 2000)
                }
            }
        }

        return PendingAuth(buildAuthUrl(codeChallenge, state, redirectUri), account)
    }

    private fun startKtorServer(state: String, code: CompletableDeferred<String>): EmbeddedServer<*, *> {
        val server = embeddedServer(CIO, host = "127.0.0.1", port = 0) {
            routing {
                get("/") {
                    val parameters = call.request.queryParameters
                    // Anything else on this port could have opened the redirect
                    if (parameters["state"] != state) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }

                    val authCode = parameters["code"]
                    if (authCode != null) {
                        call.respondText(SUCCESS_HTML, ContentType.Text.Html)
                        code.complete(authCode)
                    } else {
                        code.completeExceptionally(
                            IllegalArgumentException("No code found in the redirect URL")
                        )
                    }
                }
            }
        }

        server.start(wait = false)
        return server
    }

    private fun buildAuthUrl(codeChallenge: String, state: String, redirectUri: String): String {
        return "$AUTH_AUTHORIZE_URL?client_id=$AUTH_CLIENT_ID&redirect_uri=$redirectUri&" +
            "response_type=code&state=$state&code_challenge=$codeChallenge&code_challenge_method=S256"
    }

    private const val SUCCESS_HTML = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Authentication Successful</title>
            <style>
                body { font-family: Arial, sans-serif; background-color: #121212; color: #ffffff; text-align: center; padding: 50px; }
                .container { background-color: #1E1E1E; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.5); display: inline-block; }
                h1 { color: #4CAF50; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>Authentication Successful</h1>
                <p>You have successfully authenticated. You can close this tab now.</p>
            </div>
        </body>
        </html>
    """
}
