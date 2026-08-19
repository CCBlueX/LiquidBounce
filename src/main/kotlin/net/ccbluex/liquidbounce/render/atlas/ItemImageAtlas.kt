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

package net.ccbluex.liquidbounce.render.atlas

import com.mojang.blaze3d.platform.Lighting
import kotlinx.coroutines.future.await
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.utils.client.inGame
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.Rect2i
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items

private const val NATIVE_ITEM_SIZE: Int = GuiRenderer.DEFAULT_ITEM_SIZE

private class Atlas(
    val images: Map<Identifier, ByteArray>,
    /**
     * Contains aliases. For example `minecraft:blue_wall_banner` -> `minecraft:wall_banner` which is necessary since
     * `minecraft:blue_wall_banner` has no texture.
     */
    val aliasMap: Map<Identifier, Identifier>,
)

/**
 *
 */
object ItemImageAtlas : EventListener {

    @Volatile
    private var atlas: Atlas? = null

    @Suppress("unused")
    private val resourceReloadHandler = suspendHandler<ResourceReloadEvent>(
        behavior = SuspendHandlerBehavior.CancelPrevious,
    ) {
        tickUntil { inGame }
        atlas = ItemTextureRenderer(scale = 4).render().await()
    }

    fun getItemImage(name: Identifier): AtlasLookup {
        val atlas = this.atlas ?: return AtlasLookup.NotReady
        val resolvedName = atlas.aliasMap[name] ?: name
        val bytes = atlas.images[resolvedName]
        return if (bytes == null) AtlasLookup.Missing else AtlasLookup.Found(bytes)
    }
}

/**
 * @see net.minecraft.client.gui.render.GuiItemAtlas
 */
private class ItemTextureRenderer(private val scale: Int) : AbstractAtlasRenderer<Atlas>("Item") {

    private val items = BuiltInRegistries.ITEM

    override val tileSize get() = NATIVE_ITEM_SIZE * scale
    override val tileCount get() = items.size()

    override fun buildAtlas(images: Map<Identifier, ByteArray>) = Atlas(images, findBlockToItemAliases())

    /**
     * @see GuiRenderer.prepareItemElements
     * From 1.21.5 DrawContext code
     */
    override fun renderTiles() = buildMap(items.size()) {
        val keyedItemRenderState = TrackingItemStackRenderState()
        for ((idx, item) in items.withIndex()) {
            val rect = tileRect(idx)
            if (item !== Items.AIR) {
                mc.itemModelResolver.updateForTopItem(
                    keyedItemRenderState,
                    item.defaultInstance, // TODO: support dynamic rendered ItemStack
                    ItemDisplayContext.GUI,
                    world,
                    player,
                    0,
                )
                renderItemToAtlas(keyedItemRenderState, rect)
            }
            this[BuiltInRegistries.ITEM.getKey(item)] = rect
        }
    }

    /**
     * @see net.minecraft.client.gui.render.GuiItemAtlas.drawToSlot
     */
    private fun renderItemToAtlas(
        state: TrackingItemStackRenderState,
        rect: Rect2i,
    ) {
        withTile(rect) {
            translate(
                rect.x.toFloat() + rect.width.toFloat() * 0.5F,
                rect.y.toFloat() + rect.height.toFloat() * 0.5F,
                0.0f,
            )
            scale(rect.width.toFloat(), -rect.height.toFloat(), rect.width.toFloat())
            mc.gameRenderer.lighting().setupFor(
                if (state.usesBlockLight()) Lighting.Entry.ITEMS_3D else Lighting.Entry.ITEMS_FLAT
            )

            state.submit(this, submitNodeStorage, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0)
            featureRenderDispatcher.renderAllFeatures(submitNodeStorage)
        }
    }

    private fun findBlockToItemAliases() = buildMap {
        BuiltInRegistries.BLOCK.forEach { block ->
            val blockId = BuiltInRegistries.BLOCK.getKey(block)
            val itemId = findItemIdForBlock(blockId, block.asItem()) ?: return@forEach

            // Only keep aliases where the identifier differs. Blocks whose item has
            // the same id are resolved by the regular item lookup path.
            if (itemId != blockId) {
                this[blockId] = itemId
            }
        }
    }

    /**
     * Resolve a block id to the item used for its icon. Most blocks expose this
     * directly through [net.minecraft.world.level.block.Block.asItem]. Wall variants do not have their own
     * BlockItem, so their item id is the corresponding non-wall variant.
     */
    private fun findItemIdForBlock(blockId: Identifier, blockItem: Item): Identifier? {
        if (blockItem !== Items.AIR) {
            return BuiltInRegistries.ITEM.getKey(blockItem)
        }

        val path = blockId.path
        val candidatePath = when {
            path.startsWith("wall_") -> path.removePrefix("wall_")
            "_wall_" in path -> path.replace("_wall_", "_")
            else -> path
        }
        if (candidatePath == path) {
            return null
        }

        val candidateId = Identifier.fromNamespaceAndPath(blockId.namespace, candidatePath)
        return candidateId.takeIf { BuiltInRegistries.ITEM.containsKey(it) }
    }

}
