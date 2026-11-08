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
import net.ccbluex.liquidbounce.event.events.KeyEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
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
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.world.item.Items

/**
 * Uses wind charges to boost yourself up when holding jump, or throws one on a key press.
 */
object ModuleAutoWindCharge : ClientModule("AutoWindCharge", ModuleCategories.PLAYER) {

    private object Rotate : ToggleableValueGroup(this, "Rotate", true) {
        val rotations = tree(RotationsValueGroup(this))
    }

    private object HorizontalBoost : ToggleableValueGroup(this, "HorizontalBoost", true) {
        val pitch by floatRange("Pitch", 70f..70f, 0f..90f)
        val boostKey by key("Key", InputConstants.KEY_LCONTROL)
    }

    init {
        treeAll(HorizontalBoost, Rotate)
    }

    /**
     * Throws one charge per press, without jumping and in the air as well.
     */
    private val throwKey by key("ThrowKey")

    private val combatPauseTime by int("CombatPauseTime", 0, 0..40, "ticks")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..40, "ticks")

    /**
     * How long we keep turning down, before jumping without a charge or giving up on a key throw.
     */
    private const val MAX_AIM_TICKS = 10

    private var holdJump = false
    private var aimTicks = 0
    private var horizontalPitch: Float? = null
    private var keyThrow = false

    override fun onDisabled() {
        holdJump = false
        aimTicks = 0
        horizontalPitch = null
        keyThrow = false
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (holdJump) {
            event.jump = false
        }
    }

    @Suppress("unused")
    private val keyHandler = handler<KeyEvent> { event ->
        if (event.action == InputConstants.PRESS && event.key == throwKey) {
            keyThrow = true
        }
    }

    /**
     * A charge thrown from the ground bursts two ticks later, as it spawns at eye height and flies 1.5 blocks a tick.
     * Jumping one tick after the throw puts our feet right above the burst while the jump still lifts us,
     * so jump and knockback add up.
     */
    @Suppress("unused")
    private val autoWindChargeHandler = tickHandler {
        holdJump = false

        val onKey = keyThrow
        keyThrow = false

        val triggered = onKey || mc.options.keyJump.isDown
        if (!triggered || player.isFallFlying || player.isSwimming || player.isInLiquid) {
            aimTicks = 0
            return@tickHandler
        }

        val itemSlot = Slots.OffhandWithHotbar.findSlot(Items.WIND_CHARGE) ?: return@tickHandler
        if (player.cooldowns.isOnCooldown(itemSlot.itemStack)) {
            return@tickHandler
        }

        val rotation = throwRotation()

        if (!onKey && !player.onGround()) {
            // Face down before landing, so the throw does not wait for the turn
            if (Rotate.enabled && isLandingWithin(rotations.calculateTicks(rotation) + 1)) {
                aim(rotation)
            }
            return@tickHandler
        }

        if (Rotate.enabled) {
            aim(rotation)

            if (RotationManager.serverRotation.directionAngleTo(rotation) > 1f) {
                val aiming = aimTicks++ < MAX_AIM_TICKS
                holdJump = aiming
                keyThrow = onKey && aiming
                return@tickHandler
            }
        }

        aimTicks = 0
        holdJump = true
        horizontalPitch = null

        val (yaw, pitch) = if (Rotate.enabled) RotationManager.serverRotation else rotation.normalize()
        useHotbarSlotOrOffhand(itemSlot, slotResetDelay.random(), yaw, pitch)
    }

    private fun throwRotation(): Rotation {
        if (!HorizontalBoost.enabled || !HorizontalBoost.boostKey.isPressed) {
            return Rotation(player.yRot, 90f)
        }

        val pitch = horizontalPitch ?: HorizontalBoost.pitch.random().also { horizontalPitch = it }
        return Rotation(player.getMovementDirectionOfInput() - 180f, pitch)
    }

    private fun isLandingWithin(ticks: Int) =
        FallingPlayer.fromPlayer(player, RotationManager.movementYaw).findCollision(ticks) != null

    private fun aim(rotation: Rotation) {
        CombatManager.pauseCombatForAtLeast(combatPauseTime)
        RotationManager.setRotationTarget(
            rotations.toRotationTarget(rotation),
            Priority.IMPORTANT_FOR_USAGE_3,
            this
        )
    }

}
