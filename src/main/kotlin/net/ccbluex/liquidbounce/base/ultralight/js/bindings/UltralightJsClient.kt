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
 */

package net.ccbluex.liquidbounce.base.ultralight.js.bindings

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.IpInfoApi
import net.ccbluex.liquidbounce.features.misc.AccountManager
import net.ccbluex.liquidbounce.features.misc.ProxyManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.session.Session

/**
 * Referenced by JS as `client`
 */
object UltralightJsClient {

    val moduleManager = ModuleManager
    val accountManager = AccountManager
    val proxyManager = ProxyManager

    val sessionService = MinecraftSession

    val uuid = mc.session.uuidOrNull

    fun exitClient() = mc.scheduleStop()

    fun isUpdateAvailable() = LiquidBounce.updateAvailable

    /**
     * Access session service from Ultralight
     */
    object MinecraftSession {

        fun getUsername(): String = mc.session.username

        /**
         * Get face url to be displayed on display
         *
         * TODO: pull URL service from API instead of hard coding the url
         */
        fun getFaceUrl() = "https://crafatar.com/avatars/${mc.session.uuidOrNull}?size=100"

        fun getFaceUrlByUUID(uuid: String) = "https://crafatar.com/avatars/$uuid}?size=100"

        /**
         * Get if an account is premium or cracked
         */
        fun getAccountType() =
            if ((mc.session.accountType == Session.AccountType.MOJANG || mc.session.accountType == Session.AccountType.MSA) && mc.session.accessToken.isNotBlank()) "Premium" else "Cracked"

        /**
         * Get location of session
         *
         * This depends on the current Geo IP of the user. This might be affected by the proxy service.
         */
        fun getLocation(): String = IpInfoApi.localIpInfo?.country?.lowercase() ?: "unknown"

    }

}
