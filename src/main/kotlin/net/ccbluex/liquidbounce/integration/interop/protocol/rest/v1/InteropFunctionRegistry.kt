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

@file:Suppress("LongMethod")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1

import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.deleteAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.deleteFavoriteAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.deleteFavoriteProxy
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.deleteLocalStorage
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.deleteProxy
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.deleteRemoveProxy
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.deleteScreen
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.generateName
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getAccounts
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getAllLocalStorage
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getClientInfo
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getComponents
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getGlobalConfig
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getLocalStorage
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getLocationInfo
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getMarketplaceItem
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getMarketplaceItemReviews
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getMarketplaceItemRevision
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getMarketplaceItemRevisions
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getMarketplaceItems
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getModule
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getModules
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getProxies
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getProxyInfo
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getScreenInfo
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getScreenSize
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getSessionInfo
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getSettings
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getSpooferConfig
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getTheme
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getToggleShaderInfo
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getUpdateInfo
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getUser
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getVirtualScreenInfo
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.getWindowInfo
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.loginUser
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.logoutUser
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postAddProxy
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postBrowse
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postBrowsePath
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postCheckProxy
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postClipboardMicrosoftAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postClipboardProxy
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postEditProxy
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postExit
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postFileDialog
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postGenerateAlteningAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postLoginAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postLoginCrackedAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postLoginSessionAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postMarketplaceItemReview
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postNewAlteningAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postNewCrackedAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postNewMicrosoftAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postNewSessionAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postOrderAccounts
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postPanic
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postProxy
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postRestoreInitial
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postSwapAccounts
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.postToggleShader
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.putAllLocalStorage
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.putFavoriteAccount
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.putFavoriteProxy
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.putGlobalConfig
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.putLocalStorage
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.putScreen
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.putSettings
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.putSpooferConfig
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.subscribeMarketplaceItem
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.toggleModule
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.unsubscribeMarketplaceItem
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.deleteProtocol
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.getBrowserInfo
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.getProtocol
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.getProtocols
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.postBrowserBack
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.postBrowserClose
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.postBrowserCloseTab
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.postBrowserForceReload
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.postBrowserForward
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.postBrowserNavigate
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.postBrowserReload
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.postReconnect
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.putProtocol
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.deleteServer
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getCrosshairData
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getEffectTexture
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getInputInfo
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getIsTyping
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getItemTexture
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getKeybinds
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getPlayerData
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getPlayerInventory
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getRegistry
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getRegistryGroups
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getResource
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getServers
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getSkin
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getWorlds
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.isTyping
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.postConnect
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.postDeleteWorld
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.postEditWorld
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.postJoinWorld
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.postOrderServers
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.postSwapServers
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.putAddServer
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.putEditServer
import net.ccbluex.netty.http.rest.Node

