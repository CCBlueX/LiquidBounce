/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.strict
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.potion.Potion
import net.minecraft.util.MovementInput
import kotlin.math.abs

object Sprint : Module("Sprint", ModuleCategory.MOVEMENT, gameDetecting = false) {
    val mode by ListValue("Mode", arrayOf("Legit", "Vanilla"), "Vanilla")

    val allDirections by BoolValue("AllDirections", true) { mode == "Vanilla" }

    val jumpDirections by BoolValue("JumpDirections", false) { mode == "Vanilla" && allDirections }

    private val allDirectionsLimitSpeed by FloatValue(
        "AllDirectionsLimitSpeed",
        1f,
        0.75f..1f
    ) { mode == "Vanilla" && allDirections }

    private val allDirectionsLimitSpeedGround by BoolValue(
        "AllDirectionsLimitSpeedOnlyGround",
        true
    ) { mode == "Vanilla" && allDirections }

    private val blindness by BoolValue("Blindness", true) { mode == "Vanilla" }

    private val usingItem by BoolValue("UsingItem", false) { mode == "Vanilla" }

    private val food by BoolValue("Food", true) { mode == "Vanilla" }

    private val checkServerSide by BoolValue("CheckServerSide", false) { mode == "Vanilla" }

    private val checkServerSideGround by BoolValue(
        "CheckServerSideOnlyGround",
        false
    ) { mode == "Vanilla" && checkServerSide }

    private val inventory by BoolValue("Inventory", false) { mode == "Vanilla" }

    override val tag
        get() = mode

    fun correctSprintState(movementInput: MovementInput, isUsingItem: Boolean) {
        val player = mc.thePlayer ?: return

        if (Scaffold.handleEvents()) {
            if (!Scaffold.sprint) {
                player.isSprinting = false
                return
            } else if (Scaffold.sprint && Scaffold.eagle == "Normal" && isMoving && player.onGround && Scaffold.eagleSneaking && Scaffold.eagleSprint) {
                player.isSprinting = true
                return
            }
        }

        if (handleEvents()) {
            player.isSprinting = !shouldStopSprinting(movementInput, isUsingItem)

            if (player.isSprinting && allDirections && mode != "Legit") {
                if (!allDirectionsLimitSpeedGround || mc.thePlayer.onGround) {
                    mc.thePlayer.motionX *= allDirectionsLimitSpeed
                    mc.thePlayer.motionZ *= allDirectionsLimitSpeed
                }
            }
        }
    }

    private fun shouldStopSprinting(movementInput: MovementInput, isUsingItem: Boolean): Boolean {
        val player = mc.thePlayer ?: return false

        val isLegitModeActive = mode == "Legit"

        val modifiedForward = if (currentRotation != null && strict) {
            player.movementInput.moveForward
        } else {
            movementInput.moveForward
        }

        if (!isMoving) {
            return true
        }

        if (player.isCollidedHorizontally) {
            return true
        }

        if ((blindness || isLegitModeActive) && player.isPotionActive(Potion.blindness)) {
            return true
        }

        if ((food || isLegitModeActive) && !(player.foodStats.foodLevel > 6f || player.capabilities.allowFlying)) {
            return true
        }

        if ((usingItem || isLegitModeActive) && !NoSlow.handleEvents() && isUsingItem) {
            return true
        }

        if ((inventory || isLegitModeActive) && serverOpenInventory) {
            return true
        }

        if (isLegitModeActive) {
            return modifiedForward < 0.8
        }

        if (allDirections) {
            return false
        }

        val threshold = if ((!usingItem || NoSlow.handleEvents()) && isUsingItem) 0.2 else 0.8
        val playerForwardInput = player.movementInput.moveForward

        if (!checkServerSide) {
            return if (currentRotation != null) {
                abs(playerForwardInput) < threshold || playerForwardInput < 0 && modifiedForward < threshold
            } else {
                playerForwardInput < threshold
            }
        }

        if (checkServerSideGround && !player.onGround) {
            return currentRotation == null && modifiedForward < threshold
        }

        return modifiedForward < threshold
    }
}
