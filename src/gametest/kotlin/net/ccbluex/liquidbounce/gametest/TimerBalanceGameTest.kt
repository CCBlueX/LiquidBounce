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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleTimer
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import kotlin.math.abs

/**
 * The balance follows the ticks the server saw: held back ticks and a slow timer earn it, burst ticks and a fast
 * timer spend it, and a setback clears it for modules that ask for that. Spending stays within the module's range,
 * which may start below 0 to run ahead.
 */
class TimerBalanceGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val savedFreeze = context.fromClient { ConfigSystem.serializeValueGroup(ModuleFreeze) }
        val savedTimer = context.fromClient { ConfigSystem.serializeValueGroup(ModuleTimer) }
        val savedTickBase = context.fromClient { ConfigSystem.serializeValueGroup(tickBase) }

        try {
            context.worldBuilder().create().use { world ->
                context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TIMEOUT_TICKS)
                context.waitTicks(SETTLE_TICKS)
                check(balance(context) == 0f) { "Plain ticks moved the balance to ${balance(context)}" }

                assertHoldEarnsAndBurstSpends(context)
                assertRunsAheadToMinimum(context)
                assertTimerIsAccounted(context)
                assertSetbackClears(context, world)
            }
        } finally {
            context.onClient {
                ModuleFreeze.enabled = false
                ModuleTimer.enabled = false
                tickBase.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleFreeze, savedFreeze)
                ConfigSystem.deserializeValueGroup(ModuleTimer, savedTimer)
                ConfigSystem.deserializeValueGroup(tickBase, savedTickBase)
            }
        }
    }

    private fun assertHoldEarnsAndBurstSpends(context: ClientGameTestContext) {
        context.onClient { ModuleFreeze.enabled = true }
        val start = playerTicks(context)

        context.waitTicks(FROZEN_TICKS)
        check(playerTicks(context) == start) { "Freeze did not hold back the player ticks" }
        check(balance(context) == FROZEN_TICKS.toFloat()) {
            "$FROZEN_TICKS held ticks earned ${balance(context)} balance"
        }

        context.waitTicks(MAX_BALANCE)
        check(balance(context) == (FROZEN_TICKS + MAX_BALANCE).toFloat()) {
            "${FROZEN_TICKS + MAX_BALANCE} held ticks earned ${balance(context)} balance"
        }

        val burst = context.fromClient {
            ModuleFreeze.enabled = false
            Timer.burst(Int.MAX_VALUE, 0..MAX_BALANCE)
        }
        check(burst == MAX_BALANCE) { "Burst $burst ticks instead of the $MAX_BALANCE the range allows" }
        check(playerTicks(context) - start == MAX_BALANCE) {
            "${playerTicks(context) - start} ticks ran during the burst"
        }
        check(balance(context) == 0f) { "The burst left ${balance(context)} balance" }
    }

    private fun assertRunsAheadToMinimum(context: ClientGameTestContext) {
        val range = -AHEAD_TICKS..MAX_BALANCE
        val start = playerTicks(context)

        val burst = context.fromClient { Timer.burst(Int.MAX_VALUE, range) }
        check(burst == AHEAD_TICKS) { "Burst $burst ticks ahead instead of $AHEAD_TICKS" }
        check(playerTicks(context) - start == AHEAD_TICKS) { "${playerTicks(context) - start} ticks ran ahead" }
        check(balance(context) == -AHEAD_TICKS.toFloat()) { "Running ahead left ${balance(context)} balance" }
        check(!context.fromClient { Timer.requestBalancedTimerSpeed(2f, Priority.NORMAL, ModuleTimer, range) }) {
            "Accelerating past the start of the range was allowed"
        }
    }

    private fun assertTimerIsAccounted(context: ClientGameTestContext) {
        check(!context.fromClient { Timer.requestBalancedTimerSpeed(2f, Priority.NORMAL, ModuleTimer, RANGE) }) {
            "Accelerating without balance was allowed"
        }

        val speed = ModuleTimer.Classic.containedValues.single { it.name == "Speed" }

        // The tick after a change was planned at the previous speed
        context.onClient {
            speed.setByString("0.5")
            ModuleTimer.enabled = true
        }
        context.waitTicks(2)
        val slow = balance(context)
        context.waitTicks(TIMER_TICKS)
        val earned = balance(context) - slow
        check(abs(earned - TIMER_TICKS) < EPSILON) { "$TIMER_TICKS ticks at half speed earned $earned balance" }

        context.onClient { speed.setByString("2") }
        context.waitTicks(2)
        val fast = balance(context)
        context.waitTicks(TIMER_TICKS)
        val spent = fast - balance(context)
        check(abs(spent - TIMER_TICKS / 2f) < EPSILON) { "$TIMER_TICKS ticks at double speed spent $spent balance" }

        context.onClient { ModuleTimer.enabled = false }
    }

    private fun assertSetbackClears(context: ClientGameTestContext, world: TestSingleplayerContext) {
        context.onClient {
            ModuleFreeze.enabled = true
            tickBase.enabled = true
        }
        context.waitTicks(FROZEN_TICKS)
        check(balance(context) >= FROZEN_TICKS) { "Freeze earned only ${balance(context)} before the setback" }

        world.server.runCommand("tp @a ~ ~ ~")
        context.waitFor({ !ModuleFreeze.enabled }, SETTLE_TIMEOUT_TICKS)
        context.waitTick()
        check(balance(context) == 0f) { "The setback left ${balance(context)} balance" }
    }

    private val tickBase get() = checkNotNull(ModuleManager["TickBase"])

    private fun balance(context: ClientGameTestContext) = context.fromClient { Timer.balance }

    private fun playerTicks(context: ClientGameTestContext) = context.fromClient { PlayerTicks.count }

    /**
     * Counts the player ticks that ran. [net.minecraft.world.entity.Entity.tickCount] also counts held ones.
     */
    private object PlayerTicks : EventListener {
        var count = 0

        @Suppress("unused")
        private val tickHandler = handler<PlayerPostTickEvent> { count++ }
    }

    private companion object {
        const val SETTLE_TIMEOUT_TICKS = 200
        const val SETTLE_TICKS = 20
        const val FROZEN_TICKS = 10
        const val AHEAD_TICKS = 10
        const val MAX_BALANCE = 20
        val RANGE = 0..MAX_BALANCE
        const val TIMER_TICKS = 10
        const val EPSILON = 0.01f
    }

}
