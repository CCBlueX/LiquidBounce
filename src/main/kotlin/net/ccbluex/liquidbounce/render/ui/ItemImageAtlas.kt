package net.ccbluex.liquidbounce.render.ui

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.systems.VertexSorter
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.Vec2i
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.joml.Matrix4f
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.stream.Collectors
import kotlin.math.ceil
import kotlin.math.sqrt

private const val NATIVE_ITEM_SIZE: Int = 16

private class Atlas(
    val map: Map<Item, Pair<Vec2i, Vec2i>>,
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
object ItemImageAtlas: Listenable {

    private var atlas: Atlas? = null

    fun updateAtlas(drawContext: DrawContext) {
        if (this.atlas != null)
            return

        val renderer = ItemFramebufferRenderer(
            Registries.ITEM.stream().collect(Collectors.toList()),
            4
        )

        val items = renderer.render(drawContext)

        val image = renderer.getImage()

        val img = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)

        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val r = image.getRed(x, y).toInt() and 0xFF
                val g = image.getGreen(x, y).toInt() and 0xFF
                val b = image.getBlue(x, y).toInt() and 0xFF
                val a = image.getOpacity(x, y).toInt() and 0xFF

                img.setRGB(x, y, Color(r, g, b, a).rgb)
            }
        }

        renderer.deleteFramebuffer()

        this.atlas = Atlas(items, img, findAliases())
    }

    private fun findAliases(): Map<Identifier, Identifier> {
        val map = hashMapOf<Identifier, Identifier>()

        Registries.BLOCK.forEach {
            val pickUpState = it.getPickStack(mc.world!!, BlockPos.ORIGIN, it.defaultState)

            if (pickUpState.item != it) {
                val blockId = Registries.BLOCK.getId(it)
                val itemId = Registries.ITEM.getId(pickUpState.item)

                map[blockId] = itemId
            }
        }

        return map
    }

    @Suppress("unused")
    val onReload = handler<ResourceReloadEvent> {
        this.atlas = null
    }

    val isAtlasAvailable
        get() = this.atlas != null

    fun resolveAliasIfPresent(name: Identifier): Identifier {
        return atlas!!.aliasMap[name] ?: return name
    }

    fun getItemImage(item: Item): BufferedImage? {
        val atlas = requireNotNull(this.atlas) { "Atlas is not available yet" }
        val (atlasStart, atlasEnd) = atlas.map[item] ?: return null

        return atlas.image.getSubimage(
            atlasStart.x,
            atlasStart.y,
            atlasEnd.x - atlasStart.x,
            atlasEnd.y - atlasStart.y,
        )!!
    }
}


private class ItemFramebufferRenderer(
    val items: List<Item>,
    val scale: Int,
) {
    val itemsPerDimension = ceil(sqrt(items.size.toDouble())).toInt()

    val framebuffer: Framebuffer = run {
        val fb = SimpleFramebuffer(
            NATIVE_ITEM_SIZE * scale * itemsPerDimension,
            NATIVE_ITEM_SIZE * scale * itemsPerDimension,
            true,
            MinecraftClient.IS_SYSTEM_MAC
        )

        fb.setClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        fb
    }

    val itemPixelSizeOnFramebuffer = NATIVE_ITEM_SIZE * scale

    fun render(ctx: DrawContext): Map<Item, Pair<Vec2i, Vec2i>> {
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

        RenderSystem.setProjectionMatrix(matrix4f, VertexSorter.BY_Z)

        val map = this.items.mapIndexed { idx, item ->
            val from = Vec2i(
                (idx % this.itemsPerDimension) * NATIVE_ITEM_SIZE,
                (idx / this.itemsPerDimension) * NATIVE_ITEM_SIZE
            )

            ctx.drawItem(ItemStack(item), from.x, from.y)

            val fbFrom = Vec2i(from.x * this.scale, from.y * this.scale)
            val fbTo = Vec2i(
                fbFrom.x + this.itemPixelSizeOnFramebuffer,
                fbFrom.y + this.itemPixelSizeOnFramebuffer
            )

            item to (fbFrom to fbTo)
        }.associate { it }

        ctx.matrices.pop()

        MinecraftClient.getInstance().framebuffer.beginWrite(true)

        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorter.BY_Z)

        return map
    }

    fun getImage(): NativeImage {
        val ss = NativeImage(this.framebuffer.textureWidth, this.framebuffer.textureHeight, false)

        RenderSystem.bindTexture(this.framebuffer.colorAttachment)

        ss.loadFromTextureImage(0, false)
        ss.mirrorVertically()

        return ss
    }

    fun deleteFramebuffer() {
        this.framebuffer.delete()
    }

}
