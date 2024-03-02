package net.ccbluex.liquidbounce.render.ui

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.engine.UIRenderer
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.util.ScreenshotRecorder
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import java.io.File
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 *
 */
object ItemImageAtlas: Listenable {

    private val onOverlayRender = handler<OverlayRenderEvent> { event ->
        val nItems = Registries.ITEM.size()
//        val size = ceil(sqrt(nItems.toDouble())).toInt()
        val size = 16

        val itemWidth = 16
        val fbWidthPerITem = itemWidth * 4

        val fb = SimpleFramebuffer(
            fbWidthPerITem * size,
            fbWidthPerITem * size,
            true,
            MinecraftClient.IS_SYSTEM_MAC
        )

        fb.setClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        fb.beginWrite(true)

        var idx = 0

        event.context.matrices.push()

        event.context.matrices.loadIdentity()
        event.context.matrices.scale(0.72151124F, 0.5F, 1.0f)

        Registries.ITEM.stream().forEach {
            event.context.drawItem(ItemStack(Items.RED_STAINED_GLASS_PANE), (idx % size) * itemWidth, (idx / size) * itemWidth)

            idx += 1
        }

        event.context.matrices.pop()

        UIRenderer.overlayFramebuffer.beginWrite(true)

        val ss = ScreenshotRecorder.takeScreenshot(fb)

        ss.fillRect(fbWidthPerITem * 3, (fbWidthPerITem + 2) * 3, fbWidthPerITem, (fbWidthPerITem + 2), 0)

        ss.writeTo(File("item.png"))

        fb.draw(fb.textureWidth, fb.textureHeight, false)

        fb.delete()
    }
}
