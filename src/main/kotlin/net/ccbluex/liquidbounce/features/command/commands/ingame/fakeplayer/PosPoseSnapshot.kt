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
package net.ccbluex.liquidbounce.features.command.commands.ingame.fakeplayer

import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.entity.EntityEquipment
import net.minecraft.entity.EntityPose
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.Hand

@JvmRecord
data class PosPoseSnapshot(
    val x: Double,
    val y: Double,
    val z: Double,
    val lastX: Double,
    val lastY: Double,
    val lastZ: Double,
    val handSwinging: Boolean,
    val handSwingTicks: Int,
    val handSwingProgress: Float,
    val yaw: Float,
    val lastYaw: Float,
    val pitch: Float,
    val lastPitch: Float,
    val bodyYaw: Float,
    val lastBodyYaw: Float,
    val headYaw: Float,
    val lastHeadYaw: Float,
    val pose: EntityPose,
    val preferredHand: Hand,
    val inventory: PlayerInventory,
    val limbPos: Float
)

fun fromPlayer(entity: AbstractClientPlayerEntity): PosPoseSnapshot {
    return PosPoseSnapshot(
        entity.x,
        entity.y,
        entity.z,
        entity.x,
        entity.y,
        entity.z,
        entity.handSwinging,
        entity.handSwingTicks,
        entity.handSwingProgress,
        entity.yaw,
        entity.yaw,
        entity.pitch,
        entity.pitch,
        entity.bodyYaw,
        entity.bodyYaw,
        entity.headYaw,
        entity.headYaw,
        entity.pose,
        entity.preferredHand ?: Hand.MAIN_HAND,
        entity.inventory,
        entity.limbAnimator.animationProgress
    )
}

fun fromPlayerMotion(entity: AbstractClientPlayerEntity): PosPoseSnapshot {
    val playerInventory = PlayerInventory(null, EntityEquipment())
    playerInventory.clone(entity.inventory)
    return PosPoseSnapshot(
        entity.x,
        entity.y,
        entity.z,
        entity.lastX,
        entity.lastY,
        entity.lastZ,
        entity.handSwinging,
        entity.handSwingTicks,
        entity.handSwingProgress,
        entity.yaw,
        entity.lastYaw,
        entity.pitch,
        entity.lastPitch,
        entity.bodyYaw,
        entity.lastBodyYaw,
        entity.headYaw,
        entity.lastHeadYaw,
        entity.pose,
        entity.preferredHand ?: Hand.MAIN_HAND,
        playerInventory,
        entity.limbAnimator.animationProgress
    )
}
