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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerInteractedItemEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.ccbluex.liquidbounce.utils.item.isFood
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.world.item.ArmorStandItem
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SpawnEggItem
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

/**
 * AirPlace module
 *
 *  Allows you to place blocks in mid-air.
 */
object ModuleAirPlace : ClientModule("AirPlace", ModuleCategories.WORLD) {

    private object Preview : ToggleableConfigurable(this, "Preview", true) {
        val outlineOnly by boolean("OutlineOnly", false)
        val fillColor by color("Color", Color4b(69, 119, 255, 104))
        val outlineColor by color("OutlineColor", Color4b.WHITE)
    }

    init {
        tree(Preview)
    }


    private inline val Vec3.isBlockAir: Boolean
        get() = world.getBlockState(toBlockPos()).isAir

    // ---------- Utils ----------
    private fun isAirPlaceableItem(stack: ItemStack): Boolean {
        if (stack.isEmpty || stack.isFood || stack.isConsumable) return false
        val item = stack.item
        return item is BlockItem || item is SpawnEggItem || item is ArmorStandItem
    }

    private fun playerHasAllowedItems(): Boolean {
        val mainHand = player.mainHandItem
        val offHand = player.offhandItem
        return isAirPlaceableItem(mainHand) || isAirPlaceableItem(offHand)
    }

    // ---------- Render ----------
    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (!Preview.running) return@handler

        val target = mc.hitResult ?: return@handler
        if (!target.location.isBlockAir) return@handler
        if (!playerHasAllowedItems()) return@handler

        val targetPos = target.location.toBlockPos()
        val worldSpaceBox = AABB(targetPos)

        val negCameraPos = mc.entityRenderDispatcher.camera?.position()?.reverse() ?: return@handler
        val viewSpaceBox = worldSpaceBox.move(negCameraPos)

        val fill = if (Preview.outlineOnly) Color4b.TRANSPARENT else Preview.fillColor
        val outline = Preview.outlineColor

        renderEnvironmentForWorld(event.matrixStack) {
            drawBox(viewSpaceBox, fill, outline)
        }
    }

    // ---------- Place ----------
    @Suppress("unused")
    private val placeHandler = handler<PlayerInteractedItemEvent> { event ->
        val target = mc.hitResult ?: return@handler
        if (!target.location.isBlockAir) return@handler
        if (!playerHasAllowedItems()) return@handler

        val hand = event.hand
        if (target !is BlockHitResult) return@handler
        val actionResult = interaction.useItemOn(player, hand, target)
        if (actionResult.consumesAction()) player.swing(hand)

    }

}
