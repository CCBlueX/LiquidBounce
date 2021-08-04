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

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRadius
import net.ccbluex.liquidbounce.utils.client.MC_1_8
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.eyesPos
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext

/**
 * CrystalAura module
 *
 * Automatically places and destroys End Crystals.
 */

object ModuleCrystalAura : Module("CrystalAura", Category.WORLD) {

    private val swing by boolean("Swing", true)
    private val range by float("Range", 4f, 3f..8f)

    var functioning = false

    // Target
    private val targetTracker = tree(TargetTracker())

    // Rotation
    private val rotations = RotationsConfigurable()

    private var currentBlock: BlockPos? = null

    override fun disable() {
        targetTracker.cleanup()
        functioning = false
    }

    val networkTickHandler = repeatable {
        val slot = (0..8).firstOrNull {
            player.inventory.getStack(it).item == Items.END_CRYSTAL
        } ?: return@repeatable

        for (enemy in targetTracker.enemies()) {
            if (player.distanceTo(enemy) > range) {
                return@repeatable
            }

            updateTarget()
            val curr = currentBlock ?: return@repeatable
            val serverRotation = RotationManager.serverRotation ?: return@repeatable

            val rayTraceResult = raytraceBlock(
                range.toDouble(),
                serverRotation,
                curr,
                curr.getState() ?: return@repeatable
            )

            if (rayTraceResult?.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != curr) {
                return@repeatable
            }

            if (slot != player.inventory.selectedSlot) {
                network.sendPacket(UpdateSelectedSlotC2SPacket(slot))
            }

            if (interaction.interactBlock(
                    player,
                    world,
                    Hand.MAIN_HAND,
                    rayTraceResult
                ) == ActionResult.SUCCESS
            ) {
                if (swing) {
                    player.swingHand(Hand.MAIN_HAND)
                } else {
                    network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                }
            }

            if (slot != player.inventory.selectedSlot) {
                network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
            }

            destroy()
        }
    }

    private fun destroy() {
        if (player.isSpectator) {
            return
        }
        targetTracker.validateLock { it.boxedDistanceTo(player) <= range }
        for (block in world.entities) {
            if (block is EndCrystalEntity) {
                if (block.boxedDistanceTo(player) > range) {
                    return
                }
                // find best spot (and skip if no spot was found)
                val (rotation, _) = RotationManager.raytraceBox(
                    player.eyesPos,
                    block.boundingBox,
                    range = range.toDouble(),
                    wallsRange = 0.0
                ) ?: continue

                // lock on target tracker
                targetTracker.lock(block)

                // aim on target
                RotationManager.aimAt(rotation, configurable = rotations)
                break
            }
        }

        val entity = targetTracker.lockedOnTarget ?: return
        attackEntity(entity)
    }

    private fun attackEntity(entity: Entity) {
        EventManager.callEvent(AttackEvent(entity))

        // Swing before attacking (on 1.8)
        if (swing && protocolVersion == MC_1_8) {
            player.swingHand(Hand.MAIN_HAND)
        }

        network.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, player.isSneaking))

        // Swing after attacking (on 1.9+)
        if (swing && protocolVersion != MC_1_8) {
            player.swingHand(Hand.MAIN_HAND)
        }

        // Reset cooldown
        player.resetLastAttackedTicks()
    }

    private fun updateTarget() {
        currentBlock = null

        val targetedBlocks = hashSetOf<Block>()

        targetedBlocks.addAll(listOf(Blocks.OBSIDIAN, Blocks.BEDROCK))

        val radius = range + 1
        val radiusSquared = radius * radius
        val eyesPos = player.eyesPos

        val blockToProcess = searchBlocksInRadius(radius) { pos, state ->
            targetedBlocks.contains(state.block) && getNearestPoint(
                eyesPos,
                Box(pos, pos.add(1, 1, 1))
            ).squaredDistanceTo(eyesPos) <= radiusSquared
        }.minByOrNull { it.first.getCenterDistanceSquared() } ?: return

        val (pos, state) = blockToProcess

        val rt = RotationManager.raytraceBlock(
            player.eyesPos,
            pos,
            state,
            range = range.toDouble(),
            wallsRange = 0.0
        )

        // We got a free angle at the block? Cool.
        if (rt != null) {
            val (rotation, _) = rt
            RotationManager.aimAt(rotation, configurable = rotations)
            currentBlock = pos
            return
        }

        val raytraceResult = world.raycast(
            RaycastContext(
                player.eyesPos,
                Vec3d.of(pos).add(0.5, 0.5, 0.5),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
            )
        ) ?: return

        // Failsafe. Should not trigger
        if (raytraceResult.type != HitResult.Type.BLOCK) return

        currentBlock = raytraceResult.blockPos
    }
}
