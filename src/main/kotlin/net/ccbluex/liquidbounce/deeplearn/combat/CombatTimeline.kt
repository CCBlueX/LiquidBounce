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
@file:OptIn(ExperimentalUnsignedTypes::class)

package net.ccbluex.liquidbounce.deeplearn.combat

import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import net.ccbluex.liquidbounce.utils.entity.hasCooldown
import net.minecraft.client.player.LocalPlayer

@UnstableAddonApi
enum class CombatSource { FIRST_PERSON, OBSERVED, OBSERVED_VS_LOCAL }

/** Stored by ordinal in timelines; [id] names the model variant. */
@UnstableAddonApi
enum class CombatStyle(val id: String) {
    LEGACY("legacy"),
    COOLDOWN("cooldown");

    companion object {
        fun of(player: LocalPlayer) = if (player.hasCooldown) COOLDOWN else LEGACY
    }
}

/**
 * Raw per-tick state of one fighter. Positions are relative to the timeline origin.
 */
@Suppress("TooManyFunctions")
@UnstableAddonApi
class CombatTrack(val ticks: Int) {
    // Columns are stored in declaration order
    private val ints = ArrayList<IntArray>()
    private val shorts = ArrayList<ShortArray>()
    private val bytes = ArrayList<ByteArray>()
    val intColumns: List<IntArray> get() = ints
    val shortColumns: List<ShortArray> get() = shorts
    val byteColumns: List<ByteArray> get() = bytes

    val x = ints()
    val y = ints()
    val z = ints()
    val yaw = ints()
    val pitch = ints()
    val state = shorts()
    val events = shorts()
    val probes = shorts()
    val hurtTime = bytes()
    val health = ubytes()
    val attackDelay = ubytes()
    val attackStrength = ubytes()
    val item = bytes()
    val width = ubytes()
    val height = ubytes()
    val eyeHeight = ubytes()
    val input = bytes()
    val clicks = bytes()
    val nearestOther = ubytes()

    private fun ints() = IntArray(ticks).also(ints::add)
    private fun shorts() = ShortArray(ticks).also(shorts::add)
    private fun bytes() = ByteArray(ticks).also(bytes::add)
    private fun ubytes() = UByteArray(ticks).also { bytes += it.asByteArray() }

    /** Whether [State] [flag] is set. */
    fun has(tick: Int, flag: Int) = state[tick].toInt() and flag != 0

    /** Whether [Event] [flag] is set. */
    fun hasEvent(tick: Int, flag: Int) = events[tick].toInt() and flag != 0

    fun positionX(tick: Int) = x[tick] / POSITION_UNITS
    fun positionY(tick: Int) = y[tick] / POSITION_UNITS
    fun positionZ(tick: Int) = z[tick] / POSITION_UNITS
    fun yawDegrees(tick: Int) = yaw[tick] / ANGLE_UNITS
    fun pitchDegrees(tick: Int) = pitch[tick] / ANGLE_UNITS
    fun widthBlocks(tick: Int) = width[tick].toInt() / SIZE_UNITS
    fun heightBlocks(tick: Int) = height[tick].toInt() / SIZE_UNITS
    fun eyeHeightBlocks(tick: Int) = eyeHeight[tick].toInt() / SIZE_UNITS

    /** Health relative to maximum health, or `null` when the server hides it. */
    fun healthRatio(tick: Int) = health[tick].toInt().takeIf { it != UNKNOWN }?.let { it / 254f }

    /** Ticks between full-strength attacks, or `null` if unknown. */
    fun attackDelayTicks(tick: Int) = attackDelay[tick].toInt().takeIf { it != UNKNOWN }?.let { it / 10f }

    /** Attack strength in 0..1, or `null` if unknown. */
    fun attackStrengthScale(tick: Int) = attackStrength[tick].toInt().takeIf { it != UNKNOWN }?.let { it / 100f }

    /** Distance to the closest living entity other than the opponent, or `null` if none is near. */
    fun nearestOtherBlocks(tick: Int) = nearestOther[tick].toInt().takeIf { it != UNKNOWN }?.let { it / 16f }

    fun safeDirection(tick: Int, direction: Int) = probes[tick].toInt() and (1 shl Math.floorMod(direction, 16)) != 0

    companion object {
        const val POSITION_UNITS = 4096.0
        const val ANGLE_UNITS = 4096f
        const val SIZE_UNITS = 32f
        const val UNKNOWN = 255
    }

    // Recorded datasets store these bits, so they never change
    object State {
        const val ON_GROUND = 1
        const val SPRINTING = 1 shl 1
        const val SNEAKING = 1 shl 2
        const val USING_ITEM = 1 shl 3
        const val BLOCKING = 1 shl 4
        const val IN_WATER = 1 shl 5
        const val HORIZONTAL_COLLISION = 1 shl 6
        const val LINE_OF_SIGHT = 1 shl 7
        const val OFFHAND_SHIELD = 1 shl 8
        const val FALL_FLYING = 1 shl 9
        const val OTHERS_SWING_NEAR = 1 shl 10
    }

    object Event {
        const val SWING = 1
        const val CRITICAL_HIT = 1 shl 1
        const val MAGIC_CRITICAL_HIT = 1 shl 2
        const val HURT = 1 shl 3
        const val HURT_BY_OPPONENT = 1 shl 4
        const val KNOCKBACK = 1 shl 5
        const val ITEM_SWITCH = 1 shl 6
        const val POSITION_UPDATE = 1 shl 7
        const val ROTATION_UPDATE = 1 shl 8
        const val POSITION_SYNC = 1 shl 9
        const val BURST = 1 shl 10
        const val DEATH = 1 shl 11
    }

    /** Bits of [input], the movement keys held. */
    object Key {
        const val FORWARD = 1
        const val BACK = 1 shl 1
        const val LEFT = 1 shl 2
        const val RIGHT = 1 shl 3
        const val JUMP = 1 shl 4
        const val SNEAK = 1 shl 5
        const val SPRINT = 1 shl 6
    }
}

@UnstableAddonApi
enum class CombatItem { EMPTY, SWORD, AXE, MACE, RANGED, CONSUMABLE, BLOCK, OTHER }

@UnstableAddonApi
class CombatTimeline(
    val id: Long,
    val source: CombatSource,
    val style: CombatStyle,
    val protocol: Int,
    val selfKey: Int,
    val targetKey: Int,
    val selfPing: Int,
    val targetPing: Int,
    val self: CombatTrack,
    val target: CombatTrack,
) {
    val ticks get() = self.ticks

    /** Share of the hits [self] lands in their other fights, see [CombatSkill]. Not stored. */
    var skill = CombatSkill.TARGET

    init {
        require(self.ticks == target.ticks && self.ticks in 1..MAX_TICKS)
    }

    companion object {
        const val MAX_TICKS = 6000
    }
}
