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

package net.ccbluex.liquidbounce.features.module.modules.combat

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.minecraft.client.KeyMapping
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.phys.BlockHitResult
import org.lwjgl.glfw.GLFW

/**
 * DoubleClickMacro module
 *
 * Allows you to double click with the mouse.
 */

object ModuleDoubleClickMacro : ClientModule("DoubleClickMacro", ModuleCategories.COMBAT) {
    var shouldLeftButtonDoubleClick = false
    var shouldRightButtonDoubleClick = false

    object AttackButton : ToggleableValueGroup(this, "Attack", true) {
        internal var chance by int("Chance", 80, 1..100)
    }

    object UseButton : ToggleableValueGroup(this, "Use", false) {
        internal var chance by int("Chance", 80, 1..100)

        internal val holdingItemsForIgnore by items(
            "HoldingItemsForIgnore",
            itemSortedSetOf(
                Items.WATER_BUCKET,
                Items.LAVA_BUCKET,
                Items.ENDER_PEARL,
                Items.ENDER_EYE,
                Items.PLAYER_HEAD,
            ),
        )
        internal val blocksForIgnore by blocks(
            "BlocksForIgnore",
            BuiltInRegistries.BLOCK.filterTo(blockSortedSetOf()) {
                it is DoorBlock || it is FenceGateBlock || it is TrapDoorBlock
            },
        )
        internal val onlyBlock by boolean("OnlyBlock", false)
    }

    private val SPECIAL_ITEMS_FOR_IGNORE = ReferenceOpenHashSet.of(
        Items.RED_BED,
        Items.PLAYER_HEAD,
        Items.COMPASS,
        Items.EMERALD,
        Items.LAPIS_LAZULI,
        Items.GREEN_DYE,
        Items.GRAY_DYE,
        Items.PINK_DYE,
        Items.SLIME_BALL,
    )

    init {
        tree(AttackButton)
        tree(UseButton)
    }
    @Suppress("unused")
    private val mouseButtonHandler = handler<MouseButtonEvent> { event ->
        if (event.action == GLFW.GLFW_PRESS && event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            shouldLeftButtonDoubleClick = true
        }
        if (event.action == GLFW.GLFW_PRESS && event.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            shouldRightButtonDoubleClick = true
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        AttackButton.run {
            if (!enabled || !mc.options.keyAttack.isPressedOnAny) {
                return@run
            }

            if (interaction.isDestroying) {
                return@run
            }

            if (chance < (1..100).random()) {
                return@run
            }

            if (shouldLeftButtonDoubleClick) {
                KeyMapping.click(mc.options.keyAttack.key)
                shouldLeftButtonDoubleClick = false
            }
        }

        UseButton.run {
            if (!enabled || !mc.options.keyUse.isPressedOnAny) {
                return@run
            }

            val mainHandStack = player.mainHandItem
            val offHandStack = player.offhandItem
            if (mainHandStack.item in SPECIAL_ITEMS_FOR_IGNORE && mainHandStack.customName != null) {
                return@run
            }

            if (mainHandStack.item in holdingItemsForIgnore || offHandStack.item in holdingItemsForIgnore) {
                return@run
            }

            if (onlyBlock && mainHandStack.item !is BlockItem && offHandStack.item !is BlockItem) {
                return@run
            }

            val crosshairTarget = mc.hitResult
            if (crosshairTarget is BlockHitResult) {
                val blockState = mc.level?.getBlockState(crosshairTarget.blockPos)
                if (blockState?.block in blocksForIgnore) {
                    return@run
                }
            }

            if (chance < (1..100).random()) {
                return@run
            }

            if (shouldRightButtonDoubleClick) {
                KeyMapping.click(mc.options.keyUse.key)
                shouldRightButtonDoubleClick = false
            }
        }
    }
}