internal fun Node.registerInteropFunctions() = withPath("/api/v1/client") {
    // Client Functions
    get("/info", ::getClientInfo)
    get("/update", ::getUpdateInfo)
    post("/exit", ::postExit)
    get("/window", ::getWindowInfo)
    post("/browse", ::postBrowse)

    // User Functions
    get("/user", ::getUser).apply {
        post("/login", ::loginUser)
        post("/logout", ::logoutUser)
    }

    // OS File Functions
    post("/browsePath", ::postBrowsePath)
    post("/fileDialog", ::postFileDialog)

    // LocalStorage Functions
    get("/localStorage/all", ::getAllLocalStorage)
    put("/localStorage/all", ::putAllLocalStorage)
    get("/localStorage", ::getLocalStorage)
    put("/localStorage", ::putLocalStorage)
    delete("/localStorage", ::deleteLocalStorage)

    // Theme Functions
    get("/theme", ::getTheme) // returns current theme
    get("/theme/:id", ::getTheme)
    get("/shader", ::getToggleShaderInfo)
    post("/shader", ::postToggleShader)

    // VirtualScreen Functions
    get("/virtualScreen", ::getVirtualScreenInfo)
    get("/screen", ::getScreenInfo)
    get("/screen/size", ::getScreenSize)
    put("/screen", ::putScreen)
    delete("/screen", ::deleteScreen)

    // Module Functions
    get("/modules", ::getModules).apply {
        put("/toggle", ::toggleModule)
        delete("/toggle", ::toggleModule)
        post("/toggle", ::toggleModule)
        get("/settings", ::getSettings)
        put("/settings", ::putSettings)
        post("/panic", ::postPanic)
    }
    get("/module/:name", ::getModule)

    // Component Functions
    get("/components", ::getComponents)
    get("/components/:id", ::getComponents)

    // Session Functions
    get("/session", ::getSessionInfo)
    get("/location", ::getLocationInfo)

    // Account Functions
    get("/accounts", ::getAccounts)
    post("/accounts/new/microsoft", ::postNewMicrosoftAccount)
    post("/accounts/new/microsoft/clipboard", ::postClipboardMicrosoftAccount)
    post("/accounts/new/cracked", ::postNewCrackedAccount)
    post("/accounts/new/session", ::postNewSessionAccount)
    post("/accounts/new/altening", ::postNewAlteningAccount)
    post("/accounts/new/altening/generate", ::postGenerateAlteningAccount)
    post("/accounts/swap", ::postSwapAccounts)
    post("/accounts/order", ::postOrderAccounts)
    delete("/account", ::deleteAccount)
    post("/account/login", ::postLoginAccount)
    post("/account/login/cracked", ::postLoginCrackedAccount)
    post("/account/login/session", ::postLoginSessionAccount)
    post("/account/restore", ::postRestoreInitial)
    put("/account/favorite", ::putFavoriteAccount)
    delete("/account/favorite", ::deleteFavoriteAccount)
    post("/account/random-name", ::generateName)

    // Proxy Functions
    get("/proxy", ::getProxyInfo)
    post("/proxy", ::postProxy)
    delete("/proxy", ::deleteProxy)
    get("/proxies", ::getProxies).apply {
        post("/add", ::postAddProxy)
        post("/add/clipboard", ::postClipboardProxy)
        post("/edit", ::postEditProxy)
        post("/check", ::postCheckProxy)
        delete("/remove", ::deleteRemoveProxy)
        put("/favorite", ::putFavoriteProxy)
        delete("/favorite", ::deleteFavoriteProxy)
    }

    // Browser Functions
    get("/browser", ::getBrowserInfo).apply {
        post("/navigate", ::postBrowserNavigate)
        post("/close", ::postBrowserClose)
        post("/reload", ::postBrowserReload)
        post("/forceReload", ::postBrowserForceReload)
        post("/forward", ::postBrowserForward)
        post("/back", ::postBrowserBack)
        post("/closeTab", ::postBrowserCloseTab)
    }

    // Container Functions
    // TODO: Not being used but should be re-implemented in the future

    // Protocol Functions
    get("/protocols", ::getProtocols).apply {
        get("/protocol", ::getProtocol)
        put("/protocol", ::putProtocol)
        delete("/protocol", ::deleteProtocol)
    }

    // Reconnect Functions
    post("/reconnect", ::postReconnect)

    // Spoofer Functions
    get("/spoofer", ::getSpooferConfig)
    put("/spoofer", ::putSpooferConfig)
    // Global Functions
    get("/global", ::getGlobalConfig)
    put("/global", ::putGlobalConfig)

    // Input Functions
    get("/input", ::getInputInfo)
    get("/keybinds", ::getKeybinds)
    post("/typing", ::isTyping)
    get("/typing", ::getIsTyping)

    // Player Functions
    get("/player", ::getPlayerData)
    get("/player/inventory", ::getPlayerInventory)
    get("/crosshair", ::getCrosshairData)

    // Registry Functions
    get("/registry/:name", ::getRegistry)
    get("/registry/:name/groups", ::getRegistryGroups)

    // ServerList Functions
    get("/servers", ::getServers).apply {
        put("/add", ::putAddServer)
        delete("/remove", ::deleteServer)
        put("/edit", ::putEditServer)
        post("/swap", ::postSwapServers)
        post("/order", ::postOrderServers)
        post("/connect", ::postConnect)
    }

    // Texture Functions
    get("/resource", ::getResource).apply {
        get("/itemTexture", ::getItemTexture)
        get("/effectTexture", ::getEffectTexture)
        get("/skin", ::getSkin)
    }

    // World Functions
    get("/worlds", ::getWorlds).apply {
        post("/join", ::postJoinWorld)
        post("/edit", ::postEditWorld)
        post("/delete", ::postDeleteWorld)
    }

    // Marketplace Functions
    get("/marketplace", ::getMarketplaceItems).apply {
        get("/:id", ::getMarketplaceItem)
        get("/:id/revisions", ::getMarketplaceItemRevisions)
        get("/:id/revisions/:revisionId", ::getMarketplaceItemRevision)
        post("/:id/subscribe", ::subscribeMarketplaceItem)
        post("/:id/unsubscribe", ::unsubscribeMarketplaceItem)
        get("/:id/reviews", ::getMarketplaceItemReviews)
        post("/:id/reviews", ::postMarketplaceItemReview)
    }
}
