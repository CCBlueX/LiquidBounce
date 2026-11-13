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
package net.ccbluex.liquidbounce.features.module.modules.world.nuker.mode

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.CancelBlockBreakingEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.areaMode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.ignoreOpenInventory
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.mode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.wasTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.Surface
import net.ccbluex.liquidbounce.utils.aiming.lookAt
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockRotation
import net.ccbluex.liquidbounce.utils.aiming.utils.selectBlockTarget
import net.ccbluex.liquidbounce.utils.block.doBreak
import net.ccbluex.liquidbounce.utils.block.isNotBreakable
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.ccbluex.liquidbounce.utils.render.BreakingProgress
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import kotlin.math.max

object LegitNukerMode : Mode("Legit") {

    private var currentTarget: BlockPos? = null

    /** Aims at [currentTarget], chosen anew every tick; null while PacketMine breaks it. */
    private var aim: RotationTarget? = null

    override val parent: ModeValueGroup<Mode>
        get() = mode

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).onChange {
        minOf(it, range)
    }

    internal val forceImmediateBreak by boolean("ForceImmediateBreak", false)
    private val rotations = tree(RotationsValueGroup(this))
    private val switchDelay by int("SwitchDelay", 0, 0..20, "ticks")

    internal fun breakingProgress(): BreakingProgress? {
        if (ModulePacketMine.running) {
            return null
        }

        val target = currentTarget ?: return null
        if (forceImmediateBreak) {
            return wasTarget?.takeIf { it == target }?.let { BreakingProgress(it, 1f) }
        }

        return BreakingProgress.Provider.Default.breakingProgress(target)
    }

    @Suppress("unused")
    private val simulatedTickHandler = handler<RotationUpdateEvent> {
        aim = null

        if (!ignoreOpenInventory && mc.gui.screen() is AbstractContainerScreen<*>) {
            this.currentTarget = null
            return@handler
        }

        if (ModuleBlink.running) {
            this.currentTarget = null
            return@handler
        }

        val target = lookupTarget()
        this.currentTarget = target?.first

        if (target == null) {
            wasTarget = null
            return@handler
        }

        val (pos, plan) = target
        if (ModulePacketMine.running) {
            ModulePacketMine.setTarget(pos)
        } else {
            aim = plan
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val target = currentTarget ?: return@tickHandler

        if (ModulePacketMine.running) {
            return@tickHandler
        }

        requires { currentTarget == target }
        val plan by requires { aim }

        // Keeps aiming while the switch delay passes
        val switching = wasTarget != null && target != wasTarget
        val delayEnd = player.tickCount + switchDelay
        val rayTraceResult = lookAt(Priority.IMPORTANT_FOR_USAGE_1, { plan }) { going ->
            if (switching && player.tickCount < delayEnd) null else hitOn(target, going)
        }

        doBreak(rayTraceResult, forceImmediateBreak)
        wasTarget = target
    }

    @Suppress("unused")
    private val cancelBlockBreakingHandler = handler<CancelBlockBreakingEvent> { event ->
        if (currentTarget != null && !ModulePacketMine.running) {
            event.cancelEvent()
        }
    }

    /**
     * Chooses the best block to break next, with the target that aims at it.
     */
    private fun lookupTarget(): Pair<BlockPos, RotationTarget>? {
        val eyes = player.eyePosition

        // Check if the current target is still valid
        currentTarget?.let { pos ->
            val blockState = pos.state ?: return@let

            if (blockState.isNotBreakable(pos) || !ModuleNuker.isValid(blockState)) {
                return@let
            }

            val raytraceResult = raytraceBlockRotation(
                eyes = eyes,
                pos = pos,
                state = blockState,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble(),
            ) ?: return@let

            // We don't need to update the target if it's still valid
            return pos to rotations.toRotationTarget(
                raytraceResult.rotation,
                considerInventory = !ignoreOpenInventory,
                surface = Surface.of(pos, throughWalls = wallRange > 0f),
            )
        }

        return selectBlockTarget(
            eyes,
            range,
            wallRange,
            areaMode.activeMode.lookupTargets(range),
            rotations,
            considerInventory = !ignoreOpenInventory,
        )
    }

    /**
     * What breaking with [rotation] hits, once the ray, which may pass through other blocks within the wall range,
     * meets [pos].
     */
    private fun hitOn(pos: BlockPos, rotation: Rotation): BlockHitResult? {
        val state = pos.state ?: return null
        return raytraceBlock(max(range, wallRange).toDouble() + 1.0, rotation, pos, state)
            ?.takeIf { it.type == HitResult.Type.BLOCK && it.blockPos == pos }
    }

}
