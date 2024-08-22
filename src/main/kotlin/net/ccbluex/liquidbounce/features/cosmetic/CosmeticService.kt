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

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.kotlin.toMD5
import net.minecraft.util.Util
import java.util.*
import kotlin.concurrent.thread

/**
 * A more reliable, safer and stress reduced cosmetics service
 *
 * It will frequently update all carriers of cosmetics into a set with their MD5-hashed UUID.
 * This allows to only request cosmetics of a carrier when it is needed.
 *
 * We know this might cause sometimes users to not have their cosmetics
 * shown immediately when account switches, but we can reduce the stress
 * on the API and the connection of the user.
 */
object CosmeticService : Listenable, Configurable("Cosmetics") {

    private const val COSMETICS_API = "http://127.0.0.1:8090/api/v3/cosmetics"
    private const val CARRIERS_URL = "$COSMETICS_API/carriers"
    private const val SELF_URL = "$COSMETICS_API/self"

    private const val REFRESH_DELAY = 60000L // Every minute should update

    /**
     * Collection of all cape carriers on the API.
     * We start with an empty list, which will be updated by the refreshCapeCarriers
     * function frequently based on the REFRESH_DELAY.
     */
    internal var carriers = emptySet<String>()
    private var carriersCosmetics = hashMapOf<UUID, Set<Cosmetic>>()

    private val lastUpdate = Chronometer()
    private var task: Thread? = null

    /**
     * Refresh cosmetic carriers if needed from the API in a MD5-hashed UUID set
     * and then call out [done].
     * It will only refresh when the REFRESH_DELAY has passed or when [force] is true.
     */
    fun refreshCarriers(force: Boolean = false, done: () -> Unit) {
        // Check if there is not another task running which could conflict.
        if (task == null) {
            // Check if the required time in milliseconds has passed of the REFRESH_DELAY
            if (lastUpdate.hasElapsed(REFRESH_DELAY) || force) {
                task = thread(name = "UpdateCarriersTask") {
                    runCatching {
                        carriers = decode<Set<String>>(HttpClient.get(CARRIERS_URL))
                        task = null

                        // Reset timer and start once again
                        lastUpdate.reset()

                        // Call out done
                        mc.execute {
                            done()
                        }
                    }.onFailure {
                        logger.error("Failed to refresh cape carriers due to error.", it)
                    }
                }
            } else {
                // Call out done immediate because there is no refresh required at the moment
                done()
            }
        }
    }

    fun fetchCosmetic(uuid: UUID, category: CosmeticCategory, done: (Cosmetic) -> Unit = { }) {
        refreshCarriers {
            // todo: implement account cosmetics
    //        val clientCapeUser = clientCapeUser
    //
    //        if (uuid == mc.session.uuidOrNull && clientCapeUser != null) {
    //            // If the UUID is the same as the current user, we can use the clientCapeUser
    //            val capeName = clientCapeUser.capeName
    //            return capeName to String.format(CAPE_NAME_DL_BASE_URL, capeName)
    //        }

            if (uuid.toMD5() !in carriers) {
                return@refreshCarriers
            }

            // Check if we already have the cosmetic
            carriersCosmetics[uuid]?.let { cosmetics ->
                done(cosmetics.find { cosmetic -> cosmetic.category == category } ?: return@refreshCarriers)
                return@refreshCarriers
            }

            // Pre-allocate a set to prevent multiple requests
            carriersCosmetics[uuid] = emptySet()

            Util.getDownloadWorkerExecutor().execute {
                runCatching {
                    val cosmetics = decode<Set<Cosmetic>>(HttpClient.get("$COSMETICS_API/carrier/$uuid"))
                    carriersCosmetics[uuid] = cosmetics

                    done(cosmetics.find { cosmetic -> cosmetic.category == category } ?: return@runCatching)
                }.onFailure {
                    logger.error("Failed to get cosmetics of carrier $uuid", it)
                }
            }
        }
    }

    private fun getCosmetic(uuid: UUID, category: CosmeticCategory): Cosmetic? {
        fetchCosmetic(uuid, category)

        if (uuid.toMD5() !in carriers) {
            return null
        }

        return carriersCosmetics[uuid]?.find { cosmetic -> cosmetic.category == category }
    }

    fun hasCosmetic(uuid: UUID, category: CosmeticCategory) = getCosmetic(uuid, category) != null

}

