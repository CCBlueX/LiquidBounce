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
 */
package net.ccbluex.liquidbounce.features.cosmetic

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import java.io.InputStream
import java.net.URI

/**
 * A cape cosmetic manager
 */
object CapeCosmeticsManager {

    private const val CAPES_API = "http://capes.liquidbounce.net/api/v1/cape"
    private const val CAPE_NAME_DL_BASE_URL = "$CAPES_API/name/%s"

    /**
     * Cached capes
     *
     * This is OK because the cape texture is only loaded for players that own a cape.
     * This is very rare for most people, and therefore the cache is not that big.
     * We also don't need to worry about memory leaks
     * because the cache is cleared when the player disconnects from the world.
     */
    private var cachedCapes = mutableMapOf<String, Identifier>()

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
        Util.getMainWorkerExecutor().execute {
            runCatching {
                val uuid = player.id

                CosmeticService.fetchCosmetic(uuid, CosmeticCategory.CAPE) { cosmetic ->
                    // Get url of cape from cape service
                    val (name, url) = getCapeDownload(cosmetic) ?: return@fetchCosmetic

                    // Check if the cape is cached
                    if (cachedCapes.containsKey(name)) {
                        LiquidBounce.logger.info("Successfully loaded cached cape for ${player.name}")
                        response.response(cachedCapes[name]!!)
                        return@fetchCosmetic
                    }

                    // Request cape texture
                    val nativeImageBackedTexture = requestCape(url)
                        ?: return@fetchCosmetic

                    LiquidBounce.logger.info("Successfully loaded cape for ${player.name}")

                    // Register cape texture
                    val capeTexture = mc.textureManager.registerDynamicTexture("liquidbounce-$name",
                            nativeImageBackedTexture)

                    // Cache cape texture
                    cachedCapes[name] = capeTexture

                    // Return cape texture
                    response.response(capeTexture)
                }
            }
        }
    }

    /**
     * Requests a cape from a [url]
     */
    private fun requestCape(url: String) = runCatching {
        val capeURL = URI(url).toURL()

        // Request cape from URL which should be our API. (https://api.liquidbounce.net/api/v1/cape/uuid/%s)
        val connection = capeURL.openConnection()
        connection.addRequestProperty(
            "User-Agent",
            "${LiquidBounce.CLIENT_NAME}_${LiquidBounce.clientVersion}_${mc.gameVersion}"
        )
        connection.readTimeout = 5000
        connection.connectTimeout = 2500
        connection.connect()

        readCapeFromStream(connection.getInputStream())
    }.getOrNull()

    /**
     * Reads a cape from an [InputStream]
     */
    private fun readCapeFromStream(stream: InputStream) = stream.runCatching {
        NativeImageBackedTexture(NativeImage.read(stream))
    }.getOrNull()

    private fun getCapeDownload(cosmetic: Cosmetic): Pair<String, String>? {
        // Check if cosmetic is a cape
        if (cosmetic.category != CosmeticCategory.CAPE) return null

        // Extra should not be null if the cape is present
        val name = cosmetic.extra
            // format: cape:name
            // todo: use serialization
            ?.split(":")
            ?.getOrNull(1) ?: return null

        return name to String.format(CAPE_NAME_DL_BASE_URL, name)
    }

}
