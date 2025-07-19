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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.models.cosmetics.Cosmetic
import net.ccbluex.liquidbounce.api.models.cosmetics.CosmeticCategory
import net.ccbluex.liquidbounce.api.services.cosmetics.CosmeticApi
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.eventListenerScope
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.suspendHandler
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
    private var carriers = emptySet<String>()
    private val carriersCosmetics = hashMapOf<UUID, Set<Cosmetic>>()

    private val lastUpdate = Chronometer()
    private var task: Deferred<Result<Set<String>>>? = null

    /**
     * Refreshes cosmetic carriers if needed from the API in a MD5-hashed UUID.
     * It will only refresh when the REFRESH_DELAY has passed or when [force] is true.
     *
     * @return the refreshed carriers, or the exception
     */
    suspend fun refreshCarriers(force: Boolean = false): Result<Set<String>> {
        // If there is another running task, returns the shared value from it.
        task?.let {
            return it.await()
        }

        // Check if the required time in milliseconds has passed of the REFRESH_DELAY
        if (!lastUpdate.hasElapsed(REFRESH_DELAY) && !force) {
            // Returns immediately because there is no refresh required at the moment
            return Result.success(carriers)
        } else {
            val task = eventListenerScope.async(Dispatchers.IO) {
                runCatching {
                    carriers = CosmeticApi.getCarriers()

                    // Reset the timer and task, allow to start once again
                    lastUpdate.reset()
                    task = null

                    // Result
                    carriers
                }.onFailure {
                    logger.error("Failed to refresh cape carriers due to error.", it)
                }
            }

            this.task = task
            return task.await()
        }
    }

    /**
     * Fetches the cosmetic for the player with given [uuid] and [category].
     *
     * @return null if no [Cosmetic] found (yet)
     */
    suspend fun fetchCosmetic(uuid: UUID, category: CosmeticCategory): Cosmetic? {
        val clientAccount = ClientAccountManager.clientAccount

        // Check if the client account is available and the requested UUID is the same as the session UUID
        if ((uuid == mc.session.uuidOrNull || uuid == player.uuid) && clientAccount != ClientAccount.EMPTY_ACCOUNT) {
            clientAccount.cosmetics?.let { cosmetics ->
                return cosmetics.findWithCategory(category)
            }

            // Pre-allocate a set to prevent multiple requests
            clientAccount.cosmetics = emptySet()

            // Update cosmetics
            clientAccount.updateCosmetics()

            return clientAccount.cosmetics?.findWithCategory(category)
        }

        refreshCarriers().onSuccess { carriers ->
            if (uuid.toMD5() !in carriers) {
                return null
            }

            // Check if we already have the cosmetic
            carriersCosmetics[uuid]?.let { cosmetics ->
                return cosmetics.findWithCategory(category)
            }

            // Pre-allocate a set to prevent multiple requests
            carriersCosmetics[uuid] = emptySet()

            runCatching {
                val cosmetics = CosmeticApi.getCarrierCosmetics(uuid)
                carriersCosmetics[uuid] = cosmetics

                return cosmetics.findWithCategory(category)
            }.onFailure {
                logger.error("Failed to get cosmetics of carrier $uuid", it)
            }
        }

        return null
    }

    private fun getCosmetic(uuid: UUID, category: CosmeticCategory): Cosmetic? {
        eventListenerScope.launch(Dispatchers.IO) { fetchCosmetic(uuid, category) }

        // Check if the client account is available and the requested UUID is the same as the session UUID
        val clientAccount = ClientAccountManager.clientAccount

        if ((uuid == mc.session.uuidOrNull || uuid == player.uuid) && clientAccount != ClientAccount.EMPTY_ACCOUNT) {
            clientAccount.cosmetics?.let { cosmetics ->
                return cosmetics.findWithCategory(category)
            }
        }

        if (uuid.toMD5() !in carriers) {
            return null
        }

        return carriersCosmetics[uuid]?.findWithCategory(category)
    }

    fun hasCosmetic(uuid: UUID, category: CosmeticCategory) = getCosmetic(uuid, category) != null

    private suspend fun transferTemporaryOwnership(uuid: UUID) {
        val clientAccount = ClientAccountManager.clientAccount
        if (clientAccount == ClientAccount.EMPTY_ACCOUNT) {
            return
        }

        runCatching {
            clientAccount.transferTemporaryOwnership(uuid)
        }.onSuccess {
            logger.info("[Cosmetics] Transferred cape ownership to $uuid")

            // Refresh carriers after transfer
            refreshCarriers(force = true).onSuccess {
                logger.info("[Cosmetics] Successfully loaded ${it.size} cosmetics carriers.")
            }.onFailure {
                logger.error("[Cosmetics] Failed to loaded cosmetics carriers.", it)
            }
        }.onFailure {
            logger.error("[Cosmetics] Failed to transfer cosmetic ownership to $uuid", it)
        }
    }

    @Suppress("unused")
    private val sessionHandler = suspendHandler<SessionEvent> { event ->
        val session = event.session

        // Check if the account is valid
        if (session.accountType == Session.AccountType.LEGACY || session.accessToken.length < 2) {
            return@suspendHandler
        }
        val uuid = session.uuidOrNull ?: return@suspendHandler

        transferTemporaryOwnership(uuid)
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        clearCarriersCosmetics()
    }

    fun clearCarriersCosmetics() {
        carriersCosmetics.clear()
    }

    private fun Set<Cosmetic>.findWithCategory(category: CosmeticCategory) = find { it.category == category }

}

