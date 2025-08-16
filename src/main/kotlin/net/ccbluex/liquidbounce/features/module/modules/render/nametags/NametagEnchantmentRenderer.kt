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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.enchantments.EnchantColumn
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.enchantments.EnchantmentProcessor
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.enchantments.EnchantmentRenderer
import net.ccbluex.liquidbounce.render.RenderEnvironment
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.utils.item.getEnchantmentCount
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack

/**
 * Main renderer object that orchestrates enchantment display functionality.
 * This serves as the primary interface for rendering enchantments on nametags.
 */
object NametagEnchantmentRenderer {
    private const val FIXED_SCALE = 0.6f
    private const val PADDING = 3f

    /**
     * Draws enchantments for a single item stack
     * 
     * @param env Render environment
     * @param itemStack The item stack to render enchantments for
     * @param x X coordinate for rendering
     * @param y Y coordinate for rendering  
     * @param fontRenderer Font renderer for text drawing
     */
    fun drawEnchantments(
        env: RenderEnvironment,
        itemStack: ItemStack,
        x: Float,
        y: Float,
        fontRenderer: FontRendererBuffers
    ) {
        if (itemStack.isEmpty || !NametagShowOptions.ENCHANTMENTS.isShowing() || itemStack.getEnchantmentCount() <= 0) {
            return
        }

        val cells = EnchantmentProcessor.processItemEnchantments(itemStack)
        if (cells.isEmpty()) {
            return
        }

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        EnchantmentRenderer.renderEnchantmentColumn(env, cells, x, y, fontRenderer)
    }

    /**
     * Draws enchantments for all equipped items on an entity
     * 
     * @param env Render environment
     * @param entity The living entity to render enchantments for
     * @param fontRenderer Font renderer for text drawing
     */
    fun drawEntityEnchantments(
        env: RenderEnvironment,
        entity: LivingEntity,
        fontRenderer: FontRendererBuffers
    ) {
        if (!NametagShowOptions.ENCHANTMENTS.isShowing()) return

        val itemsWithEnchantments = EnchantmentProcessor.getEntityItemsWithEnchantments(entity)
        if (itemsWithEnchantments.isEmpty()) return

        val worldPos = entity.pos
        val worldX = worldPos.x.toFloat()
        val worldY = (worldPos.y + entity.height + 0.5f).toFloat()

        if (isPositionOccluded(worldX, worldY)) {
            return
        }

        val columnData = itemsWithEnchantments.mapNotNull { item ->
            val cells = EnchantmentProcessor.processItemEnchantments(item)
            if (cells.isEmpty()) return@mapNotNull null

            val maxWidth = cells.maxOfOrNull { it.textWidth } ?: 0f
            val columnWidth = maxWidth * FIXED_SCALE + PADDING * 2
            EnchantColumn(cells, columnWidth)
        }

        if (columnData.isNotEmpty()) {
            // Add this position to the drawn areas list to prevent overlapping
            ModuleNametags.drawnEnchantmentAreas.add(Pair(worldX, worldY))
            EnchantmentRenderer.drawEnchantmentColumns(env, worldX, worldY, fontRenderer, columnData)
        }
    }
    
    /**
     * Checks if a position would be occluded by another enchantment panel
     * This prevents overlapping enchantment displays
     * 
     * @param x X coordinate to check
     * @param y Y coordinate to check
     * @return true if the position would be occluded
     */
    private fun isPositionOccluded(x: Float, y: Float): Boolean {
        val OCCLUSION_THRESHOLD = 2f
        
        return ModuleNametags.drawnEnchantmentAreas.any { (existingX, existingY) ->
            val distance = Math.sqrt(((existingX - x) * (existingX - x) + 
                                     (existingY - y) * (existingY - y)).toDouble()).toFloat()
            distance < OCCLUSION_THRESHOLD
        }
    }
}
