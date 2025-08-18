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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.PlayerInteractedItemEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.newDrawContext
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.foodComponent
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.MiningToolItem
import net.minecraft.item.consume.UseAction
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import kotlin.math.absoluteValue

/**
 * SmartEat module
 *
 * Makes it easier to eat
 */

object ModuleSmartEat : ClientModule("SmartEat", Category.PLAYER) {

    private val HOTBAR_OFFHAND_LEFT_TEXTURE = Identifier.of("hud/hotbar_offhand_left")

    private val swapBackDelay by int("SwapBackDelay", 5, 1..20)

    private val preferGappleHealth by float("PreferGappleHealthThreshold", 9f, 0f..20f)
    private val preferNotchAppleHealth by float("PreferNotchAppleHealthThreshold", 2f, 0f..20f)
    private val preferHealthPotHealth by float("PreferHealthPotHealthThreshold", 12f, 0f..20f)

    private val combatPauseTime by int("CombatPauseTime", 0, 0..40, "ticks")
    private val notDuringCombat by boolean("NotDuringCombat", false)

    private object Estimator {
        private val comparator = ComparatorChain<Pair<HotbarItemSlot, FoodEstimationData>>(
            // If there is an indication for a special item, we should use it. Items with lower health threshold
            // are preferred since their usage is probably more urgent.
            compareByDescending { it.second.healthThreshold },
            compareBy { it.second.restoredHunger },
            // Use the closest slot
            compareByDescending { (it.first.hotbarSlot - SilentHotbar.serversideSlot).absoluteValue },
            // Just for stabilization reasons
            compareBy { SilentHotbar.serversideSlot }
        )

        fun findBestFood(): HotbarItemSlot? {
            return Slots.Hotbar
                .mapNotNull { slot -> getFoodEstimationData(slot.itemStack)?.let { slot to it } }
                .maxWithOrNull(comparator)?.first
        }

        private class FoodEstimationData(val restoredHunger: Int = 0, val healthThreshold: Int = 20)

        private fun getFoodEstimationData(itemStack: ItemStack): FoodEstimationData? {
            val item = itemStack.item

            val prefersGapples = player.health <= preferGappleHealth
            val prefersNotchApple = player.health <= preferNotchAppleHealth
            val prefersHealthPot = player.health <= preferHealthPotHealth

            return when {
                prefersGapples && item == Items.POTION -> {
                    val hasHealthEffect =
                        itemStack.getPotionEffects().any {
                            it.effectType == StatusEffects.INSTANT_HEALTH
                        }

                    if (hasHealthEffect) {
                        FoodEstimationData(healthThreshold = preferHealthPotHealth.toInt())
                    } else {
                        null
                    }
                }
                prefersHealthPot && item == Items.GOLDEN_APPLE -> {
                    FoodEstimationData(
                        healthThreshold = preferHealthPotHealth.toInt(),
                        restoredHunger = itemStack.foodComponent!!.nutrition
                    )
                }
                prefersNotchApple && item == Items.ENCHANTED_GOLDEN_APPLE -> {
                    FoodEstimationData(
                        healthThreshold = preferNotchAppleHealth.toInt(),
                        restoredHunger = itemStack.foodComponent!!.nutrition
                    )
                }
                itemStack.foodComponent != null ->
                    FoodEstimationData(restoredHunger = itemStack.foodComponent!!.nutrition)
                else -> null
            }
        }
    }

    private object SilentOffhand : ToggleableConfigurable(this, "SilentOffhand", true) {
        private object RenderSlot : ToggleableConfigurable(this, "RenderSlot", true) {

            private val offset by int("Offset", 40, 30..70)

            @Suppress("unused")
            private val renderHandler = handler<OverlayRenderEvent> {
                renderEnvironmentForGUI {
                    // MC-Rendering code for off-hand

                    val currentFood = Estimator.findBestFood() ?: return@renderEnvironmentForGUI
                    val dc = newDrawContext()
                    val scaledWidth = dc.scaledWindowWidth
                    val scaledHeight = dc.scaledWindowHeight
                    val i: Int = scaledWidth / 2
                    val x = i - 91 - 26 - offset
                    val y = scaledHeight - 16 - 3
                    dc.drawStackOverlay(mc.textRenderer, currentFood.itemStack, x, y)
                    dc.drawItem(currentFood.itemStack, x, y)
                    dc.drawGuiTexture(
                        RenderLayer::getGuiTextured,
                        HOTBAR_OFFHAND_LEFT_TEXTURE, i - 91 - 29 - offset,
                        scaledHeight - 23, 29, 24
                    )
                }
            }

        }

        @Suppress("unused")
        private val interactionHandler = handler<PlayerInteractedItemEvent> { event ->
            if (!enabled) {
                return@handler
            }

            if (event.actionResult != ActionResult.PASS) {
                return@handler
            }

            val currentFood = Estimator.findBestFood() ?: return@handler

            val alwaysEdible = currentFood.itemStack.foodComponent?.canAlwaysEat == false

            if (!player.canConsume(false) && alwaysEdible) {
                return@handler
            }

            if (notDuringCombat && CombatManager.isInCombat) {
                return@handler
            }

            // Only use silent offhand if we have tools in hand.
            if (player.mainHandStack.item !is MiningToolItem) {
                return@handler
            }

            CombatManager.pauseCombatForAtLeast(combatPauseTime)
            SilentHotbar.selectSlotSilently(
                this@SilentOffhand,
                currentFood,
                swapBackDelay.coerceAtLeast(5)
            )
        }

        @Suppress("unused")
        private val tickHandler = tickHandler {
            val useAction = player.activeItem.useAction

            if (useAction != UseAction.EAT && useAction != UseAction.DRINK) {
                return@tickHandler
            }

            if (!SilentHotbar.isSlotModifiedBy(this@SilentOffhand)) {
                return@tickHandler
            }

            // if we are already eating, we want to keep the silent slot
            SilentHotbar.selectSlotSilently(this@SilentOffhand, SilentHotbar.serversideSlot, swapBackDelay)
        }

        init {
            tree(RenderSlot)
        }

    }

    private object AutoEat : ToggleableConfigurable(this, "AutoEat", true) {

        private val minHunger by int("MinHunger", 15, 0..20)
        private var forceUseKey = false

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (player.hungerManager.foodLevel < minHunger) {
                if (notDuringCombat && CombatManager.isInCombat) {
                    return@tickHandler
                }

                CombatManager.pauseCombatForAtLeast(combatPauseTime)
                waitUntil {
                    eat()
                    player.hungerManager.foodLevel > minHunger
                }

                forceUseKey = false
            }
        }

        @Suppress("unused")
        private val keyBindIsPressedHandler = handler<KeybindIsPressedEvent> { event ->
            if (event.keyBinding == mc.options.useKey && forceUseKey) {
                CombatManager.pauseCombatForAtLeast(combatPauseTime)
                event.isPressed = true
            }
        }

        fun eat() {
            val currentBestFood = Estimator.findBestFood() ?: return

            SilentHotbar.selectSlotSilently(AutoEat, currentBestFood, swapBackDelay)
            forceUseKey = true
        }

    }

    init {
        tree(SilentOffhand)
        tree(AutoEat)
    }

}
