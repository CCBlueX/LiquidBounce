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

import net.ccbluex.liquidbounce.render.ItemStackListRenderer.SingleItemStackRenderer
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

internal class NametagRenderState {

    @JvmField var entity: Entity? = null

    @JvmField var scale: Float = 0F

    /**
     * The text to render as nametag
     */
    @JvmField var text: Component = PlainText.EMPTY

    @JvmField val equipments = Equipments()

    /**
     * Updated on frame.
     */
    @JvmField var screenPos: Vec3f? = null

    fun update(entity: Entity, scale: Float) {
        this.entity = entity
        this.scale = scale
        this.text = NametagTextFormatter.format(entity)
        this.equipments.update(entity)
    }

    fun reset() {
        this.entity = null
        this.scale = 0F
        this.text = PlainText.EMPTY
        this.equipments.reset()
    }

    fun calculateScreenPos(tickDelta: Float): Vec3f? {
        val entity = this.entity!!
        val nametagPos = entity.interpolateCurrentPosition(tickDelta)
            .add(0.0, entity.getEyeHeight(entity.pose) + 0.55, 0.0)

        screenPos = WorldToScreen.calculateScreenPos(nametagPos)
        return screenPos
    }

    class Equipments {

        /**
         * The items that should be rendered above the name tag
         */
        @JvmField var itemStacks: List<ItemStack> = emptyList()

        /**
         * For entity using item.
         */
        @JvmField var highlightIndex: Int = -1

        fun update(entity: Entity) {
            if (entity is LivingEntity) {
                NametagEquipment.update(entity, this)
            } else {
                this.reset()
            }
        }

        fun reset() {
            this.itemStacks = emptyList()
            this.highlightIndex = -1
        }
    }

    fun equipmentStackRenderer(): SingleItemStackRenderer {
        val raw = if (NametagEquipment.showInfo) {
            if (entity === player) {
                SingleItemStackRenderer.All
            } else {
                SingleItemStackRenderer.ForOtherPlayer
            }
        } else {
            SingleItemStackRenderer.OnlyItem
        }

        if (equipments.highlightIndex !in equipments.itemStacks.indices) return raw

        return SingleItemStackRenderer { font, index, stack, x, y ->
            with(raw) {
                drawItemStack(font, index, stack, x, y)
            }
            if (equipments.highlightIndex == index) {
                NametagEquipment.HighlightItemInUse.draw(x.toFloat(), y.toFloat())
            }
        }
    }
}
