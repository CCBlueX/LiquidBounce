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

import net.ccbluex.liquidbounce.features.module.modules.render.nametags.NametagEnchantmentRenderer.drawItemEnchantments
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.ItemStackListRenderState
import net.ccbluex.liquidbounce.render.gui.ItemStackListRenderer.SingleItemStackRenderer
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityEquipment
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

internal class NametagRenderState {

    @JvmField var entity: Entity? = null

    @JvmField var scale: Float = 0F

    /**
     * Distance to the camera, kept so that sorting does not recompute it for every comparison.
     *
     * @see ModuleNametags.collectAndSortNametagsToRender
     */
    @JvmField var distance: Float = 0F

    /**
     * The text to render as nametag
     */
    @JvmField var text: FormattedCharSequence = FormattedCharSequence.EMPTY

    @JvmField val equipments = Equipments()

    /**
     * Updated on frame.
     */
    @JvmField var screenPos: Vec3f? = null

    fun update(entity: Entity, scale: Float, distance: Float) {
        this.entity = entity
        this.scale = scale
        this.distance = distance
        this.text = NametagTextFormatter.format(entity)
        this.equipments.update(entity)
    }

    fun reset() {
        this.entity = null
        this.scale = 0F
        this.distance = 0F
        this.text = FormattedCharSequence.EMPTY
        this.equipments.reset()
    }

    fun calculateScreenPos(tickDelta: Float): Vec3f? {
        val entity = this.entity ?: return null
        val nametagPos = entity.interpolateCurrentPosition(tickDelta)
            .add(0.0, entity.getEyeHeight(entity.pose) + 0.55, 0.0)

        screenPos = WorldToScreen.calculateScreenPos(nametagPos)
        return screenPos
    }

    inner class Equipments {
        /**
         * The order of equipment slots
         */
        @JvmField val slotOrder = ArrayList<EquipmentSlot>()

        /**
         * The items that should be rendered above the name tag
         */
        @JvmField val equipment = EntityEquipment()

        /**
         * For entity using item.
         */
        @JvmField var highlightStackRef: ItemStack? = null

        fun update(entity: Entity) {
            if (entity is LivingEntity) {
                NametagEquipment.update(entity, this)
            } else {
                this.reset()
            }
        }

        fun reset() {
            this.slotOrder.clear()
            this.equipment.clear()
            this.highlightStackRef = null
        }

        private val stacksView = object : AbstractList<ItemStack>(), RandomAccess {
            override val size get() = slotOrder.size
            override fun get(index: Int) = equipment[slotOrder[index]]
        }

        /**
         * Drawn list of [stacksView], reused by every frame: each draw binds it to the current
         * extractor and sets the position and scale again.
         */
        @JvmField
        val equipmentList = ItemStackListRenderState(stacksView)
            .rectBackground(Color4b.TRANSPARENT)
            .itemStackRenderer { font, index, stack, x, y ->
                val delegation = if (NametagEquipment.showInfo) {
                    if (entity === player) {
                        SingleItemStackRenderer.All
                    } else {
                        SingleItemStackRenderer.ForOtherPlayer
                    }
                } else {
                    SingleItemStackRenderer.OnlyItem
                }

                with(delegation) {
                    drawItemStack(
                        font = font,
                        index = index,
                        stack = stack,
                        x = x,
                        y = y,
                    )
                }

                if (highlightStackRef === stack) {
                    NametagEquipment.HighlightItemInUse.draw(x.toFloat(), y.toFloat())
                }

                drawItemEnchantments(
                    stack = stack,
                    x = x.toFloat(),
                    y = y.toFloat(),
                )
            }
    }

}
