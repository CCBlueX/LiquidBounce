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
package net.ccbluex.liquidbounce.web.interop.protocol.rest.v1

import net.ccbluex.liquidbounce.web.interop.protocol.rest.v1.client.*
import net.ccbluex.liquidbounce.web.interop.protocol.rest.v1.features.*
import net.ccbluex.liquidbounce.web.interop.protocol.rest.v1.game.*
import net.ccbluex.netty.http.rest.Node

internal fun v1Functions(node: Node) = node.run {
    // Client Functions
    get("/api/v1/client/info", ::getClientInfo)
    get("/api/v1/client/update", ::getUpdateInfo)
    post("/api/v1/client/exit", ::postExit)
    get("/api/v1/client/window", ::getWindowInfo)
    post("/api/v1/client/browse", ::postBrowse)

    // LocalStorage Functions
    get("/api/v1/client/localStorage/all", ::getAllLocalStorage)
    put("/api/v1/client/localStorage/all", ::putAllLocalStorage)
    get("/api/v1/client/localStorage", ::getLocalStorage)
    put("/api/v1/client/localStorage", ::putLocalStorage)
    delete("/api/v1/client/localStorage", ::deleteLocalStorage)

    // Theme Functions
    get("/api/v1/client/theme", ::getThemeInfo)
    post("/api/v1/client/theme/shader", ::postShaderState)
    post("/api/v1/client/theme/switch", ::postThemeSwitch)

    // VirtualScreen Functions
    get("/api/v1/client/virtualScreen", ::getVirtualScreenInfo)
    post("/api/v1/client/virtualScreen", ::postVirtualScreen)
    get("/api/v1/client/screen", ::getScreenInfo)
    get("/api/v1/client/screen/size", ::getScreenSize)
    put("/api/v1/client/screen", ::putScreen)

    // Module Functions
    get("/api/v1/client/modules", ::getModules)
    put("/api/v1/client/modules/toggle", ::toggleModule)
    delete("/api/v1/client/modules/toggle", ::toggleModule)
    post("/api/v1/client/modules/toggle", ::toggleModule)
    get("/api/v1/client/modules/settings", ::getSettings)
    put("/api/v1/client/modules/settings", ::putSettings)
    post("/api/v1/client/modules/panic", ::postPanic)

    // Component Functions
    get("/api/v1/client/components", ::getComponents)

    // Session Functions
    get("/api/v1/client/session", ::getSessionInfo)
    get("/api/v1/client/location", ::getLocationInfo)

    // Account Functions
    get("/api/v1/client/accounts", ::getAccounts)
    post("/api/v1/client/accounts/new/microsoft", ::postNewMicrosoftAccount)
    post("/api/v1/client/accounts/new/microsoft/clipboard", ::postClipboardMicrosoftAccount)
    post("/api/v1/client/accounts/new/cracked", ::postNewCrackedAccount)
    post("/api/v1/client/accounts/new/session", ::postNewSessionAccount)
    post("/api/v1/client/accounts/new/altening", ::postNewAlteningAccount)
    post("/api/v1/client/accounts/new/altening/generate", ::postGenerateAlteningAccount)
    post("/api/v1/client/accounts/swap", ::postSwapAccounts)
    post("/api/v1/client/accounts/order", ::postOrderAccounts)
    delete("/api/v1/client/account", ::deleteAccount)
    post("/api/v1/client/account/login", ::postLoginAccount)
    post("/api/v1/client/account/login/cracked", ::postLoginCrackedAccount)
    post("/api/v1/client/account/login/session", ::postLoginSessionAccount)
    post("/api/v1/client/account/restore", ::postRestoreInitial)
    put("/api/v1/client/account/favorite", ::putFavoriteAccount)
    delete("/api/v1/client/account/favorite", ::deleteFavoriteAccount)

    // Proxy Functions
    get("/api/v1/client/proxy", ::getProxyInfo)
    post("/api/v1/client/proxy", ::postProxy)
    delete("/api/v1/client/proxy", ::deleteProxy)
    get("/api/v1/client/proxies", ::getProxies)
    post("/api/v1/client/proxies/add", ::postAddProxy)
    post("/api/v1/client/proxies/clipboard", ::postClipboardProxy)
    post("/api/v1/client/proxies/edit", ::postEditProxy)
    post("/api/v1/client/proxies/check", ::postCheckProxy)
    delete("/api/v1/client/proxies/remove", ::deleteRemoveProxy)
    put("/api/v1/client/proxies/favorite", ::putFavoriteProxy)
    delete("/api/v1/client/proxies/favorite", ::deleteFavoriteProxy)

    // Browser Functions
    get("/api/v1/client/browser", ::getBrowserInfo)
    post("/api/v1/client/browser/navigate", ::postBrowserNavigate)
    post("/api/v1/client/browser/close", ::postBrowserClose)
    post("/api/v1/client/browser/reload", ::postBrowserReload)
    post("/api/v1/client/browser/forceReload", ::postBrowserForceReload)
    post("/api/v1/client/browser/forward", ::postBrowserForward)
    post("/api/v1/client/browser/back", ::postBrowserBack)
    post("/api/v1/client/browser/closeTab", ::postBrowserCloseTab)

    // Container Functions
    // TODO: Not being used but should be re-implemented in the future

    // Protocol Functions
    get("/api/v1/client/protocols", ::getProtocols)
    get("/api/v1/client/protocol", ::getProtocol)
    put("/api/v1/client/protocol", ::putProtocol)
    delete("/api/v1/client/protocol", ::deleteProtocol)

    // Reconnect Functions
    post("/api/v1/client/reconnect", ::postReconnect)

    // Input Functions
    get("/api/v1/client/input", ::getInputInfo)
    get("/api/v1/client/keybinds", ::getKeybinds)

    // Player Functions
    get("/api/v1/client/player", ::getPlayerData)

    // Registry Functions
    get("/api/v1/client/registries", ::getRegistries)

    // ServerList Functions
    get("/api/v1/client/servers", ::getServers)
    put("/api/v1/client/servers/add", ::putAddServer)
    delete("/api/v1/client/servers/remove", ::deleteServer)
    put("/api/v1/client/servers/edit", ::putEditServer)
    post("/api/v1/client/servers/swap", ::postSwapServers)
    post("/api/v1/client/servers/order", ::postOrderServers)
    post("/api/v1/client/servers/connect", ::postConnect)

    // Texture Functions
    get("/api/v1/client/resource", ::getResource)
    get("/api/v1/client/itemTexture", ::getItemTexture)
    get("/api/v1/client/skin", ::getSkin)

    // World Functions
    get("/api/v1/client/worlds", ::getWorlds)
    post("/api/v1/client/worlds/join", ::postJoinWorld)
    post("/api/v1/client/worlds/edit", ::postEditWorld)
    post("/api/v1/client/worlds/delete", ::postDeleteWorld)
}
