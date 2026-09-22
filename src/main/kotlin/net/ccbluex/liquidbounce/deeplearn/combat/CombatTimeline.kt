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
package net.ccbluex.liquidbounce.deeplearn.combat

import net.ccbluex.liquidbounce.deeplearn.model.atomicWrite
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream

@UnstableAddonApi
enum class CombatSource { FIRST_PERSON, OBSERVED, OBSERVED_VS_LOCAL }

/** Stored by ordinal in timelines; [id] names the model variant. */
@UnstableAddonApi
enum class CombatStyle(val id: String) { LEGACY("legacy"), COOLDOWN("cooldown") }

/**
 * Raw per-tick state of one fighter. Positions are relative to the timeline origin.
 */
@Suppress("TooManyFunctions")
@UnstableAddonApi
class CombatTrack(val ticks: Int) {
    val x = IntArray(ticks)
    val y = IntArray(ticks)
    val z = IntArray(ticks)
    val yaw = IntArray(ticks)
    val pitch = IntArray(ticks)
    val state = ShortArray(ticks)
    val events = ShortArray(ticks)
    val probes = ShortArray(ticks)
    val hurtTime = ByteArray(ticks)
    val health = ByteArray(ticks)
    val attackDelay = ByteArray(ticks)
    val attackStrength = ByteArray(ticks)
    val item = ByteArray(ticks)
    val width = ByteArray(ticks)
    val height = ByteArray(ticks)
    val eyeHeight = ByteArray(ticks)
    val input = ByteArray(ticks)
    val clicks = ByteArray(ticks)
    val nearestOther = ByteArray(ticks)

    val intColumns get() = arrayOf(x, y, z, yaw, pitch)
    val shortColumns get() = arrayOf(state, events, probes)
    val byteColumns
        get() = arrayOf(
            hurtTime, health, attackDelay, attackStrength, item, width, height, eyeHeight, input, clicks, nearestOther
        )

    fun has(tick: Int, flag: Int) = state[tick].toInt() and flag != 0

    fun hasEvent(tick: Int, flag: Int) = events[tick].toInt() and flag != 0

    fun positionX(tick: Int) = x[tick] / POSITION_UNITS
    fun positionY(tick: Int) = y[tick] / POSITION_UNITS
    fun positionZ(tick: Int) = z[tick] / POSITION_UNITS
    fun yawDegrees(tick: Int) = yaw[tick] / ANGLE_UNITS
    fun pitchDegrees(tick: Int) = pitch[tick] / ANGLE_UNITS
    fun widthBlocks(tick: Int) = unsigned(width, tick) / SIZE_UNITS
    fun heightBlocks(tick: Int) = unsigned(height, tick) / SIZE_UNITS
    fun eyeHeightBlocks(tick: Int) = unsigned(eyeHeight, tick) / SIZE_UNITS

    /** Health relative to maximum health, or `null` when the server hides it. */
    fun healthRatio(tick: Int) = unsigned(health, tick).takeIf { it != UNKNOWN }?.let { it / 254f }

    /** Ticks between full-strength attacks, or `null` if unknown. */
    fun attackDelayTicks(tick: Int) = unsigned(attackDelay, tick).takeIf { it != UNKNOWN }?.let { it / 10f }

    /** Attack strength in 0..1, or `null` if unknown. */
    fun attackStrengthScale(tick: Int) = unsigned(attackStrength, tick).takeIf { it != UNKNOWN }?.let { it / 100f }

    /** Distance to the closest living entity other than the opponent, or `null` if none is near. */
    fun nearestOtherBlocks(tick: Int) = unsigned(nearestOther, tick).takeIf { it != UNKNOWN }?.let { it / 16f }

    fun safeDirection(tick: Int, direction: Int) = probes[tick].toInt() and (1 shl Math.floorMod(direction, 16)) != 0

