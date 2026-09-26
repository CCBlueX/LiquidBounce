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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import net.ccbluex.liquidbounce.api.core.formatAvatarUrl
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AccountManagerMessageEvent
import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.randomUsername

// GET /api/v1/client/accounts
private fun Route.getAccounts() = get {
    val accounts = JsonArray()
    for ((i, account) in AccountManager.accounts.withIndex()) {
        val profile = account.profile ?: continue

        accounts.add(JsonObject().apply {
            addProperty("id", i)
            addProperty("username", profile.name)
            addProperty("uuid", profile.id.toString())
            addProperty("avatar", formatAvatarUrl(profile.id, profile.name))
            add("bans", interopGson.toJsonTree(account.bans))
            addProperty("type", account.service.tag)
            addProperty("favorite", account.favorite)
        })
    }
    call.respond(accounts)
}

// POST /api/v1/client/accounts/new/microsoft/device-code
private fun Route.postNewMicrosoftAccount() = post("/device-code") {
    AccountManager.newMicrosoftAccountViaDeviceCode {
        browseUrl(it)
        EventManager.callEvent(AccountManagerMessageEvent("Opened login url in browser"))
    }
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/accounts/new/microsoft/device-code/clipboard
private fun Route.postClipboardMicrosoftAccount() = post("/device-code/clipboard") {
    AccountManager.newMicrosoftAccountViaDeviceCode {
        mc.execute {
            mc.keyboardHandler.clipboard = it
            EventManager.callEvent(AccountManagerMessageEvent("Copied login url to clipboard"))
        }
    }
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/accounts/new/microsoft/webview
private fun Route.postWebViewMicrosoftAccount() = post("/webview") {
    AccountManager.newMicrosoftAccountViaWebView()
    EventManager.callEvent(AccountManagerMessageEvent("Opened Microsoft sign-in window"))
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/accounts/new/microsoft/credentials
private fun Route.postCredentialsMicrosoftAccount() = post("/credentials") {
    data class AccountForm(val email: String, val password: String)

    val accountForm = call.receive<AccountForm>()

    AccountManager.newMicrosoftAccountViaCredentials(accountForm.email, accountForm.password)
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/accounts/new/cracked
private fun Route.postNewCrackedAccount() = post("/cracked") {
    data class AccountForm(val username: String, val online: Boolean?)

    val accountForm = call.receive<AccountForm>()

    AccountManager.newCrackedAccount(accountForm.username, accountForm.online ?: false)
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/accounts/new/session
private fun Route.postNewSessionAccount() = post("/session") {
    data class AccountForm(val token: String)

    val accountForm = call.receive<AccountForm>()

    AccountManager.newSessionAccount(accountForm.token)
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/accounts/new/altening
private fun Route.postNewAlteningAccount() = post {
    data class AlteningForm(val token: String)

    val accountForm = call.receive<AlteningForm>()
    AccountManager.newAlteningAccount(accountForm.token)
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/accounts/new/altening/generate
private fun Route.postGenerateAlteningAccount() = post("/generate") {
    data class AlteningGenForm(val apiToken: String)

    val accountForm = call.receive<AlteningGenForm>()

    AccountManager.generateAlteningAccount(accountForm.apiToken)
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/accounts/swap
private fun Route.postSwapAccounts() = post("/swap") {
    data class AccountForm(val from: Int, val to: Int)

    val accountForm = call.receive<AccountForm>()

    AccountManager.swapAccounts(accountForm.from, accountForm.to)
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/accounts/order
private fun Route.postOrderAccounts() = post("/order") {
    data class AccountOrderRequest(val order: List<Int>)

    val accountOrderRequest = call.receive<AccountOrderRequest>()

    AccountManager.orderAccounts(accountOrderRequest.order)
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/account/login
private fun Route.postLoginAccount() = post {
    data class AccountForm(val id: Int)

    val accountForm = call.receive<AccountForm>()

    AccountManager.loginAccount(accountForm.id)
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/account/login/cracked
private fun Route.postLoginCrackedAccount() = post("/cracked") {
    data class AccountForm(val username: String, val online: Boolean?)

    val accountForm = call.receive<AccountForm>()

    AccountManager.loginCrackedAccount(accountForm.username, accountForm.online ?: false)
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/account/login/session
private fun Route.postLoginSessionAccount() = post("/session") {
    data class AccountForm(val token: String)

    val accountForm = call.receive<AccountForm>()

    AccountManager.loginSessionAccount(accountForm.token)
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/account/restore
private fun Route.postRestoreInitial() = post("/restore") {
    AccountManager.restoreInitial()
    call.respond(mc.user)
}

// PUT /api/v1/client/account/favorite
private fun Route.putFavoriteAccount() = put {
    data class AccountForm(val id: Int)

    val accountForm = call.receive<AccountForm>()

    AccountManager.favoriteAccount(accountForm.id)
    call.respond(HttpStatusCode.NoContent)
}

// DELETE /api/v1/client/account/favorite
private fun Route.deleteFavoriteAccount() = delete {
    data class AccountForm(val id: Int)

    val accountForm = call.receive<AccountForm>()

    AccountManager.unfavoriteAccount(accountForm.id)
    call.respond(HttpStatusCode.NoContent)
}

// DELETE /api/v1/client/account
private fun Route.deleteAccount() = delete {
    data class AccountForm(val id: Int)

    val accountForm = call.receive<AccountForm>()
    val account = AccountManager.removeAccount(accountForm.id)

    call.respond(JsonObject().apply {
        addProperty("id", accountForm.id)

        val profile = account.profile ?: return@apply
        addProperty("username", profile.name)
        addProperty("uuid", profile.id.toString())
        addProperty("avatar", formatAvatarUrl(profile.id, profile.name))

        addProperty("type", account.service.tag)
    })
}

// POST /api/v1/client/account/random-name
private fun Route.generateName() = post("/random-name") {
    call.respond(JsonObject().apply {
        addProperty("name", randomUsername())
    })
}

internal fun Route.accountRoutes() {
    route("/accounts") {
        getAccounts()
        route("/new") {
            route("/microsoft") {
                postNewMicrosoftAccount()
                postClipboardMicrosoftAccount()
                postWebViewMicrosoftAccount()
                postCredentialsMicrosoftAccount()
            }
            postNewCrackedAccount()
            postNewSessionAccount()
            route("/altening") {
                postNewAlteningAccount()
                postGenerateAlteningAccount()
            }
        }
        postSwapAccounts()
        postOrderAccounts()
    }
    route("/account") {
        deleteAccount()
        route("/login") {
            postLoginAccount()
            postLoginCrackedAccount()
            postLoginSessionAccount()
        }
        postRestoreInitial()
        route("/favorite") {
            putFavoriteAccount()
            deleteFavoriteAccount()
        }
        generateName()
    }
}
