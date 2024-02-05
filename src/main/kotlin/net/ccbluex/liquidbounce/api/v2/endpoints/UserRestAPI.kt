package net.ccbluex.liquidbounce.api.v2.endpoints

import net.ccbluex.liquidbounce.api.v2.ClientApiV2

/**
 * Represents the user rest api.
 * POST /api/v2/user/login -> responses with a JWT
 * GET /api/v2/user/register -> responses with the URL to the registration page (this should be opened in the browser)
 * POST /api/v2/user/valid -> responses with a boolean, if the JWT is valid
 *
 * PUT /api/v2/user/link -> links minecraft account with the liquidbounce account (REQUIRES AUTH)
 */
class UserRestAPI(private val api: ClientApiV2) {

    /**
     * Use [ClientApiV2.loginWithCredentials] instead.
     */
    internal fun login(email: String, password: String): String {
        data class Credentials(val email: String, val password: String)
        data class LoginResponse(val token: String)

        val credentials = Credentials(email, password)

        val response = api.post<LoginResponse, Credentials>("user/login", credentials)
        return response.token
    }



}
