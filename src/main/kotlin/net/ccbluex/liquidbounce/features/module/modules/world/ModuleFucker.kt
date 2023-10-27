/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.item.findBlocksEndingWith
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext

/**
 * Fucker module
 *
 * Destroys/Uses selected blocks around you.
 */
object ModuleFucker : Module("Fucker", Category.WORLD) {

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).listen {
        if (it > range) {
            range
        } else {
            it
        }
    }
    private val visualSwing by boolean("VisualSwing", true)
    //private val targets by blocks("Target", findBlocksEndingWith("_BED", "DRAGON_EGG").toHashSet())
    private val action by enumChoice("Action", DestroyAction.USE, DestroyAction.values())
    private val forceImmediateBreak by boolean("ForceImmediateBreak", false)
    private val switchDelay by int("SwitchDelay", 0, 0..20)
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    // Rotation
    private val rotations = tree(RotationsConfigurable())

    private var currentTarget: DestroyerTarget? = null

    // todo: Remove when the blocks option actually works
    private val targetedBlocks = findBlocksEndingWith("_BED", "DRAGON_EGG").toHashSet()

    val moduleRepeatable = repeatable {
        if (mc.currentScreen is HandledScreen<*>) {
            wait { switchDelay }
            return@repeatable
        }

        updateTarget()

        if (ModuleBlink.enabled) {
            return@repeatable
        }

        val curr = currentTarget ?: return@repeatable
        val currentRotation = RotationManager.currentRotation ?: return@repeatable

        val rayTraceResult = raytraceBlock(
            range.toDouble(), currentRotation, curr.pos, curr.pos.getState() ?: return@repeatable
        ) ?: return@repeatable

        if (rayTraceResult.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != curr.pos) {
            return@repeatable
        }

        if (curr.action == DestroyAction.USE) {
            if (interaction.interactBlock(player, Hand.MAIN_HAND, rayTraceResult) == ActionResult.SUCCESS) {
                player.swingHand(Hand.MAIN_HAND)
            }

            wait { switchDelay }

            return@repeatable
        } else {
            val blockPos = rayTraceResult.blockPos

            if (blockPos.getState()?.isAir == false) {
                val direction = rayTraceResult.side

                if (forceImmediateBreak) {
                    network.sendPacket(
                        PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction
                        )
                    )
                    swingHand()
                    network.sendPacket(
                        PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction
                        )
                    )
                } else {
                    if (interaction.updateBlockBreakingProgress(blockPos, direction)) {
                        swingHand()
                    }
                }
            }
        }
    }

    private fun swingHand() {
        if (visualSwing) {
            player.swingHand(Hand.MAIN_HAND)
        } else {
            network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
        }
    }

    private fun updateTarget() {
        this.currentTarget = null

        val radius = range + 1
        val radiusSquared = radius * radius
        val eyesPos = player.eyes

        val blockToProcess = searchBlocksInCuboid(radius.toInt()) { pos, state ->
            targetedBlocks.contains(state.block) && getNearestPoint(
                eyesPos, Box(pos, pos.add(1, 1, 1))
            ).squaredDistanceTo(eyesPos) <= radiusSquared
        }.minByOrNull { it.first.getCenterDistanceSquared() } ?: return

        val (pos, state) = blockToProcess

        val rt = raytraceBlock(
            player.eyes, pos, state, range = range.toDouble(), wallsRange = wallRange.toDouble()
        )

        // We got a free angle at the block? Cool.
        if (rt != null) {
            val (rotation, _) = rt
            RotationManager.aimAt(rotation, openInventory = ignoreOpenInventory, configurable = rotations)

            this.currentTarget = DestroyerTarget(pos, this.action)
            return
        }

        val raytraceResult = mc.world?.raycast(
            RaycastContext(
                player.eyes,
                Vec3d.of(pos).add(0.5, 0.5, 0.5),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
            )
        ) ?: return

        // Failsafe. Should not trigger
        if (raytraceResult.type != HitResult.Type.BLOCK) return

        this.currentTarget = DestroyerTarget(raytraceResult.blockPos, DestroyAction.DESTROY)
    }

    data class DestroyerTarget(val pos: BlockPos, val action: DestroyAction)

    enum class DestroyAction(override val choiceName: String) : NamedChoice {
        DESTROY("Destroy"), USE("Use")
    }
}
