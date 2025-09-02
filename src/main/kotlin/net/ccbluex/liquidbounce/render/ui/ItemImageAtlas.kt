package net.ccbluex.liquidbounce.render.ui

import com.mojang.blaze3d.systems.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.ccbluex.liquidbounce.common.GlobalFramebuffer
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.toBufferedImage
import net.ccbluex.liquidbounce.utils.render.toNativeImage
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.util.math.Rect2i
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.joml.Matrix4f
import org.joml.Vector2i
import java.awt.image.BufferedImage
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

        val image = renderer.getImage().toBufferedImage()

        renderer.deleteFramebuffer()

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
) : MinecraftShortcuts {
    private val itemsPerDimension = ceil(sqrt(items.size().toDouble())).toInt()

    private val framebuffer: Framebuffer = SimpleFramebuffer(
        NATIVE_ITEM_SIZE * scale * itemsPerDimension,
        NATIVE_ITEM_SIZE * scale * itemsPerDimension,
        true
    ).apply {
        setClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    }

    private val itemPixelSizeOnFramebuffer = NATIVE_ITEM_SIZE * scale

    fun render(ctx: DrawContext): Map<Item, Rect2i> {
        this.framebuffer.beginWrite(true)

        ctx.matrices.push()

        ctx.matrices.loadIdentity()

        ctx.matrices.scale(scale.toFloat(), scale.toFloat(), 1.0f)

        val projectionMatrix = RenderSystem.getProjectionMatrix()

        val matrix4f = Matrix4f().setOrtho(
            0.0f,
            this.framebuffer.textureWidth.toFloat(),
            this.framebuffer.textureHeight.toFloat(),
            0.0f,
            1000.0f,
            21000.0f
        )

        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.ORTHOGRAPHIC)
        GlobalFramebuffer.push(framebuffer)

        val map = Reference2ObjectOpenHashMap<Item, Rect2i>(items.size())
        this.items.forEachIndexed { idx, item ->
            val fromX = (idx % this.itemsPerDimension) * NATIVE_ITEM_SIZE
            val fromY = (idx / this.itemsPerDimension) * NATIVE_ITEM_SIZE

            ctx.drawItem(item.defaultStack, fromX, fromY)

            map[item] = Rect2i(
                fromX * this.scale,
                fromY * this.scale,
                this.itemPixelSizeOnFramebuffer,
                this.itemPixelSizeOnFramebuffer
            )
        }

        ctx.matrices.pop()

        GlobalFramebuffer.pop()
        mc.framebuffer.beginWrite(true)

        RenderSystem.setProjectionMatrix(projectionMatrix, ProjectionType.ORTHOGRAPHIC)

        return map
    }

    fun getImage(): NativeImage = framebuffer.toNativeImage()

    fun deleteFramebuffer() {
        this.framebuffer.delete()
    }

}
