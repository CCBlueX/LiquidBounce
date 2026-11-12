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
import net.ccbluex.liquidbounce.features.client.clientState
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugParameter
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import kotlin.math.min

/**
 * Global minecraft timer.
 *
 * Also keeps the timer balance of the session: how many ticks the client is behind the time the server saw pass.
 * Held back ticks and a timer below 1 build it up, extra ticks and a timer above 1 spend it,
 * no matter which module caused them. Modules spend it within their own limits, see [balanceWithin].
 */
@AddonApi
object Timer : EventListener {

    private val requestHandler by clientState { RequestHandler<Float>() }
    private val holds by clientState { RequestHandler<Unit>() }

    /**
     * You cannot set this manually. Use [requestTimerSpeed] instead.
     */
    val timerSpeed: Float
        get() = requestHandler.getActiveRequestValue() ?: 1.0f

    /**
     * Ticks the client is behind the server, negative once it runs ahead.
     */
    var balance by clientState { 0f }
        private set

    /**
     * The speed the ticks of the current frame run at.
     */
    private var frameSpeed by clientState { 1f }

    /**
     * Extra ticks still to run, see [burst].
     */
    private var burstTicks by clientState { 0 }

    /**
     * Whether the current tick is an extra one of a [burst].
     */
    var bursting by clientState { false }
        private set

    /**
     * Ticks held back since the last tick that ran.
     */
    private var heldTicks by clientState { 0 }

    /**
     * Ticks of real time, counted like [balance] but never clamped or reset: held ticks count, burst ticks do not,
     * and a tick at a faster timer counts as less than one. Only differences between two readings mean anything.
     */
    var realTicks by clientState { 0.0 }
        private set

    /**
     * Ticks held back before each of the last ticks that ran, a ring ending before [heldHistoryEnd].
     */
    private val heldHistory by clientState { IntArray(20) }
    private var heldHistoryEnd by clientState { 0 }

    /**
     * Whether the last tick was held back. Its interpolated values are then still those of the tick before,
     * so the game is drawn as it is instead of replaying that tick every frame.
     */
    val isHeld
        get() = heldTicks > 0

    /**
     * Ticks the tick loop of the current frame runs, see [frameTicks].
     */
    private var frameEnd by clientState { 0 }

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
     * Holds back the next [ticks] game ticks, as long as [provider] is running. Nothing ticks while held, and no
     * tick end goes out either, so the server sees lag.
     */
    fun hold(provider: EventListener, ticks: Int = 1) {
        holds.request(RequestHandler.Request(ticks, 0, provider, Unit))
    }

    /**
     * Ticks held back since [ticks] ticks ago, at most 20. Other entities do not tick while held,
     * so what counts down every tick, such as their hurt time, falls behind the server by that many ticks.
     */
    fun heldTicksWithin(ticks: Int): Int {
        var held = heldTicks
        for (back in 1..min(ticks, heldHistory.size)) {
            held += heldHistory[(heldHistoryEnd - back).mod(heldHistory.size)]
        }
        return held
    }

    /**
     * Runs up to [ticks] extra game ticks right after the current one, as many as the balance covers within
     * [range]. They run through the tick loop of the frame like the ticks of a faster timer, only all at once.
     *
     * @return the ticks that will run
     */
    fun burst(ticks: Int, range: IntRange): Int {
        if (currentClient.player == null) {
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
     * Bound of the tick loop of a frame, checked before each tick. Of the [ticks] the frame advanced by, the ones
     * a [hold] covers do not run, and the [burst] ticks run in front of the others once no hold is left. All of the
     * [ticks] count in [realTicks].
     *
     * @param index the tick the loop is about to run
     */
    fun frameTicks(ticks: Int, index: Int): Int {
        if (index == 0) {
            frameEnd = ticks
            realTicks += ticks / frameSpeed.toDouble()
        }

        holdTicks(index)

        bursting = burstTicks > 0 && holds.getActiveRequestValue() == null
        if (bursting) {
            burstTicks--
            frameEnd++
        }

        if (index < frameEnd) {
            heldHistory[heldHistoryEnd] = heldTicks
            heldHistoryEnd = (heldHistoryEnd + 1) % heldHistory.size
            heldTicks = 0
        }
        return frameEnd
    }

    /**
     * Drops the ticks left in the frame a hold covers. Their time passed without running them.
     */
    private fun holdTicks(index: Int) {
        var held = 0
        while (index < frameEnd && holds.getActiveRequestValue() != null) {
            holds.tick()
            frameEnd--
            held++
        }

        if (held > 0) {
            balance += held / frameSpeed
            heldTicks += held
        }
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
        heldTicks = 0
        heldHistory.fill(0)
        holds.clear()
    }

}
