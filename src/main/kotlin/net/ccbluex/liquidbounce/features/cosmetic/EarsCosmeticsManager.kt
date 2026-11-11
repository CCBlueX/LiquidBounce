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
package net.ccbluex.liquidbounce.features.cosmetic

import com.mojang.blaze3d.platform.NativeImage
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.models.cosmetics.CosmeticCategory
import net.ccbluex.liquidbounce.api.services.cosmetics.CosmeticApi
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.registerTexture
import net.minecraft.resources.Identifier
import java.util.UUID

object EarsCosmeticsManager {

    private const val MATCH_CAPE = "cape"
    private const val DEFAULT_CAPE = "donator"

    private val logger = clientLogger("EarsCosmeticsManager")

    private val textures = hashMapOf<String, Identifier>()
    private val requested = hashSetOf<String>()

    /**
     * The ears texture of [uuid], or null while it has none or it is still loading.
     */
    fun earsTexture(uuid: UUID): Identifier? {
        val cosmetic = CosmeticService.getCosmetic(uuid, CosmeticCategory.DEADMAU5_EARS) ?: return null
        val name = when (val extra = cosmetic.extra) {
            null, MATCH_CAPE ->
                "cape_" + (CosmeticService.getCosmetic(uuid, CosmeticCategory.CAPE)?.extra ?: DEFAULT_CAPE)
            else -> extra
        }

        textures[name]?.let { return it }
        if (requested.add(name)) {
            load(name)
        }
        return null
    }

    /**
     * @param name `cape_<cape>`, `color_<rrggbb>` or an ears style, as the API serves them
     */
    private fun load(name: String) = ioScope.launch {
        val skin = runCatching {
            CosmeticApi.getEars(name).use { it.toSkinLayout() }
        }.onFailure {
            logger.error("Failed to load ears $name", it)
        }.getOrNull() ?: return@launch

        mc.execute {
            val id = LiquidBounce.identifier("ears-${name.lowercase()}")
            skin.registerTexture(id)
            textures[name] = id
        }
    }

    /**
     * Places a standalone ears texture (14x7, or a multiple of it) where the ears model
     * samples a 64x64 skin, at (24, 0).
     */
    private fun NativeImage.toSkinLayout(): NativeImage {
        require(width == height * 2 && height % 7 == 0) { "Ears texture is ${width}x$height" }

        val scale = height / 7
        val skin = NativeImage(64 * scale, 64 * scale, true)
        for (x in 0 until width) {
            for (y in 0 until height) {
                skin.setPixel(24 * scale + x, y, getPixel(x, y))
            }
        }
        return skin
    }

}
