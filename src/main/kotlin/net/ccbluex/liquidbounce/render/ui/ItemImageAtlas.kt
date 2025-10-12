package net.ccbluex.liquidbounce.render.ui

import com.mojang.blaze3d.buffers.BufferType
import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.systems.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.toBufferedImage
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.util.math.Rect2i
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.joml.Matrix4f
import java.awt.image.BufferedImage
import java.lang.AutoCloseable
import java.util.OptionalInt
import kotlin.math.ceil
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

    fun updateAtlas(drawContext: DrawContext) {
        if (this.atlas != null) {
            return
        }

        val renderer = ItemFramebufferRenderer(
            Registries.ITEM,
            4
        )

        val items = renderer.render(drawContext)

        val image = renderer.toNativeImage().toBufferedImage()

        renderer.close()

        this.atlas = Atlas(items, image, findAliases())
    }

    private fun findAliases(): Map<Identifier, Identifier> {
        val map = hashMapOf<Identifier, Identifier>()

        Registries.BLOCK.forEach {
            val pickUpState = it.getPickStack(mc.world!!, BlockPos.ORIGIN, it.defaultState, false)

            if (pickUpState.item != it) {
                val blockId = Registries.BLOCK.getId(it)
                val itemId = Registries.ITEM.getId(pickUpState.item)

                map[blockId] = itemId
            }
        }

        return map
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

private class ItemFramebufferRenderer(
    val items: Registry<Item>,
    val scale: Int,
) : MinecraftShortcuts, AutoCloseable {
    private val itemsPerDimension = ceil(sqrt(items.size().toDouble())).toInt()
    private val itemPixelSize = 16 * scale

    private val gpuDevice = RenderSystem.getDevice()

    val gpuTexture: GpuTexture = gpuDevice.createTexture(
        "ItemAtlasTexture",
        TextureFormat.RGBA8,
        itemPixelSize * itemsPerDimension,
        itemPixelSize * itemsPerDimension,
        1
    )

    private val itemPixelSizeOnFramebuffer = NATIVE_ITEM_SIZE * scale

    fun render(ctx: DrawContext): Map<Item, Rect2i> {
        val encoder = gpuDevice.createCommandEncoder()
        encoder.clearColorTexture(gpuTexture, 0) // Transparent
        val pass = encoder.createRenderPass(gpuTexture, OptionalInt.empty())

        val projectionMatrix = RenderSystem.getProjectionMatrix()
        val matrix = Matrix4f().setOrtho(
            0f,
            gpuTexture.getWidth(0).toFloat(),
            gpuTexture.getHeight(0).toFloat(),
            0f,
            1000f,
            21000f
        )

        RenderSystem.setProjectionMatrix(matrix, ProjectionType.ORTHOGRAPHIC)
        ctx.matrices.push()
        ctx.matrices.loadIdentity()
        ctx.matrices.scale(scale.toFloat(), scale.toFloat(), 1f)

        val itemMap = Reference2ObjectOpenHashMap<Item, Rect2i>(items.size())

        items.forEachIndexed { idx, item ->
            val x = (idx % itemsPerDimension) * 16
            val y = (idx / itemsPerDimension) * 16
            ctx.drawItem(item.defaultStack, x, y)
            itemMap[item] = Rect2i(x * scale, y * scale, itemPixelSize, itemPixelSize)
        }

        ctx.matrices.pop()
        pass.close()
        RenderSystem.setProjectionMatrix(projectionMatrix, ProjectionType.ORTHOGRAPHIC)

        return itemMap
    }


    fun toNativeImage(): NativeImage {
        val encoder = gpuDevice.createCommandEncoder()
        val buffer = gpuDevice.createBuffer(
            { "ItemAtlasBuffer" },
            BufferType.PIXEL_PACK,
            BufferUsage.STATIC_READ,
            gpuTexture.getWidth(0) * gpuTexture.getHeight(0) * 4,
        )
        encoder.copyTextureToBuffer(gpuTexture, buffer, 0, {}, 0)
        return encoder.readBuffer(buffer).use { view ->
            NativeImage.read(view.data())
        }
    }

    override fun close() {
        gpuTexture.close()
    }

}
