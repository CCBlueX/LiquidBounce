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

import kotlinx.coroutines.Job
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.models.cosmetics.Cosmetic
import net.ccbluex.liquidbounce.api.models.cosmetics.CosmeticCategory
import net.ccbluex.liquidbounce.api.services.cosmetics.CosmeticApi
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.toMD5
import net.minecraft.client.session.Session
import java.util.*

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
object CosmeticService : EventListener, Configurable("Cosmetics") {

    private const val REFRESH_DELAY = 60000L // Every minute should update

    /**
     * Collection of all cape carriers on the API.
     * We start with an empty list, which will be updated by the refreshCapeCarriers
     * function frequently based on the REFRESH_DELAY.
     */
    internal var carriers = emptySet<String>()
    internal var carriersCosmetics = hashMapOf<UUID, Set<Cosmetic>>()

    private val lastUpdate = Chronometer()
    private var task: Job? = null

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
                task = withScope {
                    runCatching {
                        carriers = CosmeticApi.getCarriers()
                        task = null

                        // Reset timer and start once again
                        lastUpdate.reset()

                        // Call out done
                        mc.execute(done)
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
        val clientAccount = ClientAccountManager.clientAccount

        // Check if the client account is available and the requested UUID is the same as the session UUID
        if ((uuid == mc.session.uuidOrNull || uuid == player.uuid) && clientAccount != ClientAccount.EMPTY_ACCOUNT) {
            clientAccount.cosmetics?.let { cosmetics ->
                done(cosmetics.find { cosmetic -> cosmetic.category == category } ?: return)
                return
            }

            // Pre-allocate a set to prevent multiple requests
            clientAccount.cosmetics = emptySet()

            // Update cosmetics
            withScope {
                clientAccount.updateCosmetics()

                clientAccount.cosmetics?.let { cosmetics ->
                    done(cosmetics.find { cosmetic -> cosmetic.category == category } ?: return@withScope)
                }
            }
            return
        }

        refreshCarriers {
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

            withScope {
                runCatching {
                    val cosmetics = CosmeticApi.getCarrierCosmetics(uuid)
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

        // Check if the client account is available and the requested UUID is the same as the session UUID
        val clientAccount = ClientAccountManager.clientAccount

        if ((uuid == mc.session.uuidOrNull || uuid == player.uuid) && clientAccount != ClientAccount.EMPTY_ACCOUNT) {
            clientAccount.cosmetics?.let { cosmetics ->
                return cosmetics.find { cosmetic -> cosmetic.category == category }
            }
        }

        if (uuid.toMD5() !in carriers) {
            return null
        }

        return carriersCosmetics[uuid]?.find { cosmetic -> cosmetic.category == category }
    }

    fun hasCosmetic(uuid: UUID, category: CosmeticCategory) = getCosmetic(uuid, category) != null

    private fun transferTemporaryOwnership(uuid: UUID) {
        val clientAccount = ClientAccountManager.clientAccount
        if (clientAccount == ClientAccount.EMPTY_ACCOUNT) {
            return
        }

        withScope {
            runCatching {
                clientAccount.transferTemporaryOwnership(uuid)
            }.onSuccess {
                logger.info("[Cosmetics] Transferred cape ownership to $uuid")

                // Refresh carriers after transfer
                refreshCarriers(true) {
                    logger.info("[Cosmetics] Successfully loaded ${carriers.size} cosmetics carriers.")
                }
            }.onFailure {
                logger.error("[Cosmetics] Failed to transfer cosmetic ownership to $uuid", it)
            }
        }
    }

    @Suppress("unused")
    private val sessionHandler = handler<SessionEvent> { event ->
        val session = event.session

        // Check if the account is valid
        if (session.accountType == Session.AccountType.LEGACY || session.accessToken.length < 2) {
            return@handler
        }
        val uuid = session.uuidOrNull ?: return@handler

        transferTemporaryOwnership(uuid)
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        carriersCosmetics.clear()
    }

}

