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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockRotation
import net.ccbluex.liquidbounce.utils.entity.interactBlock
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.ccbluex.liquidbounce.utils.text.stripMinecraftColorCodes
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

/**
 * The chests in the loaded chunks around the player, and the ones of this world looted or given up. ChestAura opens
 * the ones the walk passes; a chest it opened counts as looted. One the game wants opened is opened with [open]:
 * aimed at ahead of KillAura, which keeps ChestAura out while in combat, and clicked until a screen opens.
 *
 * A refill, told in chat or by the sidebar timer running out, makes every chest unopened again; ChestAura keeps
 * skipping the ones it opened, so those are left to [open]. Logs `chests refilled source=<chat|timer> chests=<n>`.
 */
@Suppress("TooManyFunctions")
internal class Chests(
    private val owner: EventListener,
    private val rotations: RotationsValueGroup,
    private val scanChunks: Int,
) : EventListener, MinecraftShortcuts {

    private val loaded = HashSet<BlockPos>()
    private val done = HashSet<BlockPos>()

    /** Opened by ChestAura before the last refill, which it does not open again. */
    private val refilled = HashSet<BlockPos>()
    private var refillAt: Int? = null
    private var refilledAt = NEVER

    private var picked: BlockPos? = null

    private var ticks = 0
    private var opening: BlockPos? = null
    private var wantedAt = NEVER
    private var clickedAt = NEVER
    private var clicks = 0

    val all: Set<BlockPos>
        get() = loaded

    /** Seconds until the next refill the sidebar counts down to, or null when it shows none. */
    val refillIn: Int?
        get() = refillAt?.let { ((it - ticks) / TICKS_PER_SECOND).coerceAtLeast(0) }

    /** Whether a chest is being opened, which needs the player to stay in reach. */
    val isOpening: Boolean
        get() = opening != null && ticks - wantedAt <= 1

    override fun parent() = owner

    fun unopened(): Sequence<BlockPos> = loaded.asSequence().filter { it !in done }

    fun isUnopened(pos: BlockPos) = pos in loaded && pos !in done

    /** The closest of [candidates]; the last pick stays while it is among them, so close chests do not take turns. */
    fun pick(candidates: Sequence<BlockPos>): BlockPos? {
        val list = candidates.toList()
        val position = player.position()
        val next = picked?.takeIf { it in list } ?: list.minByOrNull { Vec3.atCenterOf(it).distanceToSqr(position) }
        picked = next
        return next
    }

    /** Whether [pos] can be opened from where the player stands. */
    fun isInReach(pos: BlockPos) = player.eyePosition.distanceToSqr(Vec3.atCenterOf(pos)) <= REACH * REACH

    /** Opens [pos], which lies [isInReach]; call every tick until it counts as looted or skipped. */
    fun open(pos: BlockPos) {
        if (opening != pos) {
            opening = pos
            clicks = 0
            clickedAt = NEVER
        }
        wantedAt = ticks
    }

    fun skip(pos: BlockPos) {
        done += pos
        if (opening == pos) {
            opening = null
        }
        report("${label(pos)} skipped")
        MapRecorder.event("chest", "pos" to pos, "state" to "skipped")
    }

    /** ChestAura also counts the bot's own clicks; the chest being opened only counts once its screen opened. */
    fun noteLooted() {
        for (pos in FeatureChestAura.interactedBlocksSet) {
            if (pos != opening && pos in loaded && pos !in refilled && done.add(pos)) {
                reportLooted(pos)
            }
        }
    }

    private fun reportLooted(pos: BlockPos) {
        report("${label(pos)} looted")
        MapRecorder.event("chest", "pos" to pos, "state" to "looted")
    }

    private fun refill(source: String) {
        if (ticks - refilledAt < REFILL_GAP_TICKS) {
            return
        }
        refilledAt = ticks
        refilled += FeatureChestAura.interactedBlocksSet
        refilled += done
        report("chests refilled source=$source chests=${done.size}")
        MapRecorder.event("refill", "source" to source, "chests" to done.size)
        done.clear()
        picked = null
    }

    /** Reads `Refill 2:30` from the sidebar; the timer jumping up or vanishing once due is the refill. */
    private fun readRefillTimer() {
        val due = refillAt?.let { ticks >= it - REFILL_DUE_TICKS } == true
        val seconds = sidebarRefill()
        if (due && (seconds == null || seconds * TICKS_PER_SECOND > REFILL_DUE_TICKS)) {
            refill("timer")
        }
        refillAt = seconds?.let { ticks + it * TICKS_PER_SECOND }
    }

    private fun sidebarRefill(): Int? {
        val scoreboard = world.scoreboard
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return null
        for (entry in scoreboard.listPlayerScores(objective)) {
            val line = PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName())
                .string.stripMinecraftColorCodes()
            REFILL_TIMER.find(line)?.let {
                return it.groupValues[1].toInt() * SECONDS_PER_MINUTE + it.groupValues[2].toInt()
            }
        }
        return null
    }

    @Suppress("unused")
    private val chatHandler = handler<ChatReceiveEvent> { event ->
        val message = event.message
        // Server lines carry no "name: text" part.
        if (event.type == ChatReceiveEvent.ChatType.GAME_MESSAGE && !message.contains(": ") &&
            REFILLED.containsMatchIn(message)
        ) {
            refill("chat")
        }
    }

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        val pos = opening?.takeIf { isOpening && !isScreenOpen() } ?: return@handler
        val state = world.getBlockState(pos)
        val aim = raytraceBlockRotation(player.eyePosition, pos, state, REACH, REACH)?.rotation ?: return@handler
        RotationManager.setRotationTarget(
            aim,
            valueGroup = rotations,
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
            provider = owner,
        )
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        ticks++
        if (ticks % SIDEBAR_TICKS == 0) {
            readRefillTimer()
        }
        val pos = opening ?: return@handler
        if (isScreenOpen()) {
            if (clicks > 0 && ticks - clickedAt <= SCREEN_TICKS) {
                opening = null
                if (done.add(pos)) {
                    reportLooted(pos)
                }
            }
            return@handler
        }
        if (!isOpening) {
            opening = null
            return@handler
        }
        // A click would cut eating short.
        if (ticks - clickedAt < SCREEN_TICKS || Eating.isEating) {
            return@handler
        }
        if (clicks >= MAX_CLICKS) {
            skip(pos)
            return@handler
        }

        // The click goes out before this tick's rotation, so it has to match the rotation the server has.
        val state = world.getBlockState(pos)
        val hit = raytraceBlock(REACH, RotationManager.serverRotation, pos, state) ?: return@handler
        if (hit.type == HitResult.Type.BLOCK && hit.blockPos == pos) {
            interactBlock(hit)
            clicks++
            clickedAt = ticks
        }
    }

    private fun isScreenOpen() = mc.gui.screen() is AbstractContainerScreen<*>

    fun scan() {
        loaded.clear()
        val chunkX = player.blockX shr CHUNK_SHIFT
        val chunkZ = player.blockZ shr CHUNK_SHIFT
        for (x in chunkX - scanChunks..chunkX + scanChunks) {
            for (z in chunkZ - scanChunks..chunkZ + scanChunks) {
                val chunk = world.chunkSource.getChunk(x, z, ChunkStatus.FULL, false) ?: continue
                for ((pos, blockEntity) in chunk.blockEntities) {
                    if (blockEntity is ChestBlockEntity) {
                        loaded += pos
                    }
                }
            }
        }
    }

    fun clear() {
        loaded.clear()
        done.clear()
        refilled.clear()
        refillAt = null
        refilledAt = NEVER
        picked = null
        opening = null
    }

    fun label(pos: BlockPos) = "chest ${pos.x},${pos.y},${pos.z}"

    private companion object {
        const val CHUNK_SHIFT = 4
        const val NEVER = Int.MIN_VALUE / 2

        /** From the eyes to the chest center. */
        const val REACH = 4.0

        /** A screen opening this long after a click came from it; without one, the chest is clicked again. */
        const val SCREEN_TICKS = 10
        const val MAX_CLICKS = 4

        const val TICKS_PER_SECOND = 20
        const val SECONDS_PER_MINUTE = 60
        const val SIDEBAR_TICKS = 20

        /** A timer this close to zero that vanishes or starts over has run out. */
        const val REFILL_DUE_TICKS = 3 * TICKS_PER_SECOND

        /** The chat line and the timer tell the same refill. */
        const val REFILL_GAP_TICKS = 20 * TICKS_PER_SECOND

        val REFILL_TIMER = Regex("refill\\D{0,4}(\\d{1,2}):(\\d{2})", RegexOption.IGNORE_CASE)
        val REFILLED = Regex("chests? (?:have|has) been refilled", RegexOption.IGNORE_CASE)
    }

}
