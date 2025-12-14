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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MouseScrollEvent
import net.ccbluex.liquidbounce.event.events.MouseScrollInHotbarEvent
import net.ccbluex.liquidbounce.event.events.PlayerInteractItemEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.input.isPressed
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.client.util.InputUtil
import net.minecraft.item.*
import net.minecraft.util.hit.BlockHitResult

/**
 * AirPlace module
 *
 *  Allows you to place blocks in mid-air.
 */
object ModuleAirPlace : ClientModule("AirPlace", Category.WORLD) {

    object Preview : ToggleableConfigurable(this, "Preview", true) {
        val outlineOnly by boolean("OutlineOnly", false)
        val fillColor by color("Color", Color4b(69, 119, 255, 104))
        val outlineColor by color("OutlineColor", Color4b.WHITE)
    }

    val liquidPlace by boolean("Place in Liquids", false)

    object CustomRange : ToggleableConfigurable(this, "CustomRange", false) {
        private val rangeBounds = 1.0f..4.5f
        val range = float("Range", 3.0f, rangeBounds)

        object ScrollAdjust : ToggleableConfigurable(this, "ScrollAdjust", true) {
            val modifierKey by key("Modifier", InputUtil.GLFW_KEY_LEFT_ALT)
            val sensitivity by float("Sensitivity", 0.5f, 0.1f..1.0f)

            @Suppress("unused")
            private val rangeChangeHandler = handler<MouseScrollEvent> { event ->
                if (!running) return@handler
                if (modifierKey != InputUtil.UNKNOWN_KEY && !modifierKey.isPressed) return@handler
                val delta = event.vertical.toFloat() * sensitivity
                val newValue = range.get() + delta
                range.set(newValue.coerceIn(rangeBounds))
            }

            @Suppress("unused")
            private val hotbarScrollHandler = handler<MouseScrollInHotbarEvent> {
                if (running && (modifierKey == InputUtil.UNKNOWN_KEY || modifierKey.isPressed)) {
                    it.cancelEvent()
                }
            }
        }

        init {
            tree(ScrollAdjust)
        }

    }

    init {
        treeAll(Preview, CustomRange)
    }

    private inline val BlockHitResult.isAirOrFluid: Boolean
        get() = world.getBlockState(blockPos).isAir ||
            (liquidPlace && !world.getFluidState(blockPos).isEmpty && !ModuleLiquidPlace.running)


    private fun ItemStack.isAirPlaceableAt(hit: BlockHitResult): Boolean {
        if (isEmpty || isConsumable) return false
        return when (val i = item) {
            is BlockItem -> i.block.defaultState.canPlaceAt(world, hit.blockPos)
            is SpawnEggItem, is ArmorStandItem, is FireworkRocketItem -> true
            else -> false
        }
    }

    private fun canPlayerPlaceAt(hit: BlockHitResult): Boolean {
        val main = player.mainHandStack
        if (main.isAirPlaceableAt(hit)) return true

        val off = player.offHandStack
        return off.isAirPlaceableAt(hit)
    }


    private fun getValidHitResult(): BlockHitResult? {
        val hitResult = mc.crosshairTarget as? BlockHitResult ?: return null
        if (player.isSpectator) return null
        if (!hitResult.isAirOrFluid) return null
        if (!canPlayerPlaceAt(hitResult)) return null


        if (CustomRange.running) {
            val distance = CustomRange.range.get().toDouble()
            val playerEye = player.eyePos
            val direction = hitResult.pos.subtract(playerEye).normalize()
            val targetPos = playerEye.add(direction.multiply(distance))

            val newHitResult = BlockHitResult(
                targetPos,
                hitResult.side,
                targetPos.toBlockPos(),
                hitResult.isInsideBlock
            )

            if (!newHitResult.isAirOrFluid) return null
            if (!canPlayerPlaceAt(newHitResult)) return null

            return newHitResult
        }

        return hitResult
    }


    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (!Preview.running) return@handler
        val hitResult = getValidHitResult() ?: return@handler

        renderEnvironmentForWorld(event.matrixStack) {
            withPositionRelativeToCamera(hitResult.blockPos) {
                drawBox(
                    FULL_BOX,
                    if (Preview.outlineOnly) Color4b.TRANSPARENT else Preview.fillColor,
                    Preview.outlineColor
                )
            }
        }
    }

    @Suppress("unused")
    private val placeHandler = handler<PlayerInteractItemEvent> { event ->
        val hitResult = getValidHitResult() ?: return@handler

        val actionResult = interaction.interactBlock(player, event.hand, hitResult)
        if (actionResult.isAccepted) player.swingHand(event.hand)
        event.cancelEvent()
    }
}
