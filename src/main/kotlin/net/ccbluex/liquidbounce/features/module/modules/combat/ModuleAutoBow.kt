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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ListenableConfigurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatableSequence
import net.minecraft.item.BowItem
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * AutoBow module
 *
 * Automatically shoots with your bow when it's fully charged
 *  + and make it possible to shoot faster
 */
object ModuleAutoBow : Module("AutoBow", Category.COMBAT) {

    /**
     * Automatically shoots with your bow when you aim correctly at an enemy or when the bow is fully charged.
     */
    private object AutoShootOptions : ListenableConfigurable(this, "AutoShoot", true) {

        val charged by int("Charged", 20, 3..20)

        val tickRepeatable = repeatableSequence(module!!) {
            val currentItem = player.activeItem

            // Should check if player is using bow
            if (currentItem?.item is BowItem) {
                // Wait until bow is fully charged
                waitUntil { player.itemUseTime >= charged }

                // Send stop using item to server
                network.sendPacket(
                    PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN,
                        Direction.DOWN
                    )
                )
                // Stop using item client-side
                player.stopUsingItem()
            }
        }
    }

    /**
     * Bow aimbot automatically aims at enemy targets
     */
    private object BowAimbotOptions : ListenableConfigurable(this, "BowAimbot", false) {

        val tickRepeatable = repeatableSequence(module!!) {

        }

    }

    /**
     * @desc Fast charge options (like FastBow) can be used to charge the bow faster.
     * @warning Should only be used on vanilla minecraft. Most anti cheats patch these kinds of exploits
     *
     * TODO: Add version specific options
     */
    private object FastChargeOptions : ListenableConfigurable(this, "FastCharge", true) {

        val packets by int("Packets", 20, 3..20)

        val tickRepeatable = repeatableSequence(module!!) {
            val currentItem = player.activeItem

            // Should accelerated game ticks when using bow
            if (currentItem?.item is BowItem) {
                repeat(packets) {
                    // Send movement packet to simulate ticks (has been patched in 1.19)
                    network.sendPacket(PlayerMoveC2SPacket(true))
                    // Just show visual effect (not required to work - but looks better)
                    player.tickActiveItemStack()
                }

                // Shoot with bow (auto shoot has to be enabled)
                // TODO: Depend on Auto Shoot
            }
        }

    }

    init {
        tree(AutoShootOptions)
        tree(BowAimbotOptions)
        tree(FastChargeOptions)
    }

}

