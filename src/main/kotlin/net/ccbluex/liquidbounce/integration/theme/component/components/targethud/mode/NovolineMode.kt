package net.ccbluex.liquidbounce.integration.theme.component.components.targethud.mode

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.integration.theme.component.components.targethud.TargetHudComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.targethud.TargetHudComponent.applyAdaptiveScale
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.processor.TextProcessor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.SkinTextures
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Identifier
import java.util.function.Function
import kotlin.math.min

object NovolineMode : TargetHudMode("Novoline") {
    private val width by float("Width",64f,40f..100f)
    private val animationTime by int("AnimTime", 2, 1..5,"tick")

    private const val HOLD_TICKS = 10
    private val fontRenderer
        get() = FontManager.FONT_RENDERER

    private var scaleAnim = 0f
    private var easingHealth = 0f
    private var previousEasingHealth = 0f
    private var alpha = 0
    private var delayCounter = 0
    private var lastTarget: LivingEntity? = null
    private var lastKnownHealth = 0f
    private var lastKnownMax = 1f

    private fun selectTargetRender(): Pair<PlayerEntity?, Boolean> {
        if (HideAppearance.isHidingNow) {
            return null to false
        }
        val currentTarget = (ModuleKillAura.targetTracker.target ?: ModuleAimbot.targetTracker.target)
            ?.takeIf { it is PlayerEntity } as? PlayerEntity

        var hasActive: Boolean
        if (currentTarget != null) {
            lastTarget = currentTarget
            lastKnownHealth = currentTarget.getActualHealth(true)
            lastKnownMax = (currentTarget.maxHealth + currentTarget.absorptionAmount).coerceAtLeast(1f)
            delayCounter = 0
            hasActive = true
        } else if (lastTarget != null && delayCounter++ < HOLD_TICKS) {
            hasActive = true
        } else {
            hasActive = false
        }

        updateAnimationStates(lastTarget, hasActive)

        return if (alpha > 0 && lastTarget is PlayerEntity) {
            lastTarget as PlayerEntity to true
        } else {
            if (!hasActive) {
                lastTarget = null
                delayCounter = 0
                easingHealth = 0f
                previousEasingHealth = 0f
            }
            null to false
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val (entity, shouldRender) = selectTargetRender()
        if (shouldRender && entity != null) {
            renderTargetHUD(event.context, entity)
        }
    }

    @Suppress("DestructuringDeclarationWithTooManyEntries")
    private fun renderTargetHUD(ctx: DrawContext, entity: PlayerEntity) {
        val nameProcessedForWidth = fontRenderer.process(
            entity.name.string,
            TargetHudComponent.textColor.fade(0.1f)
        )
        val nameWidth = (fontRenderer.getStringWidth(nameProcessedForWidth) * 0.3f)
        val baseW = width + nameWidth
        val baseH = 36f

        applyAdaptiveScale(baseW, baseH) { scale, cx, cy ->
            val finalScale = scale * (0.8f + 0.2f * scaleAnim)
            ctx.matrices.push()
            ctx.matrices.translate(cx, cy, 0f)
            ctx.matrices.scale(finalScale, finalScale, 1f)
            ctx.matrices.translate(-baseW / 2f, -baseH / 2f, 0f)
            val x = 0f
            val y = 0f
            val w = baseW
            val h = baseH

            fun Color4b.fade() = withAlpha((a * alpha / 255f).toInt())

            ctx.fill(
                x.toInt(), y.toInt(),
                (x + w).toInt(), (y + h).toInt(),
                TargetHudComponent.backgroundColor.fade().toARGB()
            )

            listOf(
                floatArrayOf(x - 1, y - 1, x + w + 1, y),
                floatArrayOf(x - 1, y + h, x + w + 1, y + h + 1),
                floatArrayOf(x - 1, y, x, y + h),
                floatArrayOf(x + w, y, x + w + 1, y + h)
            ).forEach { arr ->
                val (sx, sy, ex, ey) = arr
                ctx.fill(
                    sx.toInt(), sy.toInt(),
                    ex.toInt(), ey.toInt(),
                    TargetHudComponent.borderColor.fade().toARGB()
                )
            }

            drawHealthBar(ctx, w, x, y)
            drawPlayerHead(ctx, x.toInt(), y.toInt())
            drawText(entity, TargetHudComponent.textColor.fade(), w, x, y, scale, cx, cy, baseW, baseH)

            ctx.matrices.pop()
        }
    }

    @Suppress("LongParameterList")
    private fun drawText(
        target: PlayerEntity,
        color: Color4b,
        width: Float,
        x: Float,
        y: Float,
        scale: Float,
        cx: Float,
        cy: Float,
        baseW: Float,
        baseH: Float
    ) {
        val max = lastKnownMax.coerceAtLeast(1f)
        val percent = (if (max > 0f) (easingHealth / max * 100).toInt().coerceIn(0, 100) else 0).toString() + "%"

        val percentColor = Color4b.WHITE.with(a = (Color4b.WHITE.a * alpha / 255f).toInt())
        val processedPercent = fontRenderer.process(percent, percentColor)
        val percentUnscaledW = fontRenderer.getStringWidth(processedPercent)
        val percentLocalW = percentUnscaledW * 0.3f
        val percentLocalX = x + 38f + ((width - 40f) - percentLocalW) / 2f
        val percentLocalY = y + 24f + 4f - (fontRenderer.height * 0.3f / 2f) - 1f

        val maxNameUnscaled = (width - 44f) / 0.3f
        var nameStr = target.gameProfile?.name ?: target.name.string
        while (nameStr.isNotEmpty() && fontRenderer.getStringWidth(
                fontRenderer.process(nameStr, color)) > maxNameUnscaled) {
            nameStr = nameStr.substring(0, nameStr.length - 1)
        }
        val processedName = fontRenderer.process(nameStr, color)

        val nameLocalX = x + 38f
        val nameLocalY = y + 6f

        val finalScale = scale * (0.8f + 0.2f * scaleAnim)

        renderEnvironmentForGUI {
            fontRenderer.withBuffers { buf ->
                fun drawTextWithTransform(text: TextProcessor.ProcessedText, localX: Float, localY: Float) {
                    matrixStack.push()

                    matrixStack.translate(cx, cy, 0f)

                    matrixStack.scale(finalScale, finalScale, 1f)

                    matrixStack.translate(localX - baseW / 2f, localY - baseH / 2f, 0f)
                    matrixStack.scale(0.3f, 0.3f, 1f)
                    fontRenderer.draw(text, 0f, 0f, shadow = false, z = 0.001f)
                    fontRenderer.commit(this@renderEnvironmentForGUI, buf)
                    matrixStack.pop()
                }

                drawTextWithTransform(processedName, nameLocalX, nameLocalY)
                drawTextWithTransform(processedPercent, percentLocalX, percentLocalY)
            }
        }
    }


    private fun updateAnimationStates(entity: LivingEntity?, hasActive: Boolean) {
        val delta = mc.renderTickCounter.getTickDelta(true)

        if (entity != null) {
            val health = entity.getActualHealth(true)
            val max = (entity.maxHealth + entity.absorptionAmount).coerceAtLeast(1f)
            lastKnownHealth = health
            lastKnownMax = max

            easingHealth = (easingHealth + (health - easingHealth) * 0.2f * delta).coerceIn(0f, max)
            previousEasingHealth = (previousEasingHealth +
                (easingHealth - previousEasingHealth) * 0.1f * delta).coerceIn(0f, max)
        } else {
            previousEasingHealth = (previousEasingHealth +
                (easingHealth - previousEasingHealth) * 0.1f * delta).coerceIn(0f, lastKnownMax.coerceAtLeast(1f))
        }

        val animSpeed = (1f / animationTime) * delta

        val targetAlpha = if (hasActive) 255 else 0
        alpha = (alpha + (targetAlpha - alpha) * animSpeed).toInt().coerceIn(0, 255)

        val targetScale = if (hasActive) 1f else 0f
        scaleAnim += (targetScale - scaleAnim) * animSpeed

        if (!hasActive && alpha <= 0) {
            easingHealth = 0f
            previousEasingHealth = 0f
            delayCounter = 0
            scaleAnim = 0f
        }
    }

    private fun drawHealthBar(ctx: DrawContext, width: Float, x: Float, y: Float) {
        val barX = x + 38f
        val barY = y + 24f
        val barW = (width - 40f).coerceAtLeast(0f)

        val maxHealth = lastKnownMax.coerceAtLeast(1f)
        val currentHealth = (easingHealth / maxHealth).coerceIn(0f, 1f) * barW
        val previousHealth = (previousEasingHealth / maxHealth).coerceIn(0f, 1f) * barW

        val (start, end) = TargetHudComponent.colorModes.activeChoice.getColors(mc.player)

        ctx.fill(
            barX.toInt(), barY.toInt(), (barX + barW).toInt(), (barY + 8f).toInt(),
            Color4b(40, 40, 40, (40 * alpha / 255f).toInt()).toARGB()
        )

        if (previousHealth != currentHealth) {
            val fadeStartX = barX + min(previousHealth, currentHealth)
            val fadeWidth = (previousHealth - currentHealth).coerceIn(-barW, barW)
            ctx.fillGradient(
                fadeStartX.toInt(),
                barY.toInt(),
                (fadeStartX + fadeWidth).toInt(), (barY + 8f).toInt(), 0,
                start.with(a = (start.a * alpha / 255f * 0.5f).toInt()).toARGB(),
                end.with(a = (end.a * alpha / 255f * 0.5f).toInt()).toARGB()
            )
        }

        ctx.fillGradient(
            barX.toInt(),
            barY.toInt(),
            (barX + currentHealth).toInt(),
            (barY + 8f).toInt(), 0,
            start.with(a = (start.a * alpha / 255f).toInt()).toARGB(),
            end.with(a = (end.a * alpha / 255f).toInt()).toARGB()
        )
    }

    private fun drawPlayerHead(ctx: DrawContext, x: Int, y: Int) {
        val target = lastTarget as? PlayerEntity ?: return
        val skinId: SkinTextures = (target as? AbstractClientPlayerEntity)?.skinTextures ?: return
        val centerX = x + 8f
        val centerY = y + 8f
        val alphaMask = (alpha.coerceIn(0, 255) shl 24) or 0xFFFFFF

        ctx.matrices.push()
        ctx.matrices.translate(centerX + 1, centerY + 1, 0f)
        ctx.matrices.scale(4.5f, 4.5f, 1f)
        ctx.matrices.translate(-centerX, -centerY, 0f)

        val layer = Function<Identifier, RenderLayer> { RenderLayer.getGuiTextured(it) }
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.setShaderTexture(0, skinId.texture)

        ctx.drawTexture(
            layer,
            skinId.texture, (x + 6), (y + 6),
            8f,
            8f,
            8,
            8,
            64,
            64,
            alphaMask
        )
        ctx.drawTexture(
            layer,
            skinId.texture, (x + 6), (y + 6),
            40f,
            8f,
            8,
            8,
            64,
            64,
            alphaMask
        )

        ctx.matrices.pop()
    }
}
