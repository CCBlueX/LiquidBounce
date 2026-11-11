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
package net.ccbluex.liquidbounce.features.client

import com.google.common.collect.MapMaker
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.account.SessionBundle
import net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MinecraftAccessor
import net.ccbluex.liquidbounce.utils.client.NullableBypass
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.User
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer
import java.util.concurrent.ConcurrentMap
import kotlin.math.min

/**
 * One account with its own connection, world and module states.
 *
 * Exactly one client is [ClientManager.active]: it has the window, the input and the sound.
 * The others keep playing in the background.
 */
class Client internal constructor(val id: Int) {

    val isMain: Boolean
        get() = id == MAIN_ID

    val isActive: Boolean
        get() = this === ClientManager.active

    /**
     * The account this client plays as, null for the one the game was started with.
     */
    @Volatile
    var account: SessionBundle? = null

    val user: User
        get() = account?.session ?: (mc as MinecraftAccessor).launchUser

    val player: LocalPlayer?
        get() = if (ClientContext.holds(this)) NullableBypass.mc()?.player else game.player

    val level: ClientLevel?
        get() = if (ClientContext.holds(this)) NullableBypass.mc()?.level else game.level

    val gameMode: MultiPlayerGameMode?
        get() = if (ClientContext.holds(this)) NullableBypass.mc()?.gameMode else game.gameMode

    val screen: Screen?
        get() = if (ClientContext.holds(this)) NullableBypass.mc()?.gui?.screen() else game.screen

    val connection: ClientPacketListener?
        get() = player?.connection

    val inGame: Boolean
        get() = player != null && level != null

    internal val game = GameState()

    /**
     * See [ClientState]. Keyed weakly, so that state of something short-lived goes with it.
     */
    internal val states: ConcurrentMap<ClientState<*>, Any> = MapMaker().weakKeys().makeMap()

    /**
     * Module settings this client keeps for itself. Any other setting is the one of the main client.
     */
    internal val values: ConcurrentMap<Value<*>, Any> = MapMaker().weakKeys().makeMap()
    internal val activeModes: ConcurrentMap<ModeValueGroup<*>, Mode> = MapMaker().weakKeys().makeMap()

    private var lastTickAt = System.nanoTime()
    private var partialTicks = 0f

    /**
     * Ticks that became due since the last call. Background clients keep their own time, so a timer
     * running in the active client does not speed them up.
     */
    internal fun dueTicks(): Int {
        val now = System.nanoTime()
        val tickRate = level?.tickRateManager()?.takeIf { it.runsNormally() }
        val millisPerTick = tickRate?.millisecondsPerTick()?.coerceAtLeast(1f) ?: DEFAULT_MILLIS_PER_TICK

        partialTicks += (now - lastTickAt) / NANOS_PER_MILLI / millisPerTick
        lastTickAt = now

        val ticks = partialTicks.toInt()
        partialTicks -= ticks
        return min(ticks, MAX_TICKS_PER_FRAME)
    }

    internal fun restartClock() {
        lastTickAt = System.nanoTime()
        partialTicks = 0f
    }

    override fun toString() = "Client(id=$id, user=${user.name})"

    companion object {
        const val MAIN_ID = 0

        private const val DEFAULT_MILLIS_PER_TICK = 50f
        private const val NANOS_PER_MILLI = 1_000_000f
        private const val MAX_TICKS_PER_FRAME = 10
    }

}
