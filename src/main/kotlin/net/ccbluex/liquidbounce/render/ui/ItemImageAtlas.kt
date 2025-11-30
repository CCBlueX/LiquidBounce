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
 *
 */

package net.ccbluex.liquidbounce.render.ui

import com.mojang.blaze3d.systems.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
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
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.ProjectionMatrix2
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.item.KeyedItemRenderState
import net.minecraft.client.util.BufferAllocator
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Rect2i
import net.minecraft.item.Item
import net.minecraft.item.ItemDisplayContext
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.awt.image.BufferedImage
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import kotlin.math.sqrt

private const val NATIVE_ITEM_SIZE: Int = 16

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
        val items = Registries.ITEM
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
    val items: Iterable<Item>,
    val count: Int,
    val scale: Int,
) : MinecraftShortcuts {
    private val itemsPerDimension = sqrt(count.toDouble()).ceilToInt()
    private val itemPixelSize = NATIVE_ITEM_SIZE * scale
    private val textureSize = itemPixelSize * itemsPerDimension

    private val itemAtlasFramebuffer = SimpleFramebuffer(
        "ItemImageAtlas Framebuffer",
        textureSize,
        textureSize,
        true,
    )
    private val bufferAllocator = BufferAllocator(0xC0000)
    private val vertexConsumers = VertexConsumerProvider.immediate(this.bufferAllocator)

    private val itemsProjectionMatrix = ProjectionMatrix2("items", -1000.0F, 1000.0F, true)

    private fun close() {
        itemsProjectionMatrix.close()
        bufferAllocator.close()
        itemAtlasFramebuffer.delete()
    }

    /**
     * @see net.minecraft.client.gui.render.GuiRenderer.prepareItemElements
     * From 1.21.5 DrawContext code
     */
    fun render(): CompletableFuture<Atlas> {
        itemAtlasFramebuffer.clearColorAndDepth(0, 1.0)
        RenderSystem.outputColorTextureOverride = itemAtlasFramebuffer.colorAttachmentView
        RenderSystem.outputDepthTextureOverride = itemAtlasFramebuffer.depthAttachmentView
        RenderSystem.backupProjectionMatrix()
        RenderSystem.setProjectionMatrix(
            this.itemsProjectionMatrix.set(textureSize.toFloat(), textureSize.toFloat()),
            ProjectionType.ORTHOGRAPHIC,
        )

        val matrixStack = Pools.MatStack.borrow()
        val keyedItemRenderState = KeyedItemRenderState()
        val itemMap = Reference2ObjectOpenHashMap<Item, Rect2i>(count)
        items.forEachIndexed { idx, item ->
            val x = (idx % itemsPerDimension) * itemPixelSize
            val y = (idx / itemsPerDimension) * itemPixelSize
            if (item !== Items.AIR) {
                val stack = item.defaultStack
                mc.itemModelManager.clearAndUpdate(
                    keyedItemRenderState,
                    stack,
                    ItemDisplayContext.GUI,
                    world,
                    player,
                    0
                )

                this.prepareItemInitially(keyedItemRenderState, matrixStack, x, y, itemPixelSize)
            }
            itemMap[item] = Rect2i(x, y, itemPixelSize, itemPixelSize)
        }
        keyedItemRenderState.clear()
        Pools.MatStack.recycle(matrixStack)

        RenderSystem.restoreProjectionMatrix()
        RenderSystem.outputColorTextureOverride = null
        RenderSystem.outputDepthTextureOverride = null

        return itemAtlasFramebuffer.colorAttachment!!.toBufferedImage()
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
     * @see net.minecraft.client.gui.render.GuiRenderer.prepareItemInitially
     */
    private fun prepareItemInitially(
        state: KeyedItemRenderState,
        matrices: MatrixStack,
        scaledX: Int,
        scaledY: Int,
        itemPixelSize: Int,
    ) {
        matrices.push()
        matrices.translate(
            scaledX.toFloat() + itemPixelSize.toFloat() * 0.5F,
            scaledY.toFloat() + itemPixelSize.toFloat() * 0.5F,
            0.0f,
        )
        matrices.scale(itemPixelSize.toFloat(), -itemPixelSize.toFloat(), itemPixelSize.toFloat())
        mc.gameRenderer.diffuseLighting.setShaderLights(
            if (state.isSideLit) DiffuseLighting.Type.ITEMS_3D else DiffuseLighting.Type.ITEMS_FLAT
        )

        RenderSystem.enableScissorForRenderTypeDraws(
            scaledX, textureSize - scaledY - itemPixelSize, itemPixelSize, itemPixelSize
        )
        state.render(matrices, vertexConsumers, 15728880, OverlayTexture.DEFAULT_UV)
        vertexConsumers.draw()
        RenderSystem.disableScissorForRenderTypeDraws()
        matrices.pop()
    }

    private fun findBlockToItemAliases(): Map<Identifier, Identifier> {
        val world = mc.world ?: return emptyMap()
        val map = Object2ObjectOpenHashMap<Identifier, Identifier>()

        Registries.BLOCK.forEach {
            val pickUpState = it.getPickStack(
                world,
                BlockPos.ORIGIN,
                it.defaultState,
                false
            )

            if (pickUpState.item !== it.asItem()) {
                val blockId = Registries.BLOCK.getId(it)
                val itemId = Registries.ITEM.getId(pickUpState.item)

                map[blockId] = itemId
            }
        }

        return map
    }

}
