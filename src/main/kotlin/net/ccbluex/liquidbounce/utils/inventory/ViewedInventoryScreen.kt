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
package net.ccbluex.liquidbounce.utils.inventory

import com.mojang.blaze3d.opengl.GlStateManager
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.INVENTORY_LOCATION
import net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class ViewedInventoryScreen(private val player: () -> Player?) : Screen(PlainText.EMPTY) {

    val handler: InventoryMenu?
        get() = player()?.inventoryMenu

    private val backgroundWidth: Int = 176
    private val backgroundHeight: Int = 166
    private var x: Int = (width - backgroundWidth) / 2
    private var y: Int = (height - backgroundHeight) / 2

    override fun init() {
        x = (width - backgroundWidth) / 2
        y = (height - backgroundHeight) / 2
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val handler = handler ?: return
        GlStateManager._disableDepthTest()
        context.pose().pushMatrix()
        context.pose().translate(x.toFloat(), y.toFloat())
        var hoveredSlot: Slot? = null

        for (slot in handler.slots) {
            if (slot.isActive) {
                drawSlot(context, slot)
            }

            if (isPointOverSlot(slot, mouseX.toDouble(), mouseY.toDouble()) && slot.isActive) {
                hoveredSlot = slot
                if (slot.isHighlightable) {
                    // draw slot highlight
                    context.fillGradient(
                        slot.x, slot.y,
                        slot.x + GuiRenderer.DEFAULT_ITEM_SIZE,
                        slot.y + GuiRenderer.DEFAULT_ITEM_SIZE,
                        -2130706433, -2130706433,
                    )
                }
            }
        }

        val cursorStack = handler.carried
        if (!cursorStack.isEmpty) {
            drawItem(context, cursorStack, mouseX - x - 8, mouseY - y - 8)
        }

        context.pose().popMatrix()
        GlStateManager._enableDepthTest()

        if (cursorStack.isEmpty && hoveredSlot != null && hoveredSlot.hasItem()) {
            val hoveredItemStack = hoveredSlot.item
            context.setTooltipForNextFrame(
                font, getTooltipFromItem(mc, hoveredItemStack),
                hoveredItemStack.tooltipImage, mouseX, mouseY
            )
        }
    }

    override fun renderBackground(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderTransparentBackground(context)
        drawBackground(context, mouseX, mouseY)
    }

    private fun drawItem(context: GuiGraphics, stack: ItemStack, x: Int, y: Int) {
        context.pose().withPush {
            context.renderItem(stack, x, y)
            context.renderItemDecorations(font, stack, x, y, null)
        }
    }

    private fun drawBackground(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        context.blit(
            RenderPipelines.GUI_TEXTURED, INVENTORY_LOCATION, x, y,
            0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256)
        player()?.let { player ->
            renderEntityInInventoryFollowsMouse(
                context, x + 26, y + 8, x + 75, y + 78,
                30, 0.0625f, mouseX.toFloat(), mouseY.toFloat(), player
            )
        }
    }

    private fun drawSlot(context: GuiGraphics, slot: Slot) {
        var spriteDrawn = false

        context.pose().pushMatrix()
        context.pose().translate(0f, 0f)
        if (slot.item.isEmpty && slot.isActive) {
            val identifier = slot.noItemIcon
            if (identifier != null) {
                context.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    identifier, slot.x, slot.y,
                    GuiRenderer.DEFAULT_ITEM_SIZE, GuiRenderer.DEFAULT_ITEM_SIZE
                )
                spriteDrawn = true
            }
        }

        if (!spriteDrawn) {
            val seed = slot.x + slot.y * backgroundWidth
            if (slot.isFake) {
                context.renderFakeItem(slot.item, slot.x, slot.y, seed)
            } else {
                context.renderItem(slot.item, slot.x, slot.y, seed)
            }

            context.renderItemDecorations(font, slot.item, slot.x, slot.y, null)
        }

        context.pose().popMatrix()
    }

    private fun isPointOverSlot(slot: Slot, pointX: Double, pointY: Double): Boolean {
        val width = GuiRenderer.DEFAULT_ITEM_SIZE
        val height = GuiRenderer.DEFAULT_ITEM_SIZE
        val pX = pointX - x
        val pY = pointY - y
        return pX >= slot.x - 1 && pX < slot.x + width + 1
            && pY >= slot.y - 1 && pY < slot.y + height + 1
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        super.keyPressed(input)

        if (mc.options.keyInventory.matches(input)) {
            onClose()
        }

        return true
    }

    override fun isPauseScreen() = false

    override fun tick() {
        if (handler == null) {
            onClose()
        }
    }
}
