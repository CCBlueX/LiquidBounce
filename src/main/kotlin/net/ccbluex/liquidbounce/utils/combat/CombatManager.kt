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
package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TargetChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.utils.client.ServerObserver
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundPunchPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import kotlin.math.roundToInt

/**
 * A rotation manager
 */
object CombatManager : EventListener {

    // useful for something like autoSoup
    private var pauseCombat: Int = 0

    // useful for something like autopot
    private var pauseRotation: Int = 0

    // useful for autoblock
    private var pauseBlocking: Int = 0

    const val PAUSE_COMBAT = 40 // 40 ticks = 2 seconds
    var duringCombat: Int = 0

    /**
     * [Timer.realTicks] when the server last restarted our attack cooldown.
     */
    private var cooldownResetTicks = 0.0

    /**
     * Server ticks per tick of real time: the TPS [ServerObserver] measured against the tick rate the client runs at.
     * 1 until there is a measurement.
     */
    val serverTickRate: Double
        get() {
            val tps = ServerObserver.tps
            val level = mc.level
            return if (tps.isNaN() || level == null) 1.0 else tps / level.tickRateManager().tickrate()
        }

    /**
     * Ticks the server has counted towards our attack cooldown by [ticks] ticks of real time from now. It counts real
     * time at [serverTickRate], so held, burst and timer ticks of the client do not shift it. Rounded to whole ticks
     * like the server counts them, so a TPS measured a little under 20 does not hold the hit back a tick.
     */
    fun attackCooldownTicks(ticks: Int = 0) =
        ((Timer.realTicks - cooldownResetTicks + ticks) * serverTickRate).roundToInt()

    /**
     * Like [Player.getAttackStrengthScale] on the server for an attack [ticks] ticks of real time from now, not
     * clamped.
     */
    fun attackCooldownProgress(ticks: Int = 0) =
        (attackCooldownTicks(ticks) + 0.5f) / player.currentItemAttackStrengthDelay

    private fun updatePauseRotation() {
        if (pauseRotation <= 0) return

        pauseRotation--
    }

    private fun updatePauseCombat() {
        if (pauseCombat <= 0) return

        pauseCombat--
    }

    private fun updatePauseBlocking() {
        if (pauseBlocking <= 0) return

        pauseBlocking--
    }

    private fun updateDuringCombat() {
        if (duringCombat <= 0) return

        duringCombat--
    }

    /**
     * Update current rotation to new rotation step
     */
    fun update() {
        updatePauseRotation()
        updatePauseCombat()
        // TODO: implement this for killaura autoblock and other
        updatePauseBlocking()
        updateDuringCombat()
    }

    val tickHandler = handler<GameTickEvent> {
        update()

        val player = mc.player ?: return@handler
        debugParameter("Client Cooldown Ticks") { player.attackStrengthTicker }
        debugParameter("Server Cooldown Ticks") { attackCooldownTicks() }
        debugParameter("Server Tick Rate") { serverTickRate }
    }

    @Suppress("unused")
    val attackHandler = handler<AttackEntityEvent> { event ->
        val entity = event.entity

        if (entity is LivingEntity && entity.shouldBeAttacked()) {
            duringCombat = PAUSE_COMBAT

            if (entity is Player) {
                EventManager.callEvent(TargetChangeEvent(PlayerData.fromPlayer(entity)))
            }
        }
    }

    @Suppress("unused")
    private val cooldownPacketHandler = handler<PacketEvent> { event ->
        when (event.packet) {
            // The server restarts it on every swing, attack and change of the held item
            is ServerboundAttackPacket, is ServerboundPunchPacket, is ServerboundSetCarriedItemPacket ->
                cooldownResetTicks = Timer.realTicks
            // and on a new player after respawning
            is ClientboundRespawnPacket -> mc.execute { cooldownResetTicks = Timer.realTicks }
        }
    }

    @Suppress("unused")
    private val cooldownWorldHandler = handler<WorldChangeEvent> {
        cooldownResetTicks = Timer.realTicks
    }

    val shouldPauseCombat: Boolean
        get() = pauseCombat > 0
    val shouldPauseRotation: Boolean
        get() = pauseRotation > 0
    val shouldPauseBlocking: Boolean
        get() = pauseBlocking > 0
    @AddonApi
    val isInCombat: Boolean
        get() = this.duringCombat > 0 ||
            (ModuleKillAura.running && ModuleKillAura.targetTracker.target != null)

    @AddonApi
    fun pauseCombatForAtLeast(pauseTime: Int) {
        pauseCombat = pauseCombat.coerceAtLeast(pauseTime)
    }

    @AddonApi
    fun pauseRotationForAtLeast(pauseTime: Int) {
        pauseRotation = pauseRotation.coerceAtLeast(pauseTime)
    }

    fun pauseBlockingForAtLeast(pauseTime: Int) {
        pauseBlocking = pauseBlocking.coerceAtLeast(pauseTime)
    }

}
