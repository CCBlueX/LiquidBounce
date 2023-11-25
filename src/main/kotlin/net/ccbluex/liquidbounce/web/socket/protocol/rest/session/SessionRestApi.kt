/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.web.socket.protocol.rest.session

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.api.ClientApi
import net.ccbluex.liquidbounce.api.IpInfoApi
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.features.misc.AccountManager
import net.ccbluex.liquidbounce.utils.client.isPremium
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson

internal fun RestNode.setupSessionRestApi() {
    setupLocalSessionRestApi()
    setupAltManagerRestApi()
}

private fun RestNode.setupLocalSessionRestApi() {
    get("/session") {
        httpOk(JsonObject().apply {
            mc.session.let {
                addProperty("username", it.username)
                addProperty("uuid", it.uuid)
                addProperty("accountType", it.accountType.getName())
                addProperty("faceUrl", ClientApi.FACE_URL.format(mc.session.uuid))
                addProperty("premium", it.isPremium())
            }
        })
    }
    get("/location") {
        httpOk(
            protocolGson.toJsonTree(
                IpInfoApi.localIpInfo ?: return@get httpForbidden("location is not known (yet)")
            )
        )
    }
}

private fun RestNode.setupAltManagerRestApi() {
    get("/accounts") {
        val accounts = JsonArray()
        for ((i, account) in AccountManager.accounts.withIndex()) {
            accounts.add(JsonObject().apply {
                // Why are we not serializing the whole account?
                // -> We do not want to share the access token or anything relevant

                addProperty("id", i)
                addProperty("username", account.name)
                // Be careful with this, it will require a session refresh which takes time
                // addProperty("uuid", account.session.uuid)
                // addProperty("faceUrl", ClientApi.FACE_URL.format(account.session.uuid))
                addProperty("type", account.type)
            })
        }
        httpOk(accounts)
    }

    post("/account/login") {
        class AccountForm(
            val id: Int
        )
        val accountForm = decode<AccountForm>(it.content)
        AccountManager.loginAccount(accountForm.id)

        httpOk(JsonObject().apply {
            mc.session.let {
                addProperty("username", it.username)
                addProperty("uuid", it.uuid)
                addProperty("accountType", it.accountType.getName())
                addProperty("faceUrl", ClientApi.FACE_URL.format(mc.session.uuid))
                addProperty("premium", it.isPremium())
            }
        })
    }

    post("/accounts/new/cracked") {
        class AccountForm(
            val username: String
        )
        val accountForm = decode<AccountForm>(it.content)

        val account = AccountManager.newCrackedAccount(accountForm.username)
        httpOk(JsonObject().apply {
            addProperty("id", AccountManager.accounts.indexOf(account))
            addProperty("username", account.name)
        })
    }

    post("/accounts/new/microsoft") {
        AccountManager.newMicrosoftAccount()
        httpOk(JsonObject())
    }

    post("/accounts/new/altening") {
        class AlteningForm(
            val token: String
        )
        val accountForm = decode<AlteningForm>(it.content)
        AccountManager.newAlteningAccount(accountForm.token)
        httpOk(JsonObject())
    }

    post("/accounts/new/alteningGenerate") {
        class AlteningGenForm(
            val apiToken: String
        )
        val accountForm = decode<AlteningGenForm>(it.content)

        AccountManager.generateNewAlteningAccount(accountForm.apiToken)
        httpOk(JsonObject())
    }

    // Deletes a specific account
    delete("/account") {
        class AccountForm(
            val id: Int
        )

        val accountForm = decode<AccountForm>(it.content)
        AccountManager.accounts.removeAt(accountForm.id)
        httpOk(JsonObject())
    }

    // Clears all accounts
    delete("/accounts") {
        AccountManager.accounts.clear()
        httpOk(JsonObject())
    }

}
