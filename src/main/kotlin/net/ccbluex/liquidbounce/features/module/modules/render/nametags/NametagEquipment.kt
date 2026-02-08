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

package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.fastutil.objectLinkedSetOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.inventory.EquipmentSlotChoice
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.LivingEntity

internal object NametagEquipment : ValueGroup("Equipment") {

    private val slots by multiEnumChoice(
        "Slots",
        objectLinkedSetOf(
            EquipmentSlotChoice.MAINHAND, EquipmentSlotChoice.HEAD, EquipmentSlotChoice.CHEST,
            EquipmentSlotChoice.LEGS, EquipmentSlotChoice.FEET, EquipmentSlotChoice.OFFHAND,
        ),
        canBeNone = true
    )
    private val skipEmptySlot by boolean("SkipEmptySlot", true)
    val showInfo by boolean("ShowInfo", true)

    object HighlightItemInUse : ToggleableValueGroup(ModuleNametags, "HighlightItemInUse", false) {
        private val fillColor by color("FillColor", Color4b.RED.alpha(100))
        private val outlineColor by color("OutlineColor", Color4b.TRANSPARENT)

        context(guiGraphics: GuiGraphics)
        fun draw(x: Float, y: Float) {
            guiGraphics.drawQuad(
                x,
                y,
                x + 16F,
                y + 16F,
                fillColor,
                outlineColor,
            )
        }
    }

    init {
        tree(HighlightItemInUse)
    }

    /**
     * Creates a list of items that should be rendered above the name tag.
     */
    fun update(entity: LivingEntity, equipments: NametagRenderState.Equipments) {
        if (slots.isEmpty()) {
            equipments.reset()
            return
        }

        val stacks = slots.mapToArray {
            entity.getItemBySlot(it.slot)
        }

        equipments.itemStacks = if (skipEmptySlot) {
            stacks.filterNot { it.isEmpty }
        } else {
            stacks.asList()
        }

        equipments.highlightIndex = if (HighlightItemInUse.enabled && entity.isUsingItem) {
            val usingStack = entity.getItemInHand(entity.usedItemHand)
            equipments.itemStacks.indexOfFirst { usingStack === it }
        } else {
            -1
        }
    }
}
