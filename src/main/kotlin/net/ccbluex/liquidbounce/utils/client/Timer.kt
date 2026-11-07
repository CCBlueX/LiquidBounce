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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import kotlin.math.min

/**
 * Global minecraft timer.
 *
 * Also keeps the timer balance of the session: how many ticks the client is behind the time the server saw pass.
 * Held back player ticks and a timer below 1 build it up, extra ticks and a timer above 1 spend it,
 * no matter which module caused them.
 */
@AddonApi
object Timer : EventListener, ValueGroup("Timer") {

    val maxBalance by int("MaxBalance", 20, 0..200, "ticks")
    private val resetBalanceOnFlag by boolean("ResetBalanceOnFlag", true)

    private val requestHandler = RequestHandler<Float>()
    private val holds = RequestHandler<Unit>()

    /**
     * You cannot set this manually. Use [requestTimerSpeed] instead.
     */
    val timerSpeed: Float
        get() = requestHandler.getActiveRequestValue() ?: 1.0f

    /**
     * Ticks the client is behind. Positive balance can be spent on acceleration, negative balance is owed.
     */
    var balance = 0f
        private set

    /**
     * The speed the ticks of the current frame run at.
     */
    private var frameSpeed = 1f
    private var warping = false

    /**
     * Scales the ticks a frame advances by the requested speed.
     */
    fun scaleFrame(deltaTicks: Float): Float {
        val speed = timerSpeed
        frameSpeed = if (speed > 0f) speed else 1f
        return deltaTicks * frameSpeed
    }

    /**
     * Requests a timer speed change. If another module requests with a higher priority,
     * the other module is prioritized.
     */
    @JvmOverloads
    fun requestTimerSpeed(timerSpeed: Float, priority: Priority, provider: ClientModule, resetAfterTicks: Int = 1) {
        requestHandler.request(
            RequestHandler.Request(
                // this prevents requests from being instantly removed
                resetAfterTicks + 1,
                priority.priority,
                provider,
                timerSpeed
            )
        )
    }

    /**
     * Requests [timerSpeed] if the balance allows it. Faster than 1 needs balance to spend,
     * slower than 1 stops once the balance is full.
     *
     * @return whether the speed was requested
     */
    fun requestBalancedTimerSpeed(timerSpeed: Float, priority: Priority, provider: ClientModule): Boolean {
        val allowed = when {
            timerSpeed > 1f -> balance > 0f
            timerSpeed < 1f -> balance < maxBalance
            else -> true
        }

        if (allowed) {
            requestTimerSpeed(timerSpeed, priority, provider)
        }
        return allowed
    }

    /**
     * Holds back the player ticks of the next [ticks] ticks, as long as [provider] is running.
     */
    fun hold(provider: EventListener, ticks: Int = 1) {
        holds.request(RequestHandler.Request(ticks, 0, provider, Unit))
    }

    /**
     * Runs up to [ticks] extra ticks right away, as many as the balance covers.
     *
     * @return the ticks that ran
     */
    fun warp(ticks: Int, tick: () -> Unit = { player.tick() }): Int {
        if (mc.player == null) {
            return 0
        }

        val count = min(ticks, balance.toInt())
        if (count <= 0) {
            return 0
        }

        warping = true
        try {
            repeat(count) { tick() }
        } finally {
            warping = false
        }
        debugParameter("Warped") { count }
        return count
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        requestHandler.tick()
    }

    @Suppress("unused")
    private val holdHandler = handler<PlayerTickEvent>(priority = FIRST_PRIORITY) { event ->
        if (warping) {
            return@handler
        }

        val holding = holds.getActiveRequestValue() != null
        if (holding) {
            event.cancelEvent()
        }
        holds.tick()
        debugParameter("Holding") { holding }
    }

    @Suppress("unused")
    private val balanceHandler = handler<PlayerTickEvent>(priority = READ_FINAL_STATE) { event ->
        // A tick of the frame took 1 / speed ticks of real time, while a warped one took none
        if (!warping) {
            balance += 1f / frameSpeed
        }

        if (!event.isCancelled) {
            balance -= 1f
        }

        balance = balance.coerceIn(-maxBalance.toFloat(), maxBalance.toFloat())
        debugParameter("Balance") { balance }
        debugParameter("Speed") { frameSpeed }
    }

    /**
     * Incoming packets arrive on the network thread.
     */
    @Suppress("unused")
    private val flagHandler = handler<PacketEvent>(priority = FIRST_PRIORITY) { event ->
        if (resetBalanceOnFlag && event.packet is ClientboundPlayerPositionPacket) {
            mc.execute { balance = 0f }
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        balance = 0f
        holds.clear()
    }

}
