/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.CancelBlockBreakingEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.areaMode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.ignoreOpenInventory
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.mode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.wasTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.doBreak
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isNotBreakable
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import kotlin.math.max

object LegitNukerMode : Choice("Legit") {

    private var currentTarget: BlockPos? = null

    override val parent: ChoiceConfigurable<Choice>
        get() = mode

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).onChange {
        minOf(it, range)
    }

    private val forceImmediateBreak by boolean("ForceImmediateBreak", false)
    private val rotations = tree(RotationsConfigurable(this))
    private val switchDelay by int("SwitchDelay", 0, 0..20, "ticks")

    @Suppress("unused")
    private val simulatedTickHandler = handler<RotationUpdateEvent> {
        if (!ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            this.currentTarget = null
            return@handler
        }

        if (ModuleBlink.running) {
            this.currentTarget = null
            return@handler
        }

        this.currentTarget = lookupTarget()

        val currentTarget = currentTarget
        if (currentTarget == null) {
            wasTarget = null
            return@handler
        }

        if (ModulePacketMine.running) {
            ModulePacketMine.setTarget(currentTarget)
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val currentTarget = currentTarget ?: return@tickHandler
        val state = currentTarget.getState() ?: return@tickHandler

        if (ModulePacketMine.running) {
            return@tickHandler
        }

        // Wait for the switch delay to pass
        if (wasTarget != null && currentTarget != wasTarget) {
            waitTicks(switchDelay)
        }

        val rayTraceResult = raytraceBlock(
            max(range, wallRange).toDouble() + 1.0,
            pos = currentTarget,
            state = state
        ) ?: return@tickHandler

        if (rayTraceResult.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != currentTarget) {
            return@tickHandler
        }

        doBreak(rayTraceResult, forceImmediateBreak)
        wasTarget = currentTarget
    }

    @Suppress("unused")
    private val cancelBlockBreakingHandler = handler<CancelBlockBreakingEvent> { event ->
        if (currentTarget != null && !ModulePacketMine.running) {
            event.cancelEvent()
        }
    }

    /**
     * Chooses the best block to break next and aims at it.
     */
    private fun lookupTarget(): BlockPos? {
        val eyes = player.eyePos
        val packetMine = ModulePacketMine.running

        // Check if the current target is still valid
        currentTarget?.let { pos ->
            val blockState = pos.getState() ?: return@let

            if (blockState.isNotBreakable(pos) || !ModuleNuker.isValid(blockState)) {
                return@let
            }

            val raytraceResult = raytraceBlock(
                eyes = eyes,
                pos = pos,
                state = blockState,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble(),
            ) ?: return@let

            if (!packetMine) {
                RotationManager.setRotationTarget(
                    raytraceResult.rotation,
                    considerInventory = !ignoreOpenInventory,
                    configurable = rotations,
                    priority = Priority.IMPORTANT_FOR_USAGE_1,
                    ModuleNuker
                )
            }

            // We don't need to update the target if it's still valid
            return pos
        }

        for ((pos, blockState) in areaMode.activeChoice.lookupTargets(range)) {
            val raytraceResult = raytraceBlock(
                eyes = eyes,
                pos = pos,
                state = blockState,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble(),
            ) ?: continue

            if (!packetMine) {
                RotationManager.setRotationTarget(
                    raytraceResult.rotation,
                    considerInventory = !ignoreOpenInventory,
                    configurable = rotations,
                    priority = Priority.IMPORTANT_FOR_USAGE_1,
                    ModuleNuker
                )
            }

            return pos
        }

        return null
    }

}
