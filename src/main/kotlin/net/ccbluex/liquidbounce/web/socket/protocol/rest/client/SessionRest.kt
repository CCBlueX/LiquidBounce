/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.web.socket.protocol.rest.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.api.ClientApi.formatAvatarUrl
import net.ccbluex.liquidbounce.api.IpInfoApi
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AltManagerUpdateEvent
import net.ccbluex.liquidbounce.features.misc.AccountManager
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.isPremium
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import net.minecraft.client.session.Session
import org.lwjgl.glfw.GLFW

fun RestNode.sessionRest() {
    get("/session") {
        httpOk(JsonObject().apply {
            mc.session.let {
                addProperty("username", it.username)
                addProperty("uuid", it.uuidOrNull.toString())
                addProperty("accountType", it.accountType.getName())
                addProperty("avatar", formatAvatarUrl(it.uuidOrNull, it.username))
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

    setupAccountManagerRest()
}

private fun RestNode.setupAccountManagerRest() {
    get("/accounts") {
        val accounts = JsonArray()
        for ((i, account) in AccountManager.accounts.withIndex()) {
            val profile = account.profile ?: continue

            accounts.add(JsonObject().apply {
                // Why are we not serializing the whole account?
                // -> We do not want to share the access token or anything relevant

                addProperty("id", i)
                addProperty("username", profile.username)
                addProperty("uuid", profile.uuid.toString())
                addProperty("avatar", formatAvatarUrl(profile.uuid, profile.username))
                add("bans", protocolGson.toJsonTree(account.bans))
                addProperty("type", account.type)
            })
        }
        httpOk(accounts)
    }.apply {
        post("/new/microsoft") {
            AccountManager.newMicrosoftAccount {
                browseUrl(it)
                EventManager.callEvent(AltManagerUpdateEvent(true, "Opened login url in browser"))
            }
            httpOk(JsonObject())
        }.apply {
            post("/clipboard") {
                AccountManager.newMicrosoftAccount {
                    RenderSystem.recordRenderCall {
                        GLFW.glfwSetClipboardString(mc.window.handle, it)
                        EventManager.callEvent(AltManagerUpdateEvent(true, "Copied login url to clipboard"))
                    }
                }

                httpOk(JsonObject())
            }
        }

        post("/new/cracked") {
            class AccountForm(
                val username: String
            )
            val accountForm = decode<AccountForm>(it.content)

            AccountManager.newCrackedAccount(accountForm.username)
            httpOk(JsonObject())
        }

        post("/new/altening") {
            class AlteningForm(
                val token: String
            )
            val accountForm = decode<AlteningForm>(it.content)
            AccountManager.newAlteningAccount(accountForm.token)
            httpOk(JsonObject())
        }.apply {
            post("/generate") {
                class AlteningGenForm(
                    val apiToken: String
                )
                val accountForm = decode<AlteningGenForm>(it.content)

                AccountManager.generateAlteningAccount(accountForm.apiToken)
                httpOk(JsonObject())
            }
        }
    }

    post("/account/login") {
        class AccountForm(
            val id: Int
        )
        val accountForm = decode<AccountForm>(it.content)
        AccountManager.loginAccountAsync(accountForm.id)

        httpOk(JsonObject())
    }.apply {
        post("/cracked") {
            class AccountForm(
                val username: String
            )
            val accountForm = decode<AccountForm>(it.content)
            AccountManager.loginCrackedAccountAsync(accountForm.username)
            httpOk(JsonObject())
        }
    }

    post("/account/restore") {
        AccountManager.restoreInitial()
        httpOk(mc.session.toJsonObject())
    }

    put("/account/favorite") {
        class AccountForm(
            val id: Int
        )

        val accountForm = decode<AccountForm>(it.content)
        AccountManager.favoriteAccount(accountForm.id)
        httpOk(JsonObject())
    }

    delete("/account/favorite") {
        class AccountForm(
            val id: Int
        )

        val accountForm = decode<AccountForm>(it.content)
        AccountManager.unfavoriteAccount(accountForm.id)
        httpOk(JsonObject())
    }

    post("/account/swap") {
        class AccountForm(
            val from: Int,
            val to: Int
        )

        val accountForm = decode<AccountForm>(it.content)
        AccountManager.swapAccounts(accountForm.from, accountForm.to)
        httpOk(JsonObject())
    }

    delete("/account") {
        class AccountForm(
            val id: Int
        )

        val accountForm = decode<AccountForm>(it.content)
        val account = AccountManager.accounts.removeAt(accountForm.id)

        httpOk(JsonObject().apply {
            addProperty("id", accountForm.id)

            val profile = account.profile ?: return@apply
            addProperty("username", profile.username)
            addProperty("uuid", profile.uuid.toString())
            addProperty("avatar", formatAvatarUrl(profile.uuid, profile.username))
            addProperty("type", account.type)
        })
    }

}

private fun Session.toJsonObject() = JsonObject().apply {
    addProperty("username", username)
    addProperty("uuid", uuidOrNull.toString())
    addProperty("accountType", accountType.getName())
    addProperty("avatar", formatAvatarUrl(uuidOrNull, username))
    addProperty("premium", isPremium())
}
