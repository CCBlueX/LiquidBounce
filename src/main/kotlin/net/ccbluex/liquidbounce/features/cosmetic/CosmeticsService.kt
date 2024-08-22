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
import net.ccbluex.liquidbounce.features.cosmetic.CapeService.CAPE_API
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.minecraft.util.Util
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.codec.digest.Md5Crypt
import java.util.*
import kotlin.concurrent.thread

/**
 * A more reliable and stress reduced cape service
 *
 * It will frequently update all carriers of capes into a map with the described cape name.
 * This allows to cache already known capes and store them locally and will more quickly load them.
 *
 * We know this might cause sometimes users to not have their capes
 * shown immediately when account switches, but we can reduce the stress
 * on the API and the connection of the user.
 */
object CosmeticsService : Listenable, Configurable("Cosmetics") {

//    var clientCapeUser: Cosmetics? = null

    /**
     * I would prefer to use CLIENT_API but due to Cloudflare causing issues with SSL and their browser integrity check,
     * we have a separate domain.
     */
    private const val COSMETICS_API = "http://capes.liquidbounce.net/api/v3/cosmetics"

    /**
     * The API URL to get all cape carriers.
     * Format: [["8f617b6abea04af58e4bd026d8fa9de8", "marco"], ...]
     */
    private const val CARRIERS_URL = "$COSMETICS_API/carriers"
    const val SELF_CAPE_URL = "$COSMETICS_API/self"
    private const val CAPE_NAME_DL_BASE_URL = "$CAPE_API/name/%s"

    private const val REFRESH_DELAY = 60000L // Every minute should update

    /**
     * Collection of all cape carriers on the API.
     * We start with an empty list, which will be updated by the refreshCapeCarriers
     * function frequently based on the REFRESH_DELAY.
     */
    internal var carriers = emptyList<String>()
    internal var metCarriers = hashMapOf<String, Set<Cosmetic>>()

    private val lastUpdate = Chronometer()
    private var task: Thread? = null

    /**
     * Refresh cape carriers, capture from the API.
     * It will take a list of (uuid, cape_name) tuples.
     */
    fun refreshCarriers(force: Boolean = false, done: () -> Unit) {
        // Check if there is not another task running which could conflict.
        if (task == null) {
            // Check if the required time in milliseconds has passed of the REFRESH_DELAY
            if (lastUpdate.hasElapsed(REFRESH_DELAY) || force) {
                task = thread(name = "UpdateCarriersTask") {
                    runCatching {
                        carriers = decode<List<String>>(HttpClient.get(CARRIERS_URL))
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

    fun hasCosmetic(uuid: UUID, category: CosmeticCategory): Boolean {
        val md5 = DigestUtils.md5Hex(uuid.toString())
        val carrier = carriers.find { md5Hex ->
            // Check if the UUID matches the MD5 hex of one of the carriers
            // TODO: Does the md5 hex match md5Crypt?
            md5Hex == md5
        } ?: return false

        // Check if we already met the cape carrier, if not we will run an async task to ask
        // http://127.0.0.1:8090/api/v3/cosmetics/carrier/85ac9d5ec3204e94933b3b0b8f6c512b
        // for the cape carrier's cosmetics
        if (carrier !in metCarriers) {
            metCarriers[carrier] = emptySet()

            Util.getDownloadWorkerExecutor().execute {
                runCatching {
                    val cosmetics = decode<List<Cosmetic>>(HttpClient.get("$COSMETICS_API/carrier/$carrier"))
                    metCarriers[carrier] = cosmetics.toSet()
                }.onFailure {
                    logger.error("Failed to get cosmetics of carrier $carrier", it)
                }
            }
        }

        // Check if the cape carrier has the cosmetic
        return metCarriers[carrier]?.any { cosmetic -> cosmetic.category == category } ?: false
    }

    /**
     * Get the download url to cape of UUID
     */
//    fun getCapeDownload(uuid: UUID): Pair<String, String>? {
//        val clientCapeUser = clientCapeUser
//
//        if (uuid == mc.session.uuidOrNull && clientCapeUser != null) {
//            // If the UUID is the same as the current user, we can use the clientCapeUser
//            val capeName = clientCapeUser.capeName
//            return capeName to String.format(CAPE_NAME_DL_BASE_URL, capeName)
//        }

        // Lookup cape carrier by UUID, if UUID is matching
//        val capeCarrier = carriers.find { it.uuid == uuid } ?: return null
//
//        return capeCarrier.capeName to String.format(CAPE_NAME_DL_BASE_URL, capeCarrier.capeName)
//    }

}

data class Cosmetic(val category: CosmeticCategory, val extra: String? = null)

enum class CosmeticCategory {
    CAPE,
    DEADMAU5_EARS,
    DINNERBONE,
}
