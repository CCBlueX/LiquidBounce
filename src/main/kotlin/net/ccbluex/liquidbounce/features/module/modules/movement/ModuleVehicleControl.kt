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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.warning
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.direction
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.math.copy
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.util.Hand

/**
 * Vehicle control module
 *
 * Move with your vehicle however you want.
 */
object ModuleVehicleControl : ClientModule("VehicleControl", Category.MOVEMENT, aliases = arrayOf("BoatFly")) {

    init {
        enableLock()
    }

    object BaseSpeed : Configurable("BaseSpeed") {
        val horizontalSpeed by float("Horizontal", 0.5f, 0.1f..10f)
        val verticalSpeed by float("Vertical", 0.35f, 0.1f..10f)
    }

    object SprintSpeed : ToggleableConfigurable(this, "SprintSpeed", true) {
        val horizontalSpeed by float("Horizontal", 5f, 0.1f..10f)
        val verticalSpeed by float("Vertical", 2f, 0.1f..10f)
    }

    private val glide by float("Glide", -0.15f, -0.3f..0.3f)

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
        val vehicle = player.controllingVehicle ?: run {
            wasInVehicle = false
            return@tickHandler
        }

        // Show explanation message
        if (!wasInVehicle && mc.options.useKey.isPressed) {
            wasInVehicle = true
            chat(warning(message("quitHelp")))
        }

        val useSprintSpeed = mc.options.sprintKey.isPressed && SprintSpeed.enabled
        val hSpeed =
            if (useSprintSpeed) SprintSpeed.horizontalSpeed else BaseSpeed.horizontalSpeed
        val vSpeed =
            if (useSprintSpeed) SprintSpeed.verticalSpeed else BaseSpeed.verticalSpeed

        // Control vehicle
        val horizontalSpeed = if (player.moving) hSpeed.toDouble() else 0.0
        val verticalSpeed = when {
            mc.options.jumpKey.isPressed -> vSpeed.toDouble()
            mc.options.sneakKey.isPressed -> -vSpeed.toDouble()
            // If we do not stop the vehicle from going down when touching water, it will
            // drown in water and cannot be controlled anymore
            !vehicle.isTouchingWater -> glide.toDouble()
            else -> 0.0
        }

        // Vehicle control velocity
        vehicle.velocity = vehicle.velocity
            .copy(y = verticalSpeed)
            .withStrafe(yaw = player.direction, speed = horizontalSpeed)
    }

    @Suppress("unused")
    private val handleMovementInputEvent = handler<MovementInputEvent> { event ->
        if (player.controllingVehicle != null || Rehook.vehicleId >= 0) {
            val isVehicleSafe = player.controllingVehicle?.let { it.isOnGround || it.isTouchingWater } == true

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
    object Rehook : ToggleableConfigurable(this, "Rehook", false) {

        private var unhookAfter by int("UnhookAfter", 4, 1..10)
        private var hookAfter by int("HookAfter", 2, 1..10)

        internal var vehicleId = -1
        private var forceAttempt = false

        @Suppress("unused")
        private val handleRehooking = tickHandler {
            if (vehicleId >= 0 && !player.hasVehicle()) {
                val vehicle = world.getEntityById(vehicleId)

                if (vehicle != null && !vehicle.isRemoved) {
                    // Check if the player is able to reach the vehicle
                    if (vehicle.boxedDistanceTo(player) > player.entityInteractionRange) {
                        chat(warning(message("vehicleTooFar")))
                        vehicleId = -1
                        return@tickHandler
                    }

                    // Enter the vehicle again
                    if (!forceAttempt) {
                        interaction.interactEntity(player, vehicle, Hand.MAIN_HAND)
                        forceAttempt = true
                    } else {
                        // We are already in the vehicle on the server-side, but our client does not know that, so
                        // we force the client to enter the vehicle again
                        player.startRiding(vehicle, true)
                    }
                } else {
                    chat(warning(message("vehicleGone")))
                    vehicleId = -1
                }
            } else {
                forceAttempt = false

                waitTicks(unhookAfter)
                vehicleId = player.controllingVehicle?.id ?: return@tickHandler
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
                player.stopRiding()
                waitTicks(hookAfter - 1)
            }
        }

    }


}
