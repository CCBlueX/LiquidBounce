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

package net.ccbluex.liquidbounce.render.ui

import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import kotlinx.coroutines.future.await
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.ceilToInt
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth
import net.ccbluex.liquidbounce.utils.render.toBufferedImage
import net.ccbluex.liquidbounce.utils.render.withOutputTextureOverride
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.client.renderer.Rect2i
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import java.awt.image.BufferedImage
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import kotlin.math.sqrt

private const val NATIVE_ITEM_SIZE: Int = GuiRenderer.DEFAULT_ITEM_SIZE

private class Atlas(
    val map: Map<Item, Rect2i>,
    val image: BufferedImage,
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

    private var atlas: Atlas? = null

    @Suppress("unused")
    private val resourceReloadHandler = suspendHandler<ResourceReloadEvent>(
        behavior = SuspendHandlerBehavior.CancelPrevious,
    ) {
        tickUntil { inGame }
        val items = BuiltInRegistries.ITEM
        atlas = ItemTextureRenderer(items = items, count = items.size(), scale = 4).render().await()
    }

    val isAtlasAvailable
        get() = this.atlas != null

    fun resolveAliasIfPresent(name: Identifier): Identifier {
        return atlas!!.aliasMap[name] ?: return name
    }

    fun getItemImage(item: Item): BufferedImage? {
        val atlas = requireNotNull(this.atlas) { "Atlas is not available yet" }
        val rect = atlas.map[item] ?: return null

        return atlas.image.getSubimage(
            rect.x,
            rect.y,
            rect.width,
            rect.height,
        )!!
    }
}

private class ItemTextureRenderer(
    val items: Registry<Item>,
    val count: Int,
    val scale: Int,
) : MinecraftShortcuts {
    private val itemsPerDimension = sqrt(count.toDouble()).ceilToInt()
    private val itemPixelSize = NATIVE_ITEM_SIZE * scale
    private val textureSize = itemPixelSize * itemsPerDimension

    private val itemAtlasFramebuffer = TextureTarget(
        "ItemImageAtlas Framebuffer",
        textureSize,
        textureSize,
        true,
    )
    private val submitNodeCollector = SubmitNodeStorage()
    private val bufferSource = mc.gameRenderer.renderBuffers.bufferSource()

    // Note: no operation -> use shared one or skip it
    private val featureRenderDispatcher = FeatureRenderDispatcher(
        this.submitNodeCollector,
        mc.blockRenderer, // No operation
        bufferSource,
        mc.atlasManager, // No operation
        mc.gameRenderer.renderBuffers.outlineBufferSource(), // No operation
        mc.gameRenderer.renderBuffers.crumblingBufferSource(), // No operation
        mc.font, // No operation
    )

    private val itemsProjectionMatrix = CachedOrthoProjectionMatrixBuffer("items", -1000.0F, 1000.0F, true)

    private fun close() {
        itemsProjectionMatrix.close()
        itemAtlasFramebuffer.destroyBuffers()
        submitNodeCollector.clear()
        featureRenderDispatcher.close()
    }

    /**
     * @see net.minecraft.client.gui.render.GuiRenderer.prepareItemElements
     * From 1.21.5 DrawContext code
     */
    fun render(): CompletableFuture<Atlas> {
        itemAtlasFramebuffer.clearColorAndDepth()
        RenderSystem.backupProjectionMatrix()
        RenderSystem.setProjectionMatrix(
            this.itemsProjectionMatrix.getBuffer(textureSize.toFloat(), textureSize.toFloat()),
            ProjectionType.ORTHOGRAPHIC,
        )
        val itemMap = Reference2ObjectOpenHashMap<Item, Rect2i>(count)

        withOutputTextureOverride(itemAtlasFramebuffer.colorTextureView, itemAtlasFramebuffer.depthTextureView) {
            val matrixStack = Pools.MatStack.borrow()
            val keyedItemRenderState = TrackingItemStackRenderState()
            for ((idx, item) in items.withIndex()) {
                val x = (idx % itemsPerDimension) * itemPixelSize
                val y = (idx / itemsPerDimension) * itemPixelSize
                if (item !== Items.AIR) {
                    val stack = item.defaultInstance
                    mc.itemModelResolver.updateForTopItem(
                        keyedItemRenderState,
                        stack,
                        ItemDisplayContext.GUI,
                        world,
                        player,
                        0
                    )

                    this.renderItemToAtlas(keyedItemRenderState, matrixStack, x, y, itemPixelSize)
                }
                itemMap[item] = Rect2i(x, y, itemPixelSize, itemPixelSize)
            }
            keyedItemRenderState.clear()
            Pools.MatStack.recycle(matrixStack)
        }

        RenderSystem.restoreProjectionMatrix()

        return itemAtlasFramebuffer.colorTexture!!.toBufferedImage()
            .thenApply { image ->
                logger.info("Loaded ${image.width} x ${image.height} item atlas")

                this.close()
//                ImageIO.write(image, "png", java.io.File("Debug_ItemAtlas.png"))

                Atlas(itemMap, image, findBlockToItemAliases())
            }.whenComplete { _, throwable ->
                if (throwable != null && throwable !is CancellationException) {
                    logger.error("Failed to load item atlas", throwable)
                }
            }
    }

    /**
     * @see GuiRenderer.renderItemToAtlas
     */
    private fun renderItemToAtlas(
        state: TrackingItemStackRenderState,
        matrices: PoseStack,
        scaledX: Int,
        scaledY: Int,
        itemPixelSize: Int,
    ) {
        matrices.pushPose()
        matrices.translate(
            scaledX.toFloat() + itemPixelSize.toFloat() * 0.5F,
            scaledY.toFloat() + itemPixelSize.toFloat() * 0.5F,
            0.0f,
        )
        matrices.scale(itemPixelSize.toFloat(), -itemPixelSize.toFloat(), itemPixelSize.toFloat())
        mc.gameRenderer.lighting.setupFor(
            if (state.usesBlockLight()) Lighting.Entry.ITEMS_3D else Lighting.Entry.ITEMS_FLAT
        )

        RenderSystem.enableScissorForRenderTypeDraws(
            scaledX, textureSize - scaledY - itemPixelSize, itemPixelSize, itemPixelSize
        )
        state.submit(matrices, this.submitNodeCollector, 0xf000f0, OverlayTexture.NO_OVERLAY, 0)
        featureRenderDispatcher.renderAllFeatures()
        bufferSource.endBatch()
        RenderSystem.disableScissorForRenderTypeDraws()
        matrices.popPose()
    }

    private fun findBlockToItemAliases(): Map<Identifier, Identifier> {
        val world = mc.level ?: return emptyMap()
        val map = Object2ObjectOpenHashMap<Identifier, Identifier>()

        BuiltInRegistries.BLOCK.forEach {
            val pickUpState = it.getCloneItemStack(
                world,
                BlockPos.ZERO,
                it.defaultBlockState(),
                false
            )

            if (pickUpState.item !== it.asItem()) {
                val blockId = BuiltInRegistries.BLOCK.getKey(it)
                val itemId = BuiltInRegistries.ITEM.getKey(pickUpState.item)

                map[blockId] = itemId
            }
        }

        return map
    }

}
