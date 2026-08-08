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

package net.ccbluex.liquidbounce.render.gui

import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import kotlinx.coroutines.future.await
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.math.ceilToInt
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth
import net.ccbluex.liquidbounce.utils.render.copyTo
import net.ccbluex.liquidbounce.utils.render.readFully
import net.ccbluex.liquidbounce.utils.render.withOutputTextureOverride
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.renderer.Projection
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.Rect2i
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.util.LightCoordsUtil
import net.minecraft.util.Util
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import okio.Buffer
import org.apache.commons.lang3.function.Consumers
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

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
        val items = BuiltInRegistries.ITEM
        atlas = ItemTextureRenderer(items = items, scale = 4).render().await()
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
@Suppress("TooManyFunctions")
private class ItemTextureRenderer(
    val items: Registry<Item>,
    val scale: Int,
) : MinecraftShortcuts {
    private val itemsPerDimension = sqrt(items.size().toDouble()).ceilToInt()
    private val itemPixelSize = NATIVE_ITEM_SIZE * scale
    private val textureSize = itemPixelSize * itemsPerDimension

    private val itemAtlasFramebuffer = TextureTarget(
        "ItemImageAtlas Framebuffer",
        textureSize,
        textureSize,
        true,
        GpuFormat.RGBA8_UNORM,
    )
    private val submitNodeStorage = SubmitNodeStorage()

    // Note: no operation -> use shared one or skip it
    private val featureRenderDispatcher = FeatureRenderDispatcher(
        mc.gameRenderer.renderBuffers,
        mc.modelManager, // No operation
        mc.atlasManager, // No operation
        mc.font, // No operation
        mc.gameRenderer.gameRenderState(), // No operation
    )

    private val poseStack = PoseStack()
    private val projection = Projection()
    private val projectionMatrixBuffer = ProjectionMatrixBuffer("items")
    private val closed = AtomicBoolean()

    private fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }

        projectionMatrixBuffer.close()
        itemAtlasFramebuffer.destroyBuffers()
        submitNodeStorage.drainPhases(Consumers.nop())
        featureRenderDispatcher.close()
    }

    /**
     * @see net.minecraft.client.gui.render.GuiRenderer.prepareItemElements
     * From 1.21.5 DrawContext code
     */
    fun render(): CompletableFuture<Atlas> = try {
        val itemMap = renderItems()
        val aliasMap = findBlockToItemAliases()
        submitReadback(itemMap, aliasMap)
    } catch (throwable: Throwable) {
        close()
        CompletableFuture.failedFuture(throwable)
    }

    private fun renderItems(): Map<Identifier, Rect2i> {
        itemAtlasFramebuffer.clearColorAndDepth()
        RenderSystem.backupProjectionMatrix()
        try {
            projection.setupOrtho(
                -1000.0F,
                1000.0F,
                textureSize.toFloat(),
                textureSize.toFloat(),
                true,
            )
            RenderSystem.setProjectionMatrix(
                projectionMatrixBuffer.getBuffer(projection),
                ProjectionType.ORTHOGRAPHIC,
            )

            withOutputTextureOverride(
                itemAtlasFramebuffer.colorTextureView,
                itemAtlasFramebuffer.depthTextureView,
            ) {
                 return buildMap(items.size()) {
                    val keyedItemRenderState = TrackingItemStackRenderState()
                    for ((idx, item) in items.withIndex()) {
                        val x = (idx % itemsPerDimension) * itemPixelSize
                        val y = (idx / itemsPerDimension) * itemPixelSize
                        if (item !== Items.AIR) {
                            mc.itemModelResolver.updateForTopItem(
                                keyedItemRenderState,
                                item.defaultInstance, // TODO: support dynamic rendered ItemStack
                                ItemDisplayContext.GUI,
                                world,
                                player,
                                0,
                            )
                            renderItemToAtlas(keyedItemRenderState, x, y, itemPixelSize)
                        }
                        this[BuiltInRegistries.ITEM.getKey(item)] = Rect2i(x, y, itemPixelSize, itemPixelSize)
                    }
                }
            }
        } finally {
            RenderSystem.restoreProjectionMatrix()
        }
    }

    private fun submitReadback(
        itemMap: Map<Identifier, Rect2i>,
        aliasMap: Map<Identifier, Identifier>,
    ): CompletableFuture<Atlas> {
        val colorTexture = requireNotNull(itemAtlasFramebuffer.colorTexture) {
            "Item atlas framebuffer has no color texture"
        }
        val readbackBuffer = gpuDevice.createBuffer(
            { "ItemImageAtlas readback" },
            GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_COPY_DST,
            textureSize.toLong() * textureSize * GpuFormat.RGBA8_UNORM.blockSize(),
        )

        val result = CompletableFuture<Atlas>()
        try {
            colorTexture.copyTo(readbackBuffer) {
                processReadback(readbackBuffer, itemMap, aliasMap, result)
            }
        } catch (t: Throwable) {
            readbackBuffer.close()
            throw t
        }
        return result
    }

    private fun processReadback(
        readbackBuffer: GpuBuffer,
        itemMap: Map<Identifier, Rect2i>,
        aliasMap: Map<Identifier, Identifier>,
        result: CompletableFuture<Atlas>,
    ) {
        val atlasPixels = try {
            if (result.isCancelled) {
                return
            }

            readbackBuffer.readFully()
        } catch (throwable: Throwable) {
            completeExceptionally(result, throwable)
            return
        } finally {
            readbackBuffer.close()
            close()
        }

        encodeAsync(atlasPixels, itemMap, aliasMap, result)
    }

    private fun encodeAsync(
        atlasPixels: ByteBuffer,
        itemMap: Map<Identifier, Rect2i>,
        aliasMap: Map<Identifier, Identifier>,
        result: CompletableFuture<Atlas>,
    ) {
        try {
            Util.backgroundExecutor().execute {
                try {
                    if (result.isCancelled) return@execute

                    val atlas = Atlas(
                        encodeItemImages(atlasPixels, itemMap, result),
                        aliasMap,
                    )

                    if (!result.isCancelled) {
                        logger.info("Loaded $textureSize x $textureSize item atlas with ${atlas.images.size} PNGs")
                        result.complete(atlas)
                    }
                } catch (throwable: Throwable) {
                    completeExceptionally(result, throwable)
                } finally {
                    MemoryUtil.memFree(atlasPixels)
                }
            }
        } catch (throwable: Throwable) {
            MemoryUtil.memFree(atlasPixels)
            completeExceptionally(result, throwable)
        }
    }

    private fun completeExceptionally(result: CompletableFuture<*>, throwable: Throwable) {
        if (throwable !is CancellationException) {
            logger.error("Failed to load item atlas", throwable)
        }
        result.completeExceptionally(throwable)
    }

    private fun encodeItemImages(
        atlasPixels: ByteBuffer,
        itemMap: Map<Identifier, Rect2i>,
        result: CompletableFuture<Atlas>,
    ) = buildMap(itemMap.size) {
        NativeImage(itemPixelSize, itemPixelSize, false).use { itemImage ->
            val buffer = Buffer()
            for ((identifier, rect) in itemMap) {
                if (result.isCancelled) {
                    throw CancellationException("Item atlas generation was cancelled")
                }

                atlasPixels.copyRectTo(itemImage, rect)
                check(itemImage.writeToChannel(buffer)) { "Failed to encode item texture $identifier" }
                val encoded = buffer.readByteArray()
                this[identifier] = encoded
            }
        }
    }

    private fun ByteBuffer.copyRectTo(target: NativeImage, rect: Rect2i) {
        require(target.format() == NativeImage.Format.RGBA)
        require(rect.width == target.width && rect.height == target.height)
        require(rect.x >= 0 && rect.y >= 0 && rect.x + rect.width <= textureSize && rect.y + rect.height <= textureSize)

        val bytesPerPixel = NativeImage.Format.RGBA.components()
        val rowBytes = rect.width * bytesPerPixel.toLong()
        val sourcePixels = MemoryUtil.memAddress(this)
        for (row in 0 until rect.height) {
            // GPU readback rows are bottom-up while atlas rectangles use top-down coordinates.
            val sourceY = textureSize - rect.y - row - 1
            val sourceOffset = (sourceY * textureSize + rect.x) * bytesPerPixel.toLong()
            val targetOffset = row * rowBytes
            MemoryUtil.memCopy(sourcePixels + sourceOffset, target.pointer + targetOffset, rowBytes)
        }
    }

    /**
     * @see net.minecraft.client.gui.render.GuiItemAtlas.drawToSlot
     */
    private fun renderItemToAtlas(
        state: TrackingItemStackRenderState,
        scaledX: Int,
        scaledY: Int,
        itemPixelSize: Int,
    ) {
        poseStack.withPush {
            poseStack.translate(
                scaledX.toFloat() + itemPixelSize.toFloat() * 0.5F,
                scaledY.toFloat() + itemPixelSize.toFloat() * 0.5F,
                0.0f,
            )
            poseStack.scale(itemPixelSize.toFloat(), -itemPixelSize.toFloat(), itemPixelSize.toFloat())
            mc.gameRenderer.lighting().setupFor(
                if (state.usesBlockLight()) Lighting.Entry.ITEMS_3D else Lighting.Entry.ITEMS_FLAT
            )

            RenderSystem.enableScissorForRenderTypeDraws(
                scaledX, textureSize - scaledY - itemPixelSize, itemPixelSize, itemPixelSize
            )
            try {
                state.submit(poseStack, submitNodeStorage, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0)
                featureRenderDispatcher.renderAllFeatures(submitNodeStorage)
            } finally {
                RenderSystem.disableScissorForRenderTypeDraws()
            }
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
