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
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.authlib.utils.generateOfflinePlayerUuid
import net.ccbluex.liquidbounce.authlib.yggdrasil.GameProfileRepository
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.client.util.SkinTextures
import java.util.function.Supplier

object ModuleSkinChanger : ClientModule("SkinChanger", Category.RENDER) {

    private val username by text("Username", "LiquidBounce").onChanged { username ->
        this.skinTextures = textureSupplier(username)
    }.apply(::tagBy)

    var skinTextures: Supplier<SkinTextures>
        private set

    init {
        this.skinTextures = textureSupplier(username)
    }

    private fun textureSupplier(username: String): Supplier<SkinTextures> {
        val uuid = GameProfileRepository().fetchUuidByUsername(username)
            ?: generateOfflinePlayerUuid(username)
        val profile = mc.sessionService.fetchProfile(uuid, false)?.profile
            ?: GameProfile(uuid, username)

        // todo: fix not showing skin textures after they were loaded
        return PlayerListEntry.texturesSupplier(profile)
    }

}
