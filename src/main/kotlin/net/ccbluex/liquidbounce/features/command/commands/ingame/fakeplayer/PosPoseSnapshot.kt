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
package net.ccbluex.liquidbounce.features.command.commands.ingame.fakeplayer

import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EntityEquipment
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Inventory

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
    val pose: Pose,
    val preferredHand: InteractionHand,
    val inventory: Inventory,
    val limbPos: Float
)

fun fromPlayer(entity: AbstractClientPlayer): PosPoseSnapshot {
    return PosPoseSnapshot(
        entity.x,
        entity.y,
        entity.z,
        entity.xo,
        entity.yo,
        entity.zo,
        entity.swinging,
        entity.swingTime,
        entity.attackAnim,
        entity.yRot,
        entity.yRotO,
        entity.xRot,
        entity.xRotO,
        entity.yBodyRot,
        entity.yBodyRotO,
        entity.yHeadRot,
        entity.yHeadRotO,
        entity.pose,
        entity.swingingArm ?: InteractionHand.MAIN_HAND,
        entity.inventory,
        entity.walkAnimation.position
    )
}

fun fromPlayerMotion(entity: AbstractClientPlayer): PosPoseSnapshot {
    val playerInventory = Inventory(entity, EntityEquipment())
    playerInventory.replaceWith(entity.inventory)
    return PosPoseSnapshot(
        entity.x,
        entity.y,
        entity.z,
        entity.xo,
        entity.yo,
        entity.zo,
        entity.swinging,
        entity.swingTime,
        entity.attackAnim,
        entity.yRot,
        entity.yRotO,
        entity.xRot,
        entity.xRotO,
        entity.yBodyRot,
        entity.yBodyRotO,
        entity.yHeadRot,
        entity.yHeadRotO,
        entity.pose,
        entity.swingingArm,
        playerInventory,
        entity.walkAnimation.position
    )
}
