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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.send1_21_5StartSneaking
import net.ccbluex.liquidbounce.utils.client.warning
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.world.InteractionHand

/**
 * Vehicle control module
 *
 * Move with your vehicle however you want.
 */
object ModuleVehicleControl : ClientModule("VehicleControl", ModuleCategories.MOVEMENT, aliases = listOf("BoatFly")) {

    private object BaseSpeed : ValueGroup("BaseSpeed") {
        val horizontalSpeed by float("Horizontal", 0.5f, 0.1f..10f)
        val verticalSpeed by float("Vertical", 0.35f, 0.1f..10f)
    }

    private object SprintSpeed : ToggleableValueGroup(this, "SprintSpeed", true) {
        val horizontalSpeed by float("Horizontal", 5f, 0.1f..10f)
        val verticalSpeed by float("Vertical", 2f, 0.1f..10f)
    }

    private val glide by float("Glide", -0.15f, -0.3f..0.3f)

    private val mouseControl by boolean("MouseControl", false)
    private val noGlideOnSprint by boolean("NoGlideOnSpring", false)

    init {
        tree(BaseSpeed)
        tree(SprintSpeed)
        tree(Rehook)
    }

    private var wasInVehicle = false

    override fun onEnabled() {
        chat(warning(message("quitHelp")))
        super.onEnabled()
    }

    @Suppress("unused")
    private val handleVehicleMovement = tickHandler {
        val vehicle = player.controlledVehicle ?: run {
            wasInVehicle = false
            return@tickHandler
        }

        // Show explanation message
        if (!wasInVehicle && mc.options.keyUse.isDown) {
            wasInVehicle = true
            chat(warning(message("quitHelp")))
        }

        val useSprintSpeed = mc.options.keySprint.isDown && SprintSpeed.enabled
        val hSpeed =
            if (useSprintSpeed) SprintSpeed.horizontalSpeed else BaseSpeed.horizontalSpeed
        val vSpeed =
            if (useSprintSpeed) SprintSpeed.verticalSpeed else BaseSpeed.verticalSpeed

        // Control vehicle
        val horizontalSpeed = if (player.moving) hSpeed.toDouble() else 0.0

        if (mouseControl) {
            vehicle.yRot = player.yRot
            vehicle.yRotO = player.yRot
        }

        val verticalSpeed = when {
            mc.options.keyJump.isDown -> vSpeed.toDouble()
            mc.options.keyShift.isDown -> -vSpeed.toDouble()
            // If we do not stop the vehicle from going down when touching water, it will
            // drown in water and cannot be controlled anymore
            !vehicle.isInWater &&
                !(useSprintSpeed && noGlideOnSprint) // No glide option
                     -> glide.toDouble()
            else -> 0.0
        }

        // Vehicle control velocity
        val input = DirectionalInput(player.input)
        val movementYaw = getMovementDirectionOfInput(vehicle.yRot, input)
        vehicle.deltaMovement = vehicle.deltaMovement
            .copy(y = verticalSpeed)
            .withStrafe(yaw = movementYaw, speed = horizontalSpeed)
    }

    @Suppress("unused")
    private val handleMovementInputEvent = handler<MovementInputEvent> { event ->
        if (player.controlledVehicle != null || Rehook.vehicleId >= 0) {
            val isVehicleSafe = player.controlledVehicle?.let { it.onGround() || it.isInWater } == true

            // Do not quit vehicle if not safe to do so
            event.sneak = event.sneak && isVehicleSafe

            if (event.sneak) {
                Rehook.vehicleId = -1
            }
        }
    }

    /**
     * Bypasses BoatFly checks on anti-cheats such as Vulcan 2.9.1
     */
    object Rehook : ToggleableValueGroup(this, "Rehook", false) {

        private var unhookAfter by int("UnhookAfter", 4, 1..10)
        private var hookAfter by int("HookAfter", 2, 1..10)

        internal var vehicleId = -1
        private var forceAttempt = false

        @Suppress("unused")
        private val handleRehooking = tickHandler {
            if (vehicleId >= 0 && !player.isPassenger) {
                val vehicle = world.getEntity(vehicleId)

                if (vehicle != null && !vehicle.isRemoved) {
                    // Check if the player is able to reach the vehicle
                    if (vehicle.boxedDistanceTo(player) > player.entityInteractionRange()) {
                        chat(warning(message("vehicleTooFar")))
                        vehicleId = -1
                        return@tickHandler
                    }

                    // Enter the vehicle again
                    if (!forceAttempt) {
                        interaction.interact(player, vehicle, InteractionHand.MAIN_HAND)
                        forceAttempt = true
                    } else {
                        // We are already in the vehicle on the server-side, but our client does not know that, so
                        // we force the client to enter the vehicle again
                        player.startRiding(vehicle, true, true)
                    }
                } else {
                    chat(warning(message("vehicleGone")))
                    vehicleId = -1
                }
            } else {
                forceAttempt = false

                waitTicks(unhookAfter)
                vehicleId = player.controlledVehicle?.id ?: return@tickHandler
                network.send1_21_5StartSneaking()
                player.stopRiding()
                waitTicks(hookAfter - 1)
            }
        }

    }


}
