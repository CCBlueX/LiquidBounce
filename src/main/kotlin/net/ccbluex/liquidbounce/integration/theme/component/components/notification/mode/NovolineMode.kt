package net.ccbluex.liquidbounce.integration.theme.component.components.notification.mode

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent.Severity.*
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.integration.theme.component.components.applyAdaptiveScale
import net.ccbluex.liquidbounce.integration.theme.component.components.notification.NotificationComponent.alignment
import net.ccbluex.liquidbounce.integration.theme.component.components.notification.NotificationComponent.backgroundColor
import net.ccbluex.liquidbounce.integration.theme.component.components.notification.NotificationComponent.size
import net.ccbluex.liquidbounce.integration.theme.component.components.notification.NotificationMode
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats

object NovolineMode : NotificationMode("Novoline") {
    private val animationTime by int("AnimTime", 5, 1..10, "tick")
    private val fontRenderer
        get() = FontManager.FONT_RENDERER

    private val successTexture = "image/hud/notification/success.png".registerAsDynamicImageFromClientResources()
    private val disabledTexture = "image/hud/notification/disabled.png".registerAsDynamicImageFromClientResources()
    private val errorTexture = "image/hud/notification/error.png".registerAsDynamicImageFromClientResources()
    private val infoTexture = "image/hud/notification/info.png".registerAsDynamicImageFromClientResources()

    private data class NotificationData(
        val title: String,
        val message: String,
        val severity: NotificationEvent.Severity,
        val animationKey: Long,
        var alpha: Float = 0f,
        var xOffset: Float = 0f,
        val startTime: Long = System.currentTimeMillis()
    )

    private val notifications = mutableListOf<NotificationData>()

    @Suppress("unused")
    val renderHandler = handler<NotificationEvent> { event ->
        val animationKey = System.currentTimeMillis()

        if (event.severity == ENABLED || event.severity == DISABLED) {
            notifications.removeIf { it.message == event.message && it.severity in listOf(ENABLED, DISABLED) }
        }

        notifications.add(0, NotificationData(event.title, event.message, event.severity, animationKey))
    }

    @Suppress("unused")
    val overlayHandler = handler<OverlayRenderEvent> { event ->
        updateAnimations()
        renderNotifications(event.context)
    }

    private fun updateAnimations() {
        val animSpeed = (1f / animationTime)
        val now = System.currentTimeMillis()

        notifications.forEach { notification ->
            val elapsed = (System.currentTimeMillis() - notification.startTime) / 1000f
            notification.alpha = if (elapsed < 0.2f) {
                (notification.alpha + animSpeed).coerceIn(0f, 1f)
            } else if (elapsed > 2.8f) {
                (notification.alpha - animSpeed).coerceIn(0f, 1f)
            } else {
                1f
            }
            notification.xOffset = if (elapsed < 0.2f) {
                (notification.xOffset - (30f * animSpeed)).coerceIn(0f, 30f)
            } else if (elapsed > 2.8f) {
                (notification.xOffset + (30f * animSpeed)).coerceIn(0f, 30f)
            } else {
                0f
            }
        }

        notifications.removeAll { it.alpha <= 0f || (now - it.startTime) > 3000 }
    }

    private fun renderNotifications(ctx: DrawContext) {
        val baseWidth = 300f
        val baseHeight = 52f
        var offsetY = 0f

        notifications.forEach { notification ->
            applyAdaptiveScale(size, baseWidth, baseHeight, alignment) { scale, cx, cy ->
                renderSingleNotification(ctx, notification, baseWidth, baseHeight, scale, cx, cy - offsetY)
                offsetY += baseHeight * scale + 6f
            }
        }
    }

    private fun renderSingleNotification(
        ctx: DrawContext,
        notification: NotificationData,
        width: Float,
        height: Float,
        scale: Float,
        cx: Float,
        cy: Float,
    ) {
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)

        ctx.matrices.push()
        ctx.matrices.translate(cx + notification.xOffset, cy, 0f)
        ctx.matrices.scale(scale, scale, 1f)
        ctx.matrices.translate(-width / 2f, -height / 2f, 0f)

        ctx.fill(
            0, 0, width.toInt(), height.toInt(),
            backgroundColor.withAlpha(70 * notification.alpha.toInt()).toARGB()
        )
        ctx.matrices.pop()

        val iconTexture = when (notification.severity) {
            SUCCESS -> successTexture
            ENABLED -> successTexture
            ERROR -> errorTexture
            DISABLED -> disabledTexture
            INFO -> infoTexture
            BLINK -> infoTexture
            BLINKED -> infoTexture
            BLINKING -> infoTexture
        }

        renderEnvironmentForGUI {
            matrixStack.push()
            matrixStack.translate(cx + notification.xOffset, cy, 0f)
            matrixStack.scale(scale, scale, 1f)
            matrixStack.translate(-width / 2f, -height / 2f, 0f)

            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.disableCull()
            val shader = mc.shaderLoader.getOrCreateProgram(ShaderProgramKeys.POSITION_TEX_COLOR)!!
            RenderSystem.setShader(shader)
            RenderSystem.setShaderTexture(0, iconTexture)
            RenderSystem.setShaderColor(1f, 1f, 1f, notification.alpha)

            matrixStack.push()
            matrixStack.translate(10f, (height - 40f) / 2f, 0f)
            matrixStack.scale(40f / 64f, 40f / 64f, 1f)

            drawCustomMesh(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR,
                ShaderProgramKeys.POSITION_TEX_COLOR
            ) { mat ->
                vertex(mat, 0f, 0f, 0f).texture(0f, 0f).color(255, 255, 255, (255 * notification.alpha).toInt())
                vertex(mat, 64f, 0f, 0f).texture(1f, 0f).color(255, 255, 255, (255 * notification.alpha).toInt())
                vertex(mat, 64f, 64f, 0f).texture(1f, 1f).color(255, 255, 255, (255 * notification.alpha).toInt())
                vertex(mat, 0f, 64f, 0f).texture(0f, 1f).color(255, 255, 255, (255 * notification.alpha).toInt())
            }

            matrixStack.pop()

            fontRenderer.withBuffers { buf ->
                val fontScale = 0.3f
                val textStartX = 10f + 40f + 10f // = 60f

                matrixStack.push()
                matrixStack.translate(textStartX, 0f, 0f)
                matrixStack.scale(fontScale, fontScale, 1f)
                val titleColor = Color4b(255, 255, 255, (255 * notification.alpha).toInt())
                val processedTitle = fontRenderer.process(notification.title, titleColor)
                fontRenderer.draw(processedTitle, 0f, 12f / fontScale, shadow = false, z = 0.001f)
                fontRenderer.commit(this@renderEnvironmentForGUI, buf)
                matrixStack.pop()

                matrixStack.push()
                matrixStack.translate(textStartX, 0f, 0f)
                matrixStack.scale(fontScale, fontScale, 1f)
                val messageColor = Color4b(203, 209, 227, (255 * notification.alpha).toInt())
                val processedMsg = fontRenderer.process(notification.message, messageColor)
                fontRenderer.draw(processedMsg, 0f, 30f / fontScale, shadow = false, z = 0.001f)
                fontRenderer.commit(this@renderEnvironmentForGUI, buf)
                matrixStack.pop()
            }

            matrixStack.pop()
        }

        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
    }
}
