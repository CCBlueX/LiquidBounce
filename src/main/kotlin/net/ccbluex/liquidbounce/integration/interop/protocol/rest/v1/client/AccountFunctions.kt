@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.blaze3d.systems.RenderSystem
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.api.core.formatAvatarUrl
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AccountManagerMessageEvent
import net.ccbluex.liquidbounce.features.misc.AccountManager
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.randomUsername
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk
import org.lwjgl.glfw.GLFW

// GET /api/v1/client/accounts
@Suppress("UNUSED_PARAMETER")
fun getAccounts(requestObject: RequestObject): FullHttpResponse {
    val accounts = JsonArray()
    for ((i, account) in AccountManager.accounts.withIndex()) {
        val profile = account.profile ?: continue

        accounts.add(JsonObject().apply {
            addProperty("id", i)
            addProperty("username", profile.username)
            addProperty("uuid", profile.uuid.toString())
            addProperty("avatar", formatAvatarUrl(profile.uuid, profile.username))
            add("bans", interopGson.toJsonTree(account.bans))
            addProperty("type", account.type)
            addProperty("favorite", account.favorite)
        })
    }
    return httpOk(accounts)
}

// POST /api/v1/client/accounts/new/microsoft
@Suppress("UNUSED_PARAMETER")
fun postNewMicrosoftAccount(requestObject: RequestObject): FullHttpResponse {
    AccountManager.newMicrosoftAccount {
        browseUrl(it)
        EventManager.callEvent(AccountManagerMessageEvent("Opened login url in browser"))
    }
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/accounts/clipboard
@Suppress("UNUSED_PARAMETER")
fun postClipboardMicrosoftAccount(requestObject: RequestObject): FullHttpResponse {
    AccountManager.newMicrosoftAccount {
        RenderSystem.recordRenderCall {
            GLFW.glfwSetClipboardString(mc.window.handle, it)
            EventManager.callEvent(AccountManagerMessageEvent("Copied login url to clipboard"))
        }
    }
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/accounts/new/cracked
@Suppress("UNUSED_PARAMETER")
fun postNewCrackedAccount(requestObject: RequestObject): FullHttpResponse {
    data class AccountForm(val username: String, val online: Boolean?)
    val accountForm = requestObject.asJson<AccountForm>()

    AccountManager.newCrackedAccount(accountForm.username, accountForm.online ?: false)
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/accounts/new/session
@Suppress("UNUSED_PARAMETER")
fun postNewSessionAccount(requestObject: RequestObject): FullHttpResponse {
    data class AccountForm(val token: String)
    val accountForm = requestObject.asJson<AccountForm>()

    AccountManager.newSessionAccount(accountForm.token)
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/accounts/new/altening
@Suppress("UNUSED_PARAMETER")
fun postNewAlteningAccount(requestObject: RequestObject): FullHttpResponse {
    data class AlteningForm(val token: String)
    val accountForm = requestObject.asJson<AlteningForm>()
    AccountManager.newAlteningAccount(accountForm.token)
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/accounts/generate
@Suppress("UNUSED_PARAMETER")
fun postGenerateAlteningAccount(requestObject: RequestObject): FullHttpResponse {
    data class AlteningGenForm(val apiToken: String)
    val accountForm = requestObject.asJson<AlteningGenForm>()

    AccountManager.generateAlteningAccount(accountForm.apiToken)
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/accounts/swap
@Suppress("UNUSED_PARAMETER")
fun postSwapAccounts(requestObject: RequestObject): FullHttpResponse {
    data class AccountForm(val from: Int, val to: Int)
    val accountForm = requestObject.asJson<AccountForm>()

    AccountManager.swapAccounts(accountForm.from, accountForm.to)
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/accounts/order
@Suppress("UNUSED_PARAMETER")
fun postOrderAccounts(requestObject: RequestObject): FullHttpResponse {
    data class AccountOrderRequest(val order: List<Int>)
    val accountOrderRequest = requestObject.asJson<AccountOrderRequest>()

    AccountManager.orderAccounts(accountOrderRequest.order)
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/accounts/login
@Suppress("UNUSED_PARAMETER")
fun postLoginAccount(requestObject: RequestObject): FullHttpResponse {
    data class AccountForm(val id: Int)
    val accountForm = requestObject.asJson<AccountForm>()

    AccountManager.loginAccount(accountForm.id)
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/accounts/cracked
@Suppress("UNUSED_PARAMETER")
fun postLoginCrackedAccount(requestObject: RequestObject): FullHttpResponse {
    data class AccountForm(val username: String, val online: Boolean?)
    val accountForm = requestObject.asJson<AccountForm>()

    AccountManager.loginCrackedAccount(accountForm.username, accountForm.online ?: false)
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/accounts/session
@Suppress("UNUSED_PARAMETER")
fun postLoginSessionAccount(requestObject: RequestObject): FullHttpResponse {
    data class AccountForm(val token: String)
    val accountForm = requestObject.asJson<AccountForm>()

    AccountManager.loginSessionAccount(accountForm.token)
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/accounts/restore
@Suppress("UNUSED_PARAMETER")
fun postRestoreInitial(requestObject: RequestObject): FullHttpResponse {
    AccountManager.restoreInitial()
    return httpOk(interopGson.toJsonTree(mc.session))
}

// PUT /api/v1/client/accounts/favorite
@Suppress("UNUSED_PARAMETER")
fun putFavoriteAccount(requestObject: RequestObject): FullHttpResponse {
    data class AccountForm(val id: Int)
    val accountForm = requestObject.asJson<AccountForm>()

    AccountManager.favoriteAccount(accountForm.id)
    return httpOk(emptyJsonObject())
}

// DELETE /api/v1/client/accounts/favorite
@Suppress("UNUSED_PARAMETER")
fun deleteFavoriteAccount(requestObject: RequestObject): FullHttpResponse {
    data class AccountForm(val id: Int)
    val accountForm = requestObject.asJson<AccountForm>()

    AccountManager.unfavoriteAccount(accountForm.id)
    return httpOk(emptyJsonObject())
}

// DELETE /api/v1/client/accounts
@Suppress("UNUSED_PARAMETER")
fun deleteAccount(requestObject: RequestObject): FullHttpResponse {
    data class AccountForm(val id: Int)
    val accountForm = requestObject.asJson<AccountForm>()
    val account = AccountManager.removeAccount(accountForm.id)

    return httpOk(JsonObject().apply {
        addProperty("id", accountForm.id)

        val profile = account.profile ?: return@apply
        addProperty("username", profile.username)
        addProperty("uuid", profile.uuid.toString())
        addProperty("avatar", formatAvatarUrl(profile.uuid, profile.username))
        addProperty("type", account.type)
    })
}

// POST /api/v1/client/account/random-name
@Suppress("UNUSED_PARAMETER")
fun generateName(requestObject: RequestObject): FullHttpResponse {
    return httpOk(JsonObject().apply {
        addProperty("name", randomUsername())
    })
}
