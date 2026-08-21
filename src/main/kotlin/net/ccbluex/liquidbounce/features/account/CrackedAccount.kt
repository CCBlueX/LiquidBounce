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

package net.ccbluex.liquidbounce.features.account

import com.google.gson.JsonObject
import com.mojang.authlib.GameProfile
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.api.thirdparty.lookupUuidByName
import net.ccbluex.liquidbounce.config.gson.util.boolean
import net.minecraft.core.UUIDUtil

/**
 * A cracked account - has no credentials and cannot join premium servers.
 *
 * With [online] set, the real UUID of the name is looked up so that skins resolve; otherwise the
 * offline UUID Minecraft derives from the name is used.
 */
class CrackedAccount(username: String, private var online: Boolean = false) :
    MinecraftAccount(AccountService.CRACKED) {

    init {
        this.username = username
    }

    /**
     * Used for JSON deserialize.
     */
    @Suppress("unused")
    constructor() : this("", false)

    override fun refresh() {
        val uuid = if (online) {
            runCatching { runBlocking { lookupUuidByName(username) } }.getOrNull()
        } else {
            null
        }

        profile = if (uuid == null) UUIDUtil.createOfflineProfile(username) else GameProfile(uuid, username)
    }

    override fun acquireAccessToken() = "-"

    override fun toRawJson(json: JsonObject) = json.run {
        writeProfile()
        addProperty("online", online)
    }

    override fun fromRawJson(json: JsonObject) = json.run {
        readProfile()
        online = boolean("online") ?: false
    }

}
