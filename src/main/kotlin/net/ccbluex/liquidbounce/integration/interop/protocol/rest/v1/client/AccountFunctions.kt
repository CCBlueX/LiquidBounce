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
import net.ccbluex.liquidbounce.api.core.formatAvatarUrl
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AccountManagerMessageEvent
import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.randomUsername
import net.ccbluex.netty.http.routing.RoutingContext
import org.lwjgl.glfw.GLFW

// GET /api/v1/client/accounts
fun RoutingContext.getAccounts() {
    val accounts = JsonArray()
    for ((i, account) in AccountManager.accounts.withIndex()) {
        val profile = account.profile ?: continue

        accounts.add(JsonObject().apply {
            addProperty("id", i)
            addProperty("username", profile.username)
            addProperty("uuid", profile.uuid.toString())
            addProperty("avatar", formatAvatarUrl(profile.uuid, profile.username))
            add("bans", interopGson.toJsonTree(account.bans))
            addProperty("type", account.type.commonName)
            addProperty("favorite", account.favorite)
        })
    }
    respond(accounts)
}

// POST /api/v1/client/accounts/new/microsoft
fun RoutingContext.postNewMicrosoftAccount() {
    AccountManager.newMicrosoftAccount {
        browseUrl(it)
        EventManager.callEvent(AccountManagerMessageEvent("Opened login url in browser"))
    }
    respondNoContent()
}

// POST /api/v1/client/accounts/clipboard
fun RoutingContext.postClipboardMicrosoftAccount() {
    AccountManager.newMicrosoftAccount {
        mc.execute {
            GLFW.glfwSetClipboardString(mc.window.handle(), it)
            EventManager.callEvent(AccountManagerMessageEvent("Copied login url to clipboard"))
        }
    }
    respondNoContent()
}

// POST /api/v1/client/accounts/new/cracked
fun RoutingContext.postNewCrackedAccount() {
    data class AccountForm(val username: String, val online: Boolean?)

    val accountForm = receive<AccountForm>()

    AccountManager.newCrackedAccount(accountForm.username, accountForm.online ?: false)
    respondNoContent()
}

// POST /api/v1/client/accounts/new/session
fun RoutingContext.postNewSessionAccount() {
    data class AccountForm(val token: String)

    val accountForm = receive<AccountForm>()

    AccountManager.newSessionAccount(accountForm.token)
    respondNoContent()
}

// POST /api/v1/client/accounts/new/altening
fun RoutingContext.postNewAlteningAccount() {
    data class AlteningForm(val token: String)

    val accountForm = receive<AlteningForm>()
    AccountManager.newAlteningAccount(accountForm.token)
    respondNoContent()
}

// POST /api/v1/client/accounts/generate
fun RoutingContext.postGenerateAlteningAccount() {
    data class AlteningGenForm(val apiToken: String)

    val accountForm = receive<AlteningGenForm>()

    AccountManager.generateAlteningAccount(accountForm.apiToken)
    respondNoContent()
}

// POST /api/v1/client/accounts/swap
fun RoutingContext.postSwapAccounts() {
    data class AccountForm(val from: Int, val to: Int)

    val accountForm = receive<AccountForm>()

    AccountManager.swapAccounts(accountForm.from, accountForm.to)
    respondNoContent()
}

// POST /api/v1/client/accounts/order
fun RoutingContext.postOrderAccounts() {
    data class AccountOrderRequest(val order: List<Int>)

    val accountOrderRequest = receive<AccountOrderRequest>()

    AccountManager.orderAccounts(accountOrderRequest.order)
    respondNoContent()
}

// POST /api/v1/client/accounts/login
fun RoutingContext.postLoginAccount() {
    data class AccountForm(val id: Int)

    val accountForm = receive<AccountForm>()

    AccountManager.loginAccount(accountForm.id)
    respondNoContent()
}

// POST /api/v1/client/accounts/cracked
fun RoutingContext.postLoginCrackedAccount() {
    data class AccountForm(val username: String, val online: Boolean?)

    val accountForm = receive<AccountForm>()

    AccountManager.loginCrackedAccount(accountForm.username, accountForm.online ?: false)
    respondNoContent()
}

// POST /api/v1/client/accounts/session
fun RoutingContext.postLoginSessionAccount() {
    data class AccountForm(val token: String)

    val accountForm = receive<AccountForm>()

    AccountManager.loginSessionAccount(accountForm.token)
    respondNoContent()
}

// POST /api/v1/client/accounts/restore
fun RoutingContext.postRestoreInitial() {
    AccountManager.restoreInitial()
    respond(mc.user, interopGson)
}

// PUT /api/v1/client/accounts/favorite
fun RoutingContext.putFavoriteAccount() {
    data class AccountForm(val id: Int)

    val accountForm = receive<AccountForm>()

    AccountManager.favoriteAccount(accountForm.id)
    respondNoContent()
}

// DELETE /api/v1/client/accounts/favorite
fun RoutingContext.deleteFavoriteAccount() {
    data class AccountForm(val id: Int)

    val accountForm = receive<AccountForm>()

    AccountManager.unfavoriteAccount(accountForm.id)
    respondNoContent()
}

// DELETE /api/v1/client/accounts
fun RoutingContext.deleteAccount() {
    data class AccountForm(val id: Int)

    val accountForm = receive<AccountForm>()
    val account = AccountManager.removeAccount(accountForm.id)

    respond(JsonObject().apply {
        addProperty("id", accountForm.id)

        val profile = account.profile ?: return@apply
        addProperty("username", profile.username)
        addProperty("uuid", profile.uuid.toString())
        addProperty("avatar", formatAvatarUrl(profile.uuid, profile.username))

        addProperty("type", account.type.commonName)
    })
}

// POST /api/v1/client/account/random-name
fun RoutingContext.generateName() {
    respond(JsonObject().apply {
        addProperty("name", randomUsername())
    })
}
