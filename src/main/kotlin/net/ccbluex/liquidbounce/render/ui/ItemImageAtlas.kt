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

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.ceilToInt
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.asView
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth
import net.ccbluex.liquidbounce.utils.render.saveToFile
import net.ccbluex.liquidbounce.utils.render.toBufferedImage
import net.ccbluex.liquidbounce.utils.render.toNativeImage
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.util.math.Rect2i
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import org.joml.Matrix4f
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
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
    val aliasMap: Map<Identifier, Identifier>
)

/**
 *
 */
object ItemImageAtlas : EventListener {

    private var atlas: Atlas? = null
    private var updateFuture: CompletableFuture<*>? = null

    fun updateAtlas(drawContext: DrawContext): Boolean {
        if (this.atlas != null || this.updateFuture != null) {
            return false
        }

        updateFuture = ItemTextureRenderer(items = Registries.ITEM, scale = 4)
            .render(drawContext).thenAcceptAsync({
                atlas = it
                updateFuture = null
            }, mc)

        return true
    }

    @Suppress("unused")
    private val resourceReloadHandler = handler<ResourceReloadEvent> {
        this.atlas = null
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
    val scale: Int,
) : MinecraftShortcuts {
    private val itemsPerDimension = sqrt(items.size().toDouble()).ceilToInt()
    private val itemPixelSize = NATIVE_ITEM_SIZE * scale
    private val textureSize = itemPixelSize * itemsPerDimension

    fun render(ctx: DrawContext): CompletableFuture<Atlas> {
        // FIXME
        val texture = gpuDevice.createTexture(
            "ItemImageAtlas Texture",
            GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_COPY_SRC,
            TextureFormat.RGBA8,
            textureSize,
            textureSize,
            1,
            1,
        )

        val backupTexture = RenderSystem.outputColorTextureOverride
        RenderSystem.outputColorTextureOverride = texture.asView()

        val projectionMatrix = RenderSystem.getProjectionMatrixBuffer()

        val bufferSlice = gpuDevice.createBuffer(
            { "ItemImageAtlas ProjMat" },
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST or GpuBuffer.USAGE_MAP_READ,
            Matrix4f().setOrtho(
                0f,
                textureSize.toFloat(),
                textureSize.toFloat(),
                0f,
                1000f,
                21000f
            )[MemoryUtil.memAlloc(4 * 4 * Float.SIZE_BYTES)],
        ).slice()

        RenderSystem.setProjectionMatrix(bufferSlice, ProjectionType.ORTHOGRAPHIC)
        ctx.matrices.pushMatrix()
        ctx.matrices.scale(scale.toFloat())

        val itemMap = Reference2ObjectOpenHashMap<Item, Rect2i>(items.size())

        items.forEachIndexed { idx, item ->
            val x = (idx % itemsPerDimension) * NATIVE_ITEM_SIZE
            val y = (idx / itemsPerDimension) * NATIVE_ITEM_SIZE
            ctx.drawItem(item.defaultStack, x, y)
            itemMap[item] = Rect2i(x * scale, y * scale, itemPixelSize, itemPixelSize)
        }

        ctx.matrices.popMatrix()

        RenderSystem.setProjectionMatrix(projectionMatrix, ProjectionType.ORTHOGRAPHIC)
        RenderSystem.outputColorTextureOverride = backupTexture

        return texture.toNativeImage()
            .thenApplyAsync(NativeImage::toBufferedImage, Util.getIoWorkerExecutor())
            .thenApply { image ->
                logger.info("Loaded ${image.width} x ${image.height} item atlas")

                Atlas(itemMap, image, findAliases())
            }
    }

    private fun findAliases(): Map<Identifier, Identifier> {
        val map = Object2ObjectOpenHashMap<Identifier, Identifier>()

        Registries.BLOCK.forEach {
            val pickUpState = it.getPickStack(
                mc.world!!,
                BlockPos.ORIGIN,
                it.defaultState,
                false
            )

            if (pickUpState.item != it) {
                val blockId = Registries.BLOCK.getId(it)
                val itemId = Registries.ITEM.getId(pickUpState.item)

                map[blockId] = itemId
            }
        }

        return map
    }

}
