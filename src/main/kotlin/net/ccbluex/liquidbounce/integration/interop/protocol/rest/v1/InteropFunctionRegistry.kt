/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

@file:Suppress("LongMethod")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1

import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.*
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.*
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.*
import net.ccbluex.netty.http.rest.Node

internal fun registerInteropFunctions(node: Node) = node.withPath("/api/v1/client") {
    // Client Functions
    get("/info", ::getClientInfo)
    get("/update", ::getUpdateInfo)
    post("/exit", ::postExit)
    get("/window", ::getWindowInfo)
    post("/browse", ::postBrowse)

    // LocalStorage Functions
    get("/localStorage/all", ::getAllLocalStorage)
    put("/localStorage/all", ::putAllLocalStorage)
    get("/localStorage", ::getLocalStorage)
    put("/localStorage", ::putLocalStorage)
    delete("/localStorage", ::deleteLocalStorage)

    // Theme Functions
    get("/theme", ::getThemeInfo)
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
        post("/clipboard", ::postClipboardProxy)
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
    get("/spoofer", ::getSpooferConfigurable)
    put("/spoofer", ::putSpooferConfigurable)

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
        get("/skin", ::getSkin)
    }

    // World Functions
    get("/worlds", ::getWorlds).apply {
        post("/join", ::postJoinWorld)
        post("/edit", ::postEditWorld)
        post("/delete", ::postDeleteWorld)
    }
}
