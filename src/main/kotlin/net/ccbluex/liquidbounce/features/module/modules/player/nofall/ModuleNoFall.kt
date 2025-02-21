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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.*
import net.minecraft.entity.EntityPose
import net.minecraft.item.Items

/**
 * NoFall module
 *
 * Protects you from taking fall damage.
 */

object ModuleNoFall : ClientModule("NoFall", Category.PLAYER) {

    internal val modes = choices(
        "Mode", NoFallSpoofGround, arrayOf(
            NoFallSpoofGround,
            NoFallNoGround,
            NoFallPacket,
            NoFallPacketJump,
            NoFallMLG,
            NoFallRettungsplatform,
            NoFallSpartan524Flag,
            NoFallVulcan,
            NoFallVulcanTP,
            NoFallVerus,
            NoFallForceJump,
            NoFallCancel,
            NoFallBlink,
            NoFallHypixelPacket,
            NoFallHypixel,
        )
    ).apply(::tagBy)

    private var notWhileGliding by boolean("NotWhileGliding", true)
    private var notWithMace by boolean("NotWithMace", true)

    override val running: Boolean
        get() {
            if (!super.running) {
                return false
            }

            // In creative mode, we don't need to reduce fall damage
            if (player.isCreative || player.isSpectator) {
                return false
            }

            // Check if we are invulnerable or flying
            if (player.abilities.invulnerable || player.abilities.flying) {
                return false
            }

            // With Elytra - we don't want to reduce fall damage.
            if (notWhileGliding && player.isGliding && player.isInPose(EntityPose.GLIDING)) {
                return false
            }

            // Check if we are holding a mace
            if (notWithMace && player.mainHandStack.item == Items.MACE) {
                return false
            }

            return true
        }

}
