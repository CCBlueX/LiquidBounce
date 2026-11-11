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

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import net.ccbluex.fastutil.forEachInt
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TargetChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.client.clientState
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugParameter
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.utils.client.ServerObserver
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.currentClient
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.ping
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundSwingAnimationPacket
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundPunchPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.util.Util
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import kotlin.math.roundToInt

/**
 * A rotation manager
 */
object CombatManager : EventListener {

    // useful for something like autoSoup
    private var pauseCombat: Int by clientState { 0 }

    // useful for something like autopot
    private var pauseRotation: Int by clientState { 0 }

    // useful for autoblock
    private var pauseBlocking: Int by clientState { 0 }

    const val PAUSE_COMBAT = 40 // 40 ticks = 2 seconds
    var duringCombat: Int by clientState { 0 }

    /**
     * [Timer.realTicks] when the server last restarted our attack cooldown.
     */
    private var cooldownResetTicks by clientState { 0.0 }

    /**
     * Ticks the server has counted towards our attack cooldown by [ticks] ticks of real time from now. It counts real
     * time at [ServerObserver.tickSpeed], so held, burst and timer ticks of the client do not shift it. Rounded to
     * whole ticks like the server counts them, so a TPS measured a little under 20 does not hold the hit back a tick.
     */
    fun attackCooldownTicks(ticks: Int = 0) =
        ((Timer.realTicks - cooldownResetTicks + ticks) * ServerObserver.tickSpeed).roundToInt()

    /**
     * Like [Player.getAttackStrengthScale] on the server for an attack [ticks] ticks of real time from now, not
     * clamped.
     */
    fun attackCooldownProgress(ticks: Int = 0) =
        (attackCooldownTicks(ticks) + 0.5f) / player.currentItemAttackStrengthDelay

    /**
     * [Util.getMillis] when the server last restarted the attack cooldown of other players, by entity id.
     */
    private val cooldownResets by clientState { Int2LongOpenHashMap() }

    /**
     * Like [attackCooldownProgress] for [other], [ticks] ticks of real time from now. Null when we have not seen its
     * cooldown restart.
     */
    fun attackCooldownProgress(other: Player, ticks: Double = 0.0): Float? {
        if (!cooldownResets.containsKey(other.id)) {
            return null
        }

        // We saw the restart half a ping late
        val millis = Util.getMillis() - cooldownResets.get(other.id) + player.ping / 2.0
        val serverTicks = (millis / 50 + ticks) * ServerObserver.tickSpeed
        return ((serverTicks + 0.5) / other.currentItemAttackStrengthDelay).toFloat()
    }

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

        val player = currentClient.player ?: return@handler
        debugParameter("Client Cooldown Ticks") { player.attackStrengthTicker }
        debugParameter("Server Cooldown Ticks") { attackCooldownTicks() }
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
        when (val packet = event.packet) {
            // The server restarts it on every swing, attack and change of the held item
            is ServerboundAttackPacket, is ServerboundPunchPacket, is ServerboundSetCarriedItemPacket ->
                cooldownResetTicks = Timer.realTicks
            // and on a new player after respawning
            is ClientboundRespawnPacket -> mc.execute { cooldownResetTicks = Timer.realTicks }

            // The same goes for other players, whose swings and items we see
            is ClientboundSwingAnimationPacket -> {
                val millis = Util.getMillis()
                mc.execute { cooldownResets.put(packet.entityId, millis) }
            }
            // Runs before the packet is applied, so the entity still holds the old item
            is ClientboundSetEquipmentPacket -> {
                val millis = Util.getMillis()
                mc.execute {
                    val entity = currentClient.level?.getEntity(packet.entity) as? LivingEntity ?: return@execute
                    val item = packet.slots.firstOrNull { it.first == EquipmentSlot.MAINHAND }?.second
                        ?: return@execute
                    if (!ItemStack.isSameItem(entity.mainHandItem, item)) {
                        cooldownResets.put(packet.entity, millis)
                    }
                }
            }
            is ClientboundRemoveEntitiesPacket -> mc.execute {
                packet.entityIds.forEachInt { cooldownResets.remove(it) }
            }
        }
    }

    @Suppress("unused")
    private val cooldownWorldHandler = handler<WorldChangeEvent> {
        cooldownResetTicks = Timer.realTicks
        cooldownResets.clear()
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
