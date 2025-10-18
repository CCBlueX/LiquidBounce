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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerInteractItemEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.ConstantPositionExtrapolation
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.block.BlockRenderType
import net.minecraft.entity.EntityDimensions
import net.minecraft.item.Items
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

/**
 * EasyPearl module
 *
 * Throw pearl to where you are looking at.
 **/
@Suppress("MagicNumber")
object ModuleEasyPearl :
    ClientModule("EasyPearl", Category.MISC, aliases = listOf("PearlHelper", "PearlAssist", "PearlTP")) {
    private val aimOffThreshold by float("AimOffThreshold", 2f, 0.5f..10f)
    private val reachableCheck by boolean("ReachableCheck", true)
    private val rotation = tree(RotationsConfigurable(this))

    private var targetPosition: Vec3d? = null

    var currentTargetRotation : Rotation? = null
        private set
    private val enderPearlSlot: HotbarItemSlot?
        get() = Slots.OffhandWithHotbar.findSlot(Items.ENDER_PEARL)

    override fun onDisabled() {
        currentTargetRotation = null
        super.onDisabled()
    }

    /**
     * Handler throw pearl by player self.
     */
    @Suppress("unused")
    private val interactItemHandler = handler<PlayerInteractItemEvent> { event ->
        if (!isHoldingPearl() || !mc.options.useKey.isPressed) {
            return@handler
        }

        val hitResult = getPositionPlayerLookAt()
        // While reachable check is enabled, we will check if the player
        // is looking at a block father than pearl can reach
        if (reachableCheck && getTargetRotation(hitResult.pos) == null
            && hitResult.type != HitResult.Type.MISS) {
            chat(markAsError(message("noInReachWarning")))
            event.cancelEvent()
            targetPosition = null
            return@handler
        }
        targetPosition = hitResult.pos

        // check if we are rotating to the target position correctly
        if (isRotationDone(targetPosition!!)) {
            targetPosition = null
        } else {
            event.cancelEvent()
        }
    }

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        /**
         * handler for rotation update event,and rotate to the target rotation
         */
        val finalTargetRotation = getTargetRotation(targetPosition ?: return@handler) ?: return@handler

        RotationManager.setRotationTarget(
            rotation.toRotationTarget(finalTargetRotation),
            Priority.IMPORTANT_FOR_PLAYER_LIFE,
            this@ModuleEasyPearl,
        )
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        /**
         * handler for tick event,and check if we are rotating to the target rotation correctly,if yes,throw the pearl
         */
        currentTargetRotation = targetPosition?.let(::getTargetRotation) ?: return@handler

        if (isRotationDone(targetPosition ?: return@handler)) {
            useHotbarSlotOrOffhand(
                enderPearlSlot ?: return@handler,
                0,
                currentTargetRotation!!.yaw,
                currentTargetRotation!!.pitch,
            )
            targetPosition = null
            currentTargetRotation = null
        }
    }

    /**
     * handler for world render event,and render the target position
     */
    @Suppress("unused")
    private val worldRenderHandler = handler<WorldRenderEvent> { event ->
        if (!isHoldingPearl()) {
            return@handler
        }

        val matrixStack = event.matrixStack
        val pos = getPositionPlayerLookAt(event.partialTicks)?.pos ?: return@handler
        val blockPos = pos.toBlockPos()
        val state = blockPos.getState() ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            withDisabledCull {
                val color =
                    if (getTargetRotation(pos) != null) {
                        Color4b(0x20, 0xC2, 0x06)
                    } else {
                        Color4b(0xD7, 0x09, 0x09)
                    }

                val baseColor = color.with(a = 50)
                val transparentColor = baseColor.with(a = 0)
                val outlineColor = color.with(a = 200)

                withPositionRelativeToCamera(blockPos) {
                    if (state.renderType != BlockRenderType.MODEL && state.isAir) {
                        drawBoxSide(
                            FULL_BOX,
                            Direction.DOWN,
                            baseColor,
                            outlineColor,
                        )
                        drawGradientSides(0.7, baseColor, transparentColor, FULL_BOX)
                    } else {
                        drawBox(
                            FULL_BOX,
                            baseColor,
                            outlineColor,
                        )
                    }
                }
            }
        }
    }

    private fun isHoldingPearl() =
        player.mainHandStack.item == Items.ENDER_PEARL || player.offHandStack.item == Items.ENDER_PEARL

    /**
     * check if we are rotating to the target rotation correctly
     * @param targetPosition the target position
     * @return true if we are rotating to the target rotation correctly, false otherwise
     */
    @Suppress("ReturnCount")
    private fun isRotationDone(targetPosition: Vec3d): Boolean {
        return RotationManager.serverRotation.angleTo(
            getTargetRotation(targetPosition) ?: return true,
        ) <= aimOffThreshold
    }

    /**
     * get the position player look at
     * @return the position player look at
     */
    private fun getPositionPlayerLookAt(tickDelta: Float = 0f) =
        player.raycast(1000.0, tickDelta, false)

    /**
     * get the target rotation for the target position
     * @param targetPosition the target position
     * @return the target rotation for the target position
     */
    private fun getTargetRotation(targetPosition: Vec3d): Rotation? =
        SituationalProjectileAngleCalculator.calculateAngleFor(
            TrajectoryInfo.GENERIC,
            sourcePos = player.pos,
            targetPosFunction = ConstantPositionExtrapolation(targetPosition),
            targetShape = EntityDimensions.fixed(1.0F, 0.0F),
        )
}
