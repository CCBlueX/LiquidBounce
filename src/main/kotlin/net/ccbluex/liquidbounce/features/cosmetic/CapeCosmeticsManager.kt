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
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.api.models.cosmetics.Cosmetic
import net.ccbluex.liquidbounce.api.models.cosmetics.CosmeticCategory
import net.ccbluex.liquidbounce.api.services.cosmetics.CapeApi
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.registerTexture
import net.minecraft.resources.Identifier
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.function.Consumer

/**
 * A cape cosmetic manager
 */
object CapeCosmeticsManager : EventListener {

    private val logger: Logger = LogManager.getLogger("$CLIENT_NAME/CapeCosmeticsManager")

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
    fun loadPlayerCape(player: GameProfile, callback: Consumer<Identifier>) {
        withScope {
            runCatching {
                val uuid = player.id

                CosmeticService.fetchCosmetic(uuid, CosmeticCategory.CAPE) { cosmetic ->
                    // Get url of cape from cape service
                    val name = getCapeName(cosmetic) ?: return@fetchCosmetic

                    // Check if the cape is cached
                    val cachedCapeId = cachedCapes[name]
                    if (cachedCapeId != null) {
                        logger.info("Successfully loaded cached cape for ${player.name}")
                        callback.accept(cachedCapeId)
                        return@fetchCosmetic
                    }

                    // Request cape texture
                    val nativeImageBackedTexture = runCatching {
                        runBlocking {
                            CapeApi.getCape(name)
                        }
                    }.getOrNull() ?: return@fetchCosmetic

                    logger.info("Successfully loaded cape for ${player.name}")

                    val id = LiquidBounce.identifier("cape-$name")

                    mc.execute {
                        // Register cape texture
                        nativeImageBackedTexture.registerTexture(id)

                        // Cache cape texture
                        cachedCapes[name] = id

                        // Return cape texture
                        callback.accept(id)
                    }
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
        cachedCapes.values.forEach { mc.textureManager.release(it) }
        cachedCapes.clear()
    }

}