    companion object {
        const val POSITION_UNITS = 4096.0
        const val ANGLE_UNITS = 4096f
        const val SIZE_UNITS = 32f
        const val UNKNOWN = 255

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
        const val ATTACK = 1 shl 12

        const val FORWARD = 1
        const val BACK = 1 shl 1
        const val LEFT = 1 shl 2
        const val RIGHT = 1 shl 3
        const val JUMP = 1 shl 4
        const val SNEAK = 1 shl 5
        const val SPRINT = 1 shl 6

        private fun unsigned(values: ByteArray, tick: Int) = values[tick].toInt() and 0xFF
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

@UnstableAddonApi
object CombatTimelineFiles {
    const val EXTENSION = "timeline"
    private const val MAGIC = 0x4C424354
    private const val VERSION = 1
    private const val MAX_TIMELINES = 100_000

    fun write(path: Path, timelines: Collection<CombatTimeline>) {
        atomicWrite(path, compressed = true) { output ->
            output.writeInt(MAGIC)
            output.writeShort(VERSION)
            output.writeInt(timelines.size)
            timelines.forEach { output.writeTimeline(it) }
        }
    }

    fun read(path: Path): List<CombatTimeline> = Files.newInputStream(path).use(::read)

    fun read(stream: InputStream): List<CombatTimeline> =
        DataInputStream(GZIPInputStream(stream).buffered()).let { input ->
            require(input.readInt() == MAGIC) { "Not a combat timeline" }
            require(input.readShort().toInt() == VERSION) { "Unsupported combat timeline version" }
            val count = input.readInt()
            require(count in 0..MAX_TIMELINES)
            List(count) { input.readTimeline() }
        }

    private fun DataOutputStream.writeTimeline(timeline: CombatTimeline) {
        writeLong(timeline.id)
        writeByte(timeline.source.ordinal)
        writeByte(timeline.style.ordinal)
        writeInt(timeline.protocol)
        writeInt(timeline.selfKey)
        writeInt(timeline.targetKey)
        writeShort(timeline.selfPing.coerceIn(-1, Short.MAX_VALUE.toInt()))
        writeShort(timeline.targetPing.coerceIn(-1, Short.MAX_VALUE.toInt()))
        writeInt(timeline.ticks)
        writeTrack(timeline.self)
        writeTrack(timeline.target)
    }

    private fun DataInputStream.readTimeline(): CombatTimeline {
        val id = readLong()
        val source = CombatSource.entries[readUnsignedByte()]
        val style = CombatStyle.entries[readUnsignedByte()]
        val protocol = readInt()
        val selfKey = readInt()
        val targetKey = readInt()
        val selfPing = readShort().toInt()
        val targetPing = readShort().toInt()
        val ticks = readInt()
        require(ticks in 1..CombatTimeline.MAX_TICKS)
        return CombatTimeline(id, source, style, protocol, selfKey, targetKey, selfPing, targetPing,
            readTrack(ticks), readTrack(ticks))
    }

    private fun DataOutputStream.writeTrack(track: CombatTrack) {
        for (column in track.intColumns) {
            var previous = 0
            for (value in column) {
                writeVarInt(value - previous)
                previous = value
            }
        }
        for (column in track.shortColumns) {
            column.forEach { writeShort(it.toInt()) }
        }
        for (column in track.byteColumns) {
            write(column)
        }
    }

    private fun DataInputStream.readTrack(ticks: Int) = CombatTrack(ticks).also { track ->
        for (column in track.intColumns) {
            var previous = 0
            for (index in column.indices) {
                previous += readVarInt()
                column[index] = previous
            }
        }
        for (column in track.shortColumns) {
            for (index in column.indices) {
                column[index] = readShort()
            }
        }
        for (column in track.byteColumns) {
            readFully(column)
        }
    }

    private fun DataOutputStream.writeVarInt(value: Int) {
        var zigzag = (value shl 1) xor (value shr 31)
        while (zigzag and 0x7F.inv() != 0) {
            writeByte(zigzag and 0x7F or 0x80)
            zigzag = zigzag ushr 7
        }
        writeByte(zigzag)
    }

    private fun DataInputStream.readVarInt(): Int {
        var result = 0
        var shift = 0
        while (true) {
            val byte = readUnsignedByte()
            result = result or (byte and 0x7F shl shift)
            if (byte and 0x80 == 0) {
                break
            }
            shift += 7
            require(shift < 35) { "Malformed combat timeline" }
        }
        return (result ushr 1) xor -(result and 1)
    }
}
