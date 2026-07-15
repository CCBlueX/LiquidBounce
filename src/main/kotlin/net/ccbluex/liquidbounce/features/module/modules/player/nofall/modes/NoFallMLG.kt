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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.additions.realSelectedSlot
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.interactBlock
import net.ccbluex.liquidbounce.utils.entity.useItem
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPoint
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

internal object NoFallMLG : NoFallMode("MLG") {

    private val rotations = tree(RotationsValueGroup(this))
    private val fallDistance by float("FallDistance", 3f, 0f..15f)
    private val pickupWater = tree(object : ToggleableValueGroup(this, "PickupWater", true) {})
    private var savedSlot: Int = -1

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (player.fallDistance < fallDistance || player.deltaMovement.y >= -0.08)
            return@tickHandler

        val landing = predictLanding() ?: return@tickHandler
        if (landing.ticks < 2) return@tickHandler

        val slot = findWaterBucketSlot()
        if (slot < 0) return@tickHandler

        val offhand = slot == 9
        savedSlot = player.inventory.realSelectedSlot
        if (!offhand) SilentHotbar.selectSlotSilently(this, slot, 4)

        val hand = if (offhand) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
        RotationManager.setRotationTarget(
            rotations.toRotationTarget(Rotation.lookingAt(landing.pos, player.eyePosition)),
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
            provider = ModuleNoFall
        )

        // Wait until ground is within interaction range (~2.5 blocks from feet)
        val groundY = ((landing.pos.y - 0.001).toInt() + 1.0)
        while (player.position().y - groundY > 2.5) {
            waitTicks(1)
        }

        val rot = RotationManager.serverRotation
        val hit = traceFromPoint(start = player.eyePosition, direction = rot.directionVector) as BlockHitResult

        if (hit.direction == Direction.UP) {
            interactBlock(hit, hand)
            useItem(hand)
        }

        if (pickupWater.enabled) {
            var timeout = 100
            while (!player.isInWater && timeout-- > 0) {
                waitTicks(1)
            }
            val ph = traceFromPlayer(rot) as BlockHitResult
            if (ph.direction == Direction.UP
                && player.getItemInHand(hand).`is`(Items.BUCKET)
            ) {
                interactBlock(ph, hand)
                useItem(hand)
            }
        }

        restoreSlot()
    }

    private data class Landing(val pos: Vec3, val ticks: Int)

    private fun predictLanding(): Landing? {
        var pos = player.position()
        var vel = player.deltaMovement
        for (t in 0..5) {
            vel = vel.add(0.0, -0.08, 0.0).multiply(1.0, 0.98, 1.0)
            pos = pos.add(vel)
            val blockUnder = BlockPos(pos.x.toInt(), (pos.y - 0.001).toInt(), pos.z.toInt())
            if (!world.getBlockState(blockUnder).isAir) {
                return Landing(pos, t)
            }
        }
        return null
    }

    private fun findWaterBucketSlot(): Int {
        for (i in 0..8) {
            if (player.inventory.getItem(i).`is`(Items.WATER_BUCKET)) return i
        }
        return if (player.offhandItem.`is`(Items.WATER_BUCKET)) 9 else -1
    }

    private fun restoreSlot() {
        if (savedSlot >= 0) {
            SilentHotbar.resetSlot(this)
            savedSlot = -1
        }
    }

    override fun disable() = restoreSlot()
}
