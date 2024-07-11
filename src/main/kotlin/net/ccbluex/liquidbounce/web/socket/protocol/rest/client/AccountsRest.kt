/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.web.socket.protocol.rest.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.api.ClientApi
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AccountManagerMessageEvent
import net.ccbluex.liquidbounce.features.misc.AccountManager
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import org.lwjgl.glfw.GLFW

fun RestNode.accountsRest() {
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
                addProperty("avatar", ClientApi.formatAvatarUrl(profile.uuid, profile.username))
                add("bans", protocolGson.toJsonTree(account.bans))
                addProperty("type", account.type)
                addProperty("favorite", account.favorite)
            })
        }
        httpOk(accounts)
    }.apply {
        post("/new/microsoft") {
            AccountManager.newMicrosoftAccount {
                browseUrl(it)
                EventManager.callEvent(AccountManagerMessageEvent("Opened login url in browser"))
            }
            httpOk(JsonObject())
        }.apply {
            post("/clipboard") {
                AccountManager.newMicrosoftAccount {
                    RenderSystem.recordRenderCall {
                        GLFW.glfwSetClipboardString(mc.window.handle, it)
                        EventManager.callEvent(AccountManagerMessageEvent("Copied login url to clipboard"))
                    }
                }

                httpOk(JsonObject())
            }
        }

        post("/new/cracked") {
            class AccountForm(
                val username: String,
                val online: Boolean?
            )
            val accountForm = decode<AccountForm>(it.content)

            AccountManager.newCrackedAccount(accountForm.username, accountForm.online ?: false)
            httpOk(JsonObject())
        }

        post("/new/session") {
            class AccountForm(
                val token: String
            )
            val accountForm = decode<AccountForm>(it.content)

            AccountManager.newSessionAccount(accountForm.token)
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

        post("/new/easymc") {
            class AlteningForm(
                val token: String
            )
            val accountForm = decode<AlteningForm>(it.content)
            AccountManager.newEasyMCAccount(accountForm.token)
            httpOk(JsonObject())
        }

        post("/swap") {
            class AccountForm(
                val from: Int,
                val to: Int
            )

            val accountForm = decode<AccountForm>(it.content)
            AccountManager.swapAccounts(accountForm.from, accountForm.to)
            httpOk(JsonObject())
        }

        post("/order") {
            data class AccountOrderRequest(val order: List<Int>)
            val accountOrderRequest = decode<AccountOrderRequest>(it.content)

            AccountManager.orderAccounts(accountOrderRequest.order)
            httpOk(JsonObject())
        }
    }

    post("/account/login") {
        class AccountForm(
            val id: Int
        )
        val accountForm = decode<AccountForm>(it.content)
        AccountManager.loginAccount(accountForm.id)

        httpOk(JsonObject())
    }.apply {
        post("/cracked") {
            class AccountForm(
                val username: String,
                val online: Boolean?
            )
            val accountForm = decode<AccountForm>(it.content)
            AccountManager.loginCrackedAccount(accountForm.username, accountForm.online ?: false)
            httpOk(JsonObject())
        }

        post("/session") {
            class AccountForm(
                val token: String
            )
            val accountForm = decode<AccountForm>(it.content)
            AccountManager.loginSessionAccount(accountForm.token)
            httpOk(JsonObject())
        }

        post("/easymc") {
            class AccountForm(
                val token: String
            )
            val accountForm = decode<AccountForm>(it.content)
            AccountManager.loginEasyMCAccount(accountForm.token)
            httpOk(JsonObject())
        }
    }

    post("/account/restore") {
        AccountManager.restoreInitial()
        httpOk(protocolGson.toJsonTree(mc.session))
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

    delete("/account") {
        class AccountForm(
            val id: Int
        )

        val accountForm = decode<AccountForm>(it.content)
        val account = AccountManager.removeAccount(accountForm.id)

        httpOk(JsonObject().apply {
            addProperty("id", accountForm.id)

            val profile = account.profile ?: return@apply
            addProperty("username", profile.username)
            addProperty("uuid", profile.uuid.toString())
            addProperty("avatar", ClientApi.formatAvatarUrl(profile.uuid, profile.username))
            addProperty("type", account.type)
        })
    }

}
