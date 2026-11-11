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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ClientScope
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlocksMC20250719
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallCancel
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallForceJump
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallGrim2371MC19
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallHypixel20240713
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallHypixel20240806
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallMLG
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallMount
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallNoGround
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallPacket
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallPacketJump
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallRettungsplatform
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallSpartanPhase524
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallSpoofGround
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallSpoofLanding
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallVerus
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallVulcan
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallVulcan288
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.Items
import java.util.function.BooleanSupplier

/**
 * NoFall module
 *
 * Protects you from taking fall damage.
 */
object ModuleNoFall : ClientModule("NoFall", ModuleCategories.PLAYER) {

    override val clientScope get() = ClientScope.EVERY

    internal val modes = choices(
        "Mode", NoFallSpoofGround, arrayOf(
            NoFallSpoofGround,
            NoFallSpoofLanding,
            NoFallNoGround,
            NoFallPacket,
            NoFallPacketJump,
            NoFallMLG,
            NoFallMount,
            NoFallRettungsplatform,
            NoFallSpartanPhase524,
            NoFallVulcan,
            NoFallVulcan288,
            NoFallVerus,
            NoFallForceJump,
            NoFallCancel,
            NoFallBlink,
            NoFallHypixel20240806,
            NoFallHypixel20240713,
            NoFallBlocksMC20250719,
            NoFallGrim2371MC19
        )
    ).apply(::tagBy)

    private val notConditions by multiEnumChoice("Not", NotCondition.entries)

    override val running: Boolean
        get() = when {
            !super.running -> false

            // In creative mode, we don't need to reduce fall damage
            player.isCreative || player.isSpectator -> false

            // Check if we are invulnerable or flying
            player.abilities.invulnerable || player.abilities.flying -> false

            // Test other conditions
            else -> notConditions.none { it.asBoolean }
        }

    @Suppress("unused")
    private enum class NotCondition(
        override val tag: String,
    ) : Tagged, BooleanSupplier {
        /**
         * With Elytra - we don't want to reduce fall damage.
         */
        WHILE_GLIDING("WhileGliding") {
            override fun getAsBoolean() = player.isFallFlying && player.hasPose(Pose.FALL_FLYING)
        },

        /**
         * Check if we are holding a mace
         */
        WITH_MACE("WithMace") {
            override fun getAsBoolean() = player.mainHandItem.item == Items.MACE
        };
    }
}
