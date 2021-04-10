/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.sequenceHandler
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos

/**
 * Changes the speed of the entire game.
 */
object ModuleChestAura : Module("ChestAura", Category.WORLD) {

    private val range by float("Range", 5F, 1F..6F)
    private val delay by int("Delay", 5, 1..80)
    private val visualSwing by boolean("VisualSwing", true)
    private val chest by blocks("Chest", mutableListOf(Blocks.CHEST))
    private val throughWalls by boolean("ThroughWalls", false)

    // Rotation
    private val rotations = RotationsConfigurable()

    private var currentBlock: BlockPos? = null

    val clickedBlocks = hashSetOf<BlockPos>()

    val motionHandler = sequenceHandler<PlayerNetworkMovementTickEvent> { event ->
        val player = mc.player ?: return@sequenceHandler

        // TODO Allow other modules to block chest aura

        when (event.state) {
            EventState.PRE -> {
                if (mc.currentScreen is HandledScreen<*>)
                    wait { delay }

                val targetedBlocks = HashSet<Block>()

                targetedBlocks.addAll(chest)

                val radius = range + 1
                val radiusSquared = radius * radius

                val eyesPos = player.eyesPos

                val blocksToProcess = searchBlocks(radius.toInt()) { pos, state ->
                    targetedBlocks.contains(state.block) && pos !in clickedBlocks && pos.getCenterDistanceSquared() <= radiusSquared
                }.sortedBy { it.first.getCenterDistanceSquared() }

                var finalRotation: Rotation? = null

                for ((pos, state) in blocksToProcess) {
                    val (rotation, _) = RotationManager.raytraceBlock(
                        eyesPos,
                        pos,
                        state,
                        throughWalls = throughWalls,
                        range = ModuleKillAura.range.toDouble()
                    ) ?: continue

                    finalRotation = rotation
                    currentBlock = pos
                    break
                }

                if (finalRotation == null)
                    return@sequenceHandler

                // aim on target
                RotationManager.aimAt(finalRotation, configurable = rotations)
            }
            EventState.POST -> {
                val curr = currentBlock
                val severRot = RotationManager.serverRotation

                if (curr != null && severRot != null) {
                    val rayTraceResult = raytraceBlock(range.toDouble(), severRot, curr, curr.getState()!!)

                    if (rayTraceResult?.type == HitResult.Type.MISS) {
                        return@sequenceHandler
                    }

                    if (mc.interactionManager!!.interactBlock(
                            player,
                            mc.world!!,
                            Hand.MAIN_HAND,
                            rayTraceResult
                        ) == ActionResult.SUCCESS
                    ) {
                        if (visualSwing)
                            player.swingHand(Hand.MAIN_HAND)
                        else
                            mc.networkHandler!!.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))

                        clickedBlocks.add(currentBlock!!)
                        currentBlock = null
                        wait { delay }
                    }
                }
            }
        }
    }

    override fun disable() {
        clickedBlocks.clear()
    }

}
