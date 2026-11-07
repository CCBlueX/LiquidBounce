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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import kotlin.math.min

/**
 * Global minecraft timer.
 *
 * Also keeps the timer balance of the session: how many ticks the client is behind the time the server saw pass.
 * Held back player ticks and a timer below 1 build it up, extra ticks and a timer above 1 spend it,
 * no matter which module caused them. Modules spend it within their own limits, see [balanceWithin].
 */
@AddonApi
object Timer : EventListener {

    private val requestHandler = RequestHandler<Float>()
    private val holds = RequestHandler<Unit>()

    /**
     * You cannot set this manually. Use [requestTimerSpeed] instead.
     */
    val timerSpeed: Float
        get() = requestHandler.getActiveRequestValue() ?: 1.0f

    /**
     * Ticks the client is behind the server, negative once it runs ahead.
     */
    var balance = 0f
        private set

    /**
     * The speed the ticks of the current frame run at.
     */
    private var frameSpeed = 1f

    /**
     * Extra ticks still to run, see [burst].
     */
    private var burstTicks = 0
    private var bursting = false

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
     * Clamps the balance into [range] before a module uses it. Balance saved up past the end is no use,
     * and the start is as far as the module may run ahead.
     */
    fun balanceWithin(range: IntRange): Float {
        balance = balance.coerceIn(range.first.toFloat(), range.last.toFloat())
        return balance
    }

    /**
     * Extra ticks the balance can pay for right now.
     */
    fun spendableTicks(range: IntRange) = (balanceWithin(range) - range.first).toInt()

    /**
     * Requests [timerSpeed] if the balance allows it. Faster than 1 needs balance above the start of [range],
     * slower than 1 stops once the balance reaches its end.
     *
     * @return whether the speed was requested
     */
    fun requestBalancedTimerSpeed(
        timerSpeed: Float,
        priority: Priority,
        provider: ClientModule,
        range: IntRange
    ): Boolean {
        val balance = balanceWithin(range)
        val allowed = when {
            timerSpeed > 1f -> balance > range.first
            timerSpeed < 1f -> balance < range.last
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
     * Runs up to [ticks] extra game ticks right after the current one, as many as the balance covers within
     * [range]. They run through the tick loop of the frame like the ticks of a faster timer, only all at once.
     *
     * @return the ticks that will run
     */
    fun burst(ticks: Int, range: IntRange): Int {
        if (mc.player == null) {
            return 0
        }

        val count = min(ticks, spendableTicks(range) - burstTicks)
        if (count <= 0) {
            return 0
        }

        burstTicks += count
        debugParameter("Burst") { count }
        return count
    }

    /**
     * Bound of the tick loop of a frame, checked before each tick: lets the [burst] ticks run once the [ticks]
     * the frame advanced by are done.
     *
     * @param index the tick the loop is about to run
     */
    fun frameTicks(ticks: Int, index: Int): Int {
        bursting = false
        if (index < ticks || burstTicks <= 0) {
            return ticks
        }

        burstTicks--
        bursting = true
        return index + 1
    }

    /**
     * Clears the balance after a setback. Safe to call from the network thread.
     */
    fun resetBalance() {
        mc.execute { balance = 0f }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        requestHandler.tick()
    }

    @Suppress("unused")
    private val holdHandler = handler<PlayerTickEvent>(priority = FIRST_PRIORITY) { event ->
        // Held ticks after a burst only start once the burst ran
        if (bursting || burstTicks > 0) {
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
        // A tick of the frame took 1 / speed ticks of real time, while a burst one took none
        if (!bursting) {
            balance += 1f / frameSpeed
        }

        if (!event.isCancelled) {
            balance -= 1f
        }

        debugParameter("Balance") { balance }
        debugParameter("Speed") { frameSpeed }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        balance = 0f
        burstTicks = 0
        holds.clear()
    }

}
