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

import com.mojang.authlib.GameProfile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.models.cosmetics.CosmeticCategory
import net.ccbluex.liquidbounce.api.services.cosmetics.CapeApi
import net.ccbluex.liquidbounce.event.ActiveSessionListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.registerTexture
import net.minecraft.resources.Identifier
import java.util.function.Consumer

/**
 * A cape cosmetic manager
 */
object CapeCosmeticsManager : ActiveSessionListener {

    private val logger = clientLogger("CapeCosmeticsManager")

    /**
     * Cached capes
     *
     * This is OK because the cape texture is only loaded for players that own a cape.
     * This is very rare for most people, and therefore the cache is not that big.
     * We also don't need to worry about memory leaks
     * because the cache is cleared when the player disconnects from the world.
     */
    private val cachedCapes = hashMapOf<String, Identifier>()

    /**
     * Loads a player cape
     *
     * @param player The player to load the cape for
     * @param callback The callback to call with the cape texture identifier
     */
    fun loadPlayerCape(player: GameProfile, callback: Consumer<Identifier>) =
        loadPlayerTexture(player, CosmeticCategory.CAPE, callback)

    /**
     * Loads a custom elytra, a texture in the cape layout
     */
    fun loadPlayerElytra(player: GameProfile, callback: Consumer<Identifier>) =
        loadPlayerTexture(player, CosmeticCategory.ELYTRA, callback)

    private fun loadPlayerTexture(player: GameProfile, category: CosmeticCategory, callback: Consumer<Identifier>) {
        ioScope.launch {
            val uuid = player.id

            // Asked once per player, so it waits for the carriers to be up to date
            CosmeticService.refreshCarriers()

            CosmeticService.fetchCosmetic(uuid, category) { cosmetic ->
                // Get url of cape from cape service
                val name = cosmetic.extra ?: return@fetchCosmetic

                // Check if the cape is cached
                val cachedCapeId = cachedCapes[name]
                if (cachedCapeId != null) {
                    logger.info("Successfully loaded cached $category texture for ${player.name}")
                    callback.accept(cachedCapeId)
                    return@fetchCosmetic
                }

                // Request cape texture
                val nativeImage = runCatching {
                    runBlocking {
                        CapeApi.getCape(name)
                    }
                }.getOrNull() ?: return@fetchCosmetic

                logger.info("Successfully loaded $category texture for ${player.name}")

                val id = LiquidBounce.identifier("cape-$name")

                mc.execute {
                    // Register cape texture
                    nativeImage.registerTexture(id)

                    // Cache cape texture
                    cachedCapes[name] = id

                    // Return cape texture
                    callback.accept(id)
                }
            }
        }
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        cachedCapes.values.forEach { mc.textureManager.release(it) }
        cachedCapes.clear()
    }

}
