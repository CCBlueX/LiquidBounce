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

import net.ccbluex.liquidbounce.config.types.NamedChoice
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
            NoFallBlocksMC
        )
    ).apply(::tagBy)

    private val notConditions by multiEnumChoice<NotConditions>("Not")

    override val running: Boolean
        get() = when {
            !super.running -> false

            // In creative mode, we don't need to reduce fall damage
            player.isCreative || player.isSpectator -> false

            // Check if we are invulnerable or flying
            player.abilities.invulnerable || player.abilities.flying -> false

            // Test other conditions
            else -> notConditions.none { it.testCondition() }
        }

    @Suppress("unused")
    private enum class NotConditions(
        override val choiceName: String,
        val testCondition: () -> Boolean
    ) : NamedChoice {
        /**
         * With Elytra - we don't want to reduce fall damage.
         */
        WHILE_GLIDING("WhileGliding", {
            player.isGliding && player.isInPose(EntityPose.GLIDING)
        }),

        /**
         * Check if we are holding a mace
         */
        WITH_MACE("WithMace", {
            player.mainHandStack.item == Items.MACE
        })
    }
}
