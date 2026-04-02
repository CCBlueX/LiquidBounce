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
package net.ccbluex.liquidbounce.utils.block.breaker

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.CancelBlockBreakingEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockRotation
import net.ccbluex.liquidbounce.utils.block.doBreak
import net.ccbluex.liquidbounce.utils.block.immutable
import net.ccbluex.liquidbounce.utils.block.isNotBreakable
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.HitResult
import java.util.function.BooleanSupplier
import kotlin.math.max

/**
 * Reusable helper for single-target block breaking.
 *
 * The caller decides which block should be broken. This helper owns the
 * execution side: validating targets, requesting rotations, forwarding to
 * PacketMine when enabled, handling the vanilla destroy lifecycle and
 * performing cleanup when the active target changes.
 */
class BlockBreaker(
    name: String,
    private val owner: EventListener,
    private val defaultPriority: Priority = Priority.IMPORTANT_FOR_USAGE_1,
    private val ignoreOpenInventorySupplier: BooleanSupplier? = null,
) : ValueGroup(name), EventListener, MinecraftShortcuts {

    val range by float("Range", 5f, 1f..6f)
    val wallRange by float("WallRange", 0f, 0f..6f).onChange {
        minOf(range, it)
    }

    val switchDelay by int("SwitchDelay", 0, 0..20, "ticks")
    val forceImmediateBreak by boolean("ForceImmediateBreak", false)
    private val localIgnoreOpenInventoryValue = if (ignoreOpenInventorySupplier == null) {
        boolean("IgnoreOpenInventory", true)
    } else {
        null
    }
    val ignoreOpenInventory: Boolean
        get() = ignoreOpenInventorySupplier?.asBoolean ?: localIgnoreOpenInventoryValue!!.get()
    val ignoreUsingItem by boolean("IgnoreUsingItem", true)
    val prioritizeOverKillAura by boolean("PrioritizeOverKillAura", false)

    val rotations = tree(RotationsValueGroup(this))

    var currentTarget: PreparedTarget? = null
        private set

    private var targetChanged = false
    private var trackedPacketMineTarget: BlockPos? = null

    /**
     * Returns whether block breaking should currently be paused because of shared constraints.
     */
    fun isBlocked(): Boolean {
        if (ModuleBlink.running) {
            return true
        }

        if (!ignoreOpenInventory && mc.screen is AbstractContainerScreen<*>) {
            return true
        }

        if (!ignoreUsingItem && player.isUsingItem) {
            return true
        }

        return false
    }

    /**
     * Validates a candidate block and resolves the rotation needed to break it.
     *
     * Returns `null` when the block is not breakable or cannot currently be
     * targeted with the configured ranges.
     */
    fun prepareTarget(pos: BlockPos, wallRange: Double = this.wallRange.toDouble()): PreparedTarget? {
        val immutablePos = pos.immutable
        val state = immutablePos.state ?: return null
        if (state.isNotBreakable(immutablePos)) {
            return null
        }

        val raytrace = raytraceBlockRotation(
            player.eyePosition,
            immutablePos,
            state,
            range = range.toDouble(),
            wallsRange = wallRange,
        ) ?: return null

        return PreparedTarget(immutablePos, raytrace.rotation, wallRange)
    }

    /**
     * Replaces the active target that should be broken.
     *
     * Switching to a different position stops the current vanilla destroy
     * interaction before the new target becomes active.
     */
    fun setTarget(target: PreparedTarget?) {
        val currentPos = currentTarget?.pos
        val nextPos = target?.pos

        if (currentPos == nextPos) {
            currentTarget = target
            return
        }

        if (currentPos != null) {
            interaction.stopDestroyBlock()
        }

        currentTarget = target

        if (target == null) {
            targetChanged = false
            releasePacketMineTarget()
            return
        }

        targetChanged = true
    }

    /**
     * Clears the active target and forgets any tracked destroy progress.
     */
    fun clear() {
        setTarget(null)
    }

    /**
     * Resets breaker runtime state when the owning module or mode is disabled.
     */
    fun disable() {
        clear()
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent>(priority = -20) {
        if (isBlocked()) {
            return@handler
        }

        if (ModulePacketMine.running) {
            return@handler
        }

        val target = currentTarget ?: return@handler
        RotationManager.setRotationTarget(
            target.rotation,
            considerInventory = !ignoreOpenInventory,
            valueGroup = rotations,
            priority = if (prioritizeOverKillAura) Priority.IMPORTANT_FOR_USAGE_3 else defaultPriority,
            provider = owner,
        )
    }

    @Suppress("unused")
    private val breakHandler = tickHandler {
        if (isBlocked()) {
            return@tickHandler
        }

        if (targetChanged) {
            targetChanged = false

            if (switchDelay > 0) {
                waitTicks(switchDelay)
            }

            if (targetChanged) {
                return@tickHandler
            }
        }

        val target = currentTarget ?: return@tickHandler
        if (ModulePacketMine.running) {
            ModulePacketMine.setTarget(target.pos)
            trackedPacketMineTarget = target.pos
            return@tickHandler
        }

        val state = target.pos.state ?: run {
            clear()
            return@tickHandler
        }

        if (state.isNotBreakable(target.pos)) {
            clear()
            return@tickHandler
        }

        val rayTraceResult = raytraceBlock(
            max(range.toDouble(), target.wallRange),
            RotationManager.serverRotation,
            target.pos,
            state
        ) ?: return@tickHandler

        if (rayTraceResult.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != target.pos) {
            return@tickHandler
        }

        doBreak(rayTraceResult, immediate = forceImmediateBreak)
    }

    @Suppress("unused")
    private val cancelBlockBreakingHandler = handler<CancelBlockBreakingEvent> { event ->
        if (currentTarget != null && !ModulePacketMine.running) {
            event.cancelEvent()
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        clear()
    }

    private fun releasePacketMineTarget() {
        val trackedTarget = trackedPacketMineTarget ?: return
        if (ModulePacketMine._target?.targetPos == trackedTarget) {
            ModulePacketMine._resetTarget()
        }
        trackedPacketMineTarget = null
    }

    override fun parent(): EventListener = owner

    /**
     * Prepared target data used by the breaker execution path.
     */
    @JvmRecord
    data class PreparedTarget(
        val pos: BlockPos,
        val rotation: Rotation,
        val wallRange: Double,
    )
}
