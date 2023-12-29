/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.PlayerInteractedItem
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.item.Hotbar
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.hud.InGameHud
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.UseAction
import kotlin.math.max

/**
 * AutoClicker module
 *
 * Clicks automatically when holding down a mouse button.
 */

object ModuleSmartEat : Module("SmartEat", Category.PLAYER) {

    private val food: Pair<Int, ItemStack>?
        get() = Hotbar.findBestItem(0) { _, itemStack ->
            val foodComp =
                itemStack.item.foodComponent ?: return@findBestItem -1

            if(foodComp.statusEffects.any {it.first == StatusEffects.HUNGER})
                return@findBestItem 0
            foodComp.hunger
        }
    private object SilentOffhand : ToggleableConfigurable(this, "SilentOffhand", true) {
        private val swapBackDelay by int("SwapBackDelay", 5, 1..20)
        private object RenderSlot : ToggleableConfigurable(this.module, "RenderSlot", true) {
            val renderHandler = handler<OverlayRenderEvent> {
                renderEnvironmentForGUI {
                    val currentFood = food ?: return@renderEnvironmentForGUI
                    val dc = DrawContext(mc, mc.bufferBuilders.entityVertexConsumers)
                    dc.drawItemInSlot(mc.textRenderer, currentFood.second, 100, 100)

                }
            }
        }

        val InteractionHandler = handler<PlayerInteractedItem> { event ->
            if(!enabled)
                return@handler
            if(event.actionResult != ActionResult.PASS)
                return@handler

            val currentFood = food ?: return@handler

            SilentHotbar.selectSlotSilently(this@SilentOffhand, currentFood.first, max(swapBackDelay, 5))
        }

        val tickHandler = repeatable {
            if(player.activeItem.useAction != UseAction.EAT)
                return@repeatable
            if(!SilentHotbar.isSlotModified(this@SilentOffhand))
                return@repeatable
            // if we are already eating, we want to keep the silent slot
            SilentHotbar.selectSlotSilently(this@SilentOffhand, SilentHotbar.serversideSlot, swapBackDelay)


        }

        init {
            tree(RenderSlot)
        }
    }

    init {
        tree(SilentOffhand)
    }


}
