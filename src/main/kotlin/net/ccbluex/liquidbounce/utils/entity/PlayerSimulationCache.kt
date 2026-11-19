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

package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.agent.simulation.Forecast
import net.ccbluex.agent.simulation.Forecasts
import net.ccbluex.agent.simulation.PositionExtrapolation
import net.ccbluex.agent.simulation.SimulatedInput
import net.ccbluex.agent.simulation.SimulatedPlayer
import net.ccbluex.agent.v26_3.toMinecraft
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerMovementTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/** The [Forecast]s of this tick: the local player's with its current keys, and those of the other players. */
object PlayerSimulationCache: EventListener {

    /** The other players' forecasts, cleared every tick. */
    val otherPlayers = Forecasts()

    private var localPlayerCache: Forecast? = null

    /**
     * The local player is between the start of its movement tick and the end of its tick.
     * A simulation made now has to finish this tick instead of running a whole one.
     */
    var isLocalPlayerMoving = false
        private set

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        this.otherPlayers.clear()
        this.isLocalPlayerMoving = false
    }

    @Suppress("unused")
    private val movementTickHandler = handler<PlayerMovementTickEvent>(priority = FIRST_PRIORITY) {
        this.isLocalPlayerMoving = true
    }

    @Suppress("unused")
    private val postTickHandler = handler<PlayerPostTickEvent>(priority = FIRST_PRIORITY) {
        this.isLocalPlayerMoving = false
    }

    @Suppress("unused")
    private val criticalMovementHandler = handler<MovementInputEvent>(
        priority = CRITICAL_MODIFICATION
    ) { event ->
        this.localPlayerCache = null
        updatePlayerCache(event.directionalInput)
    }

    @Suppress("unused")
    private val movementHandler = handler<MovementInputEvent> { event ->
        updatePlayerCache(event.directionalInput, verify = true)
    }

    @Suppress("unused")
    private val modalMovementHandler = handler<MovementInputEvent>(
        priority = MODEL_STATE
    ) { event ->
        updatePlayerCache(event.directionalInput, verify = true)
    }

    /**
     * Updates the cache for the local player,
     * this will be called on every movement input event
     * to ensure the cache is up to date.
     *
     * @param directionalInput the input to update the cache with
     */
    private fun updatePlayerCache(directionalInput: DirectionalInput, verify: Boolean = false) {
        // Check if we even need to update the cache
        if (verify && localPlayerCache?.input?.keys?.toMinecraft()?.let(::DirectionalInput) == directionalInput) {
            return
        }

        localPlayerCache = localForecast(directionalInput)
    }

    fun getSimulationForOtherPlayers(player: Player): Forecast = otherPlayers.of(player)

    /** Where [target] will be: by its forecast for players, else in a straight line. */
    fun extrapolate(target: Entity): PositionExtrapolation = PositionExtrapolation.of(target, otherPlayers)

    fun getSimulationForLocalPlayer(): Forecast =
        localPlayerCache ?: localForecast(DirectionalInput(player.input)).also { localPlayerCache = it }

    private fun localForecast(directionalInput: DirectionalInput) =
        Forecast(SimulatedPlayer.fromClientPlayer(SimulatedInput.fromClientPlayer(directionalInput)))
}
