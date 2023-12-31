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
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.item.Hotbar
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.potion.PotionUtil
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.UseAction
import kotlin.math.max

/**
 * AutoClicker module
 *
 * Clicks automatically when holding down a mouse button.
 */

object ModuleSmartEat : Module("SmartEat", Category.PLAYER) {
    private val HOTBAR_OFFHAND_LEFT_TEXTURE = Identifier("hud/hotbar_offhand_left")




    private object SilentOffhand : ToggleableConfigurable(this, "SilentOffhand", true) {

        private val swapBackDelay by int("SwapBackDelay", 5, 1..20)

        private val preferGappleHealth by float("PreferGappleHealth", 7f, 0f..20f)
        private val preferNotchAppleHealth by float("PreferNotchAppleHealth", 7f, 0f..20f)
        private val preferHealthPotHealth by float("PreferHealthPotHealth", 7f, 0f..20f)

        private val food: Pair<Int, ItemStack>?
            get() = Hotbar.findBestItem(0) { _, itemStack ->
                val item = itemStack.item

                if (prefersHealthPot && item == Items.POTION) {
                    val hasHealthEffect =
                        PotionUtil.getPotionEffects(itemStack).any {
                            it.effectType == StatusEffects.INSTANT_HEALTH
                        }

                    if (hasHealthEffect)
                        return@findBestItem 100 - preferHealthPotHealth.toInt()
                }

                val foodComp = item.foodComponent ?: return@findBestItem -1

                val hasHungerEffect = foodComp.statusEffects.any { it.first.effectType == StatusEffects.HUNGER }
                if (hasHungerEffect)
                    return@findBestItem 0

                if(prefersGapples && item == Items.GOLDEN_APPLE)
                    return@findBestItem 100 - preferGappleHealth.toInt()

                if (prefersNotchApple && item == Items.ENCHANTED_GOLDEN_APPLE)
                    return@findBestItem 100 - preferNotchAppleHealth.toInt()

                foodComp.hunger
            }

        private var prefersGapples = false
        private var prefersNotchApple = false
        private var prefersHealthPot = false
        private object RenderSlot : ToggleableConfigurable(this.module, "RenderSlot", true) {
            private val offset by int("Offset", 26, 0..40)
            val renderHandler = handler<OverlayRenderEvent> {
                renderEnvironmentForGUI {
                    val currentFood = food ?: return@renderEnvironmentForGUI
                    val dc = DrawContext(mc, mc.bufferBuilders.entityVertexConsumers)
                    val scaledWidth = dc.scaledWindowWidth
                    val scaledHeight = dc.scaledWindowHeight
                    val i: Int = scaledWidth / 2
                    val x = i - 91 - 26 - offset
                    val y = scaledHeight - 16 - 3
                    dc.drawItemInSlot(mc.textRenderer, currentFood.second, x, y)
                    dc.drawItem(currentFood.second, x, y)
                    dc.drawGuiTexture(
                        HOTBAR_OFFHAND_LEFT_TEXTURE, i - 91 - 29 - offset,
                        scaledHeight - 23, 29, 24
                    )

                }
            }
        }

        val InteractionHandler = handler<PlayerInteractedItem> { event ->
            if(!enabled)
                return@handler
            if(event.actionResult != ActionResult.PASS)
                return@handler

            val currentFood = food ?: return@handler

            if(
                !player.canConsume(false)
                && currentFood.second.item.foodComponent?.isAlwaysEdible() == false
                ) {
                return@handler
            }


            SilentHotbar.selectSlotSilently(this@SilentOffhand, currentFood.first, max(swapBackDelay, 5))
        }

        val tickHandler = repeatable {
            prefersGapples = player.health <= preferGappleHealth
            prefersNotchApple = player.health <= preferNotchAppleHealth
            prefersHealthPot = player.health <= preferHealthPotHealth

            val useAction = player.activeItem.useAction

            if (useAction != UseAction.EAT && useAction != UseAction.DRINK)
                return@repeatable
            if (!SilentHotbar.isSlotModified(this@SilentOffhand))
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
