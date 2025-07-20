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
package net.ccbluex.liquidbounce.features.cosmetic

import com.mojang.authlib.GameProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.api.models.cosmetics.Cosmetic
import net.ccbluex.liquidbounce.api.models.cosmetics.CosmeticCategory
import net.ccbluex.liquidbounce.api.services.cosmetics.CapeApi
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.util.Identifier

/**
 * A cape cosmetic manager
 */
object CapeCosmeticsManager : EventListener {

    /**
     * Cached capes
     *
     * This is OK because the cape texture is only loaded for players that own a cape.
     * This is very rare for most people, and therefore the cache is not that big.
     * We also don't need to worry about memory leaks
     * because the cache is cleared when the player disconnects from the world.
     */
    private val cachedCapes = mutableMapOf<String, Identifier>()

    /**
     * Interface for returning a cape texture
     */
    interface ReturnCapeTexture {

        /**
         * Returns the cape texture when it is loaded
         */
        fun response(id: Identifier)

    }

    /**
     * Loads a player cape
     */
    fun loadPlayerCape(player: GameProfile, response: ReturnCapeTexture) {
        withScope {
            runCatching {
                val uuid = player.id

                CosmeticService.fetchCosmetic(uuid, CosmeticCategory.CAPE) { cosmetic ->
                    // Get url of cape from cape service
                    val name = getCapeName(cosmetic) ?: return@fetchCosmetic

                    // Check if the cape is cached
                    if (cachedCapes.containsKey(name)) {
                        LiquidBounce.logger.info("Successfully loaded cached cape for ${player.name}")
                        response.response(cachedCapes[name]!!)
                        return@fetchCosmetic
                    }

                    // Request cape texture
                    val nativeImageBackedTexture = runCatching {
                        runBlocking(Dispatchers.IO) {
                            CapeApi.getCape(name)
                        }
                    }.getOrNull() ?: return@fetchCosmetic

                    LiquidBounce.logger.info("Successfully loaded cape for ${player.name}")

                    val id = Identifier.of("liquidbounce", "cape-$name")

                    // Register cape texture
                    mc.textureManager.registerTexture(id, nativeImageBackedTexture)

                    // Cache cape texture
                    cachedCapes[name] = id

                    // Return cape texture
                    response.response(id)
                }
            }
        }
    }

    private fun getCapeName(cosmetic: Cosmetic): String? {
        // Check if cosmetic is a cape
        if (cosmetic.category != CosmeticCategory.CAPE) return null
        return cosmetic.extra
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        cachedCapes.values.forEach { mc.textureManager.destroyTexture(it) }
        cachedCapes.clear()
    }

}
