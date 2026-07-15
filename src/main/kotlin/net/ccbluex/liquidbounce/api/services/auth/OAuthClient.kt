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
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AUTH_AUTHORIZE_URL
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AUTH_CLIENT_ID
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.client.logger
import java.util.UUID
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds

/**
 * OAuth client for handling the authentication flow
 */
object OAuthClient : EventListener {

    @Volatile
    private var serverPort: Int? = null

    @Volatile
    private var authCodeDeferred: CompletableDeferred<String>? = null

    @Volatile
    private var server: EmbeddedServer<*, *>? = null

    /**
     * Start the OAuth authentication flow
     *
     * @param onUrl Callback for when the authorization URL is ready
     * @return Client account with the authenticated session
     */
    suspend fun startAuth(onUrl: Consumer<String>): ClientAccount {
        val (codeVerifier, codeChallenge) = PKCEUtils.generatePKCE()
        val state = UUID.randomUUID().toString()

        if (serverPort == null) {
            serverPort = startKtorServer()
        }

        val redirectUri = "http://127.0.0.1:$serverPort/"
        logger.info("OAuth server started on port $serverPort.")
        val authUrl = buildAuthUrl(codeChallenge, state, redirectUri)

        onUrl.accept(authUrl)
        val code = waitForAuthCode()
        val tokenResponse = AuthenticationApi.exchangeToken(AUTH_CLIENT_ID, code, codeVerifier, redirectUri)

        serverPort = null

        return ClientAccount(session = tokenResponse.toAuthSession())
    }

    /**
     * Renew an expired session using its refresh token
     */
    suspend fun renewToken(session: OAuthSession): OAuthSession {
        val tokenResponse = AuthenticationApi.refreshToken(AUTH_CLIENT_ID, session.refreshToken)
        return tokenResponse.toAuthSession()
    }

    private suspend fun startKtorServer(): Int {
        val deferred = CompletableDeferred<String>()
        authCodeDeferred = deferred

        val server = embeddedServer(Netty, host = "127.0.0.1", port = 0) {
            routing {
                get("/") {
                    val code = call.request.queryParameters["code"]
                    if (code != null) {
                        call.respondText(SUCCESS_HTML, ContentType.Text.Html)
                        deferred.complete(code)
                    } else {
                        deferred.completeExceptionally(
                            IllegalArgumentException("No code found in the redirect URL")
                        )
                    }
                }
            }
        }

        server.start(wait = false)
        this.server = server

        val port = server.engine.resolvedConnectors().first().port

        deferred.invokeOnCompletion {
            ioScope.launch {
                server.stopSuspend(gracePeriodMillis = 1000, timeoutMillis = 2000)
            }
            this.server = null
        }

        return port
    }

    private fun buildAuthUrl(codeChallenge: String, state: String, redirectUri: String): String {
        return "$AUTH_AUTHORIZE_URL?client_id=$AUTH_CLIENT_ID&redirect_uri=$redirectUri&" +
            "response_type=code&state=$state&code_challenge=$codeChallenge&code_challenge_method=S256"
    }

    private suspend fun waitForAuthCode(): String = authCodeDeferred!!.await()

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
