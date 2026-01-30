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
package net.ccbluex.liquidbounce.features.module.modules.player

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.tickConditional
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWindCharge.Rotate.rotations
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.input.isPressed
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.world.item.Items

/**
 * Uses wind charges to boost yourself up when holding jump.
 */
object ModuleAutoWindCharge : ClientModule("AutoWindCharge", ModuleCategories.PLAYER) {

    private object Rotate : ToggleableValueGroup(this, "Rotate", true) {
        val rotations = tree(RotationsValueGroup(this))
    }

    private object HorizontalBoost : ToggleableValueGroup(this, "HorizontalBoost", true) {
        val pitch by float("Pitch", 70f, 0f..90f)
        val boostKey by key("Key", InputConstants.KEY_LCONTROL)
    }

    init {
        treeAll(HorizontalBoost, Rotate)
    }

    private val combatPauseTime by int("CombatPauseTime", 0, 0..40, "ticks")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..40, "ticks")

    /**
     * 7 ticks is the perfect time to use a wind charge before hitting the ground,
     * and drastically boosts us higher.
     */
    const val PREDICTION_TICKS = 7

    @Suppress("unused")
    private val autoWindChargeHandler = tickHandler {
        if (player.isFallFlying || player.isSwimming || player.isInLiquid || !mc.options.keyJump.isDown) {
            return@tickHandler
        }

        val collision = FallingPlayer
            .fromPlayer(player)
            .findCollision(PREDICTION_TICKS) ?: return@tickHandler

        val itemSlot = Slots.OffhandWithHotbar.findSlot(Items.WIND_CHARGE) ?: return@tickHandler

        val isHorizontalBoost = HorizontalBoost.enabled && HorizontalBoost.boostKey.isPressed
        val directionYaw = getMovementDirectionOfInput(player.yRot,
            DirectionalInput(player.input)) - 180f
        val directionPitch = when {
            isHorizontalBoost -> HorizontalBoost.pitch
            else -> 90f
        }

        var rotation = Rotation(directionYaw, 80f)

        if (Rotate.enabled) {
            fun isRotationSufficient(): Boolean {
                return RotationManager.serverRotation.angleTo(rotation) <= 1.0f
            }

            tickConditional(20) {
                CombatManager.pauseCombatForAtLeast(combatPauseTime)
                RotationManager.setRotationTarget(
                    rotations.toRotationTarget(rotation),
                    Priority.IMPORTANT_FOR_USAGE_3,
                    this@ModuleAutoWindCharge
                )

                isRotationSufficient()
            }

            if (!isRotationSufficient()) {
                return@tickHandler
            }
        }

        val (yaw, pitch) = rotation.normalize()
        useHotbarSlotOrOffhand(itemSlot, slotResetDelay.random(), yaw, pitch)
    }

}
