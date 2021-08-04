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

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleScaffold.updateTarget
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.block.Blocks
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult

/**
 * Ignite module
 *
 * Automatically sets targets around you on fire.
 */
object ModuleIgnite : Module("Ignite", Category.WORLD) {

    var delay by int("Delay", 20, 0..400)

    // Target
    private val targetTracker = tree(TargetTracker())

    val networkTickHandler = repeatable { event ->
        val player = mc.player ?: return@repeatable

        val slot = findHotbarSlot(Items.LAVA_BUCKET) ?: return@repeatable

        for (enemy in targetTracker.enemies()) {
            if (enemy.squaredBoxedDistanceTo(player) > 6.0 * 6.0) {
                continue
            }

            val pos = enemy.blockPos

            val state = pos.getState()

            if (state?.block == Blocks.LAVA) {
                continue
            }

            val currentTarget = updateTarget(pos, true) ?: continue

            val rotation = currentTarget.rotation.fixedSensitivity() ?: continue
            val rayTraceResult = raycast(4.5, rotation) ?: return@repeatable

            if (rayTraceResult.type != HitResult.Type.BLOCK) {
                continue
            }

            player.networkHandler.sendPacket(PlayerMoveC2SPacket.LookAndOnGround(rotation.yaw, rotation.pitch, player.isOnGround))

            if (slot != player.inventory.selectedSlot) {
                player.networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(slot))
            }

            player.networkHandler.sendPacket(PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, rayTraceResult))
            val itemUsageContext = ItemUsageContext(player, Hand.MAIN_HAND, rayTraceResult)

            val itemStack = player.inventory.getStack(slot)

            val actionResult: ActionResult

            if (player.isCreative) {
                val i = itemStack.count
                actionResult = itemStack.useOnBlock(itemUsageContext)
                itemStack.count = i
            } else {
                actionResult = itemStack.useOnBlock(itemUsageContext)
            }

            if (actionResult.shouldSwingHand()) {
                player.swingHand(Hand.MAIN_HAND)
            }

            if (slot != player.inventory.selectedSlot) {
                player.networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
            }

            break
        }

    }
}
