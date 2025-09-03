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
 */

package net.ccbluex.liquidbounce.integration.theme.component.components.targethud.mode

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.integration.theme.component.components.targethud.TargetHudComponent
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Identifier
import java.util.function.Function
import kotlin.math.min

object NovolineMode : TargetInfoMode("Novoline") {

    private const val HOLD_TICKS = 10

    private var easingHealth = 0f
    private var previousEasingHealth = 0f
    private var alpha = 0
    private var delayCounter = 0
    private var lastTarget: LivingEntity? = null
    private var lastKnownHealth = 0f
    private var lastKnownMax = 1f

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        if (HideAppearance.isHidingNow) {
            return@handler
        }
        val currentTarget = (ModuleKillAura.targetTracker.target ?: ModuleAimbot.targetTracker.target)
            ?.takeIf { it is PlayerEntity } as? PlayerEntity

        var hasActive = false
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

        val entity = lastTarget as? PlayerEntity
        if (alpha > 0 && entity != null) {
            renderTargetHUD(event.context, entity)
        } else if (!hasActive) {
            lastTarget = null
            delayCounter = 0
            easingHealth = 0f
            previousEasingHealth = 0f
        }
    }
    @Suppress("DestructuringDeclarationWithTooManyEntries")
    private fun renderTargetHUD(ctx: DrawContext, entity: PlayerEntity) {
        val nameWidth = mc.textRenderer.getWidth(entity.name.string) * 0.3f
        val width = 106f + nameWidth
        val x = mc.window.scaledWidth * TargetHudComponent.xOffsetRatio
        val y = mc.window.scaledHeight * TargetHudComponent.yOffsetRatio

        fun Color4b.fade() = withAlpha((a * alpha / 255f).toInt())

        ctx.fill(x.toInt(), y.toInt(),
            (x + width).toInt(), (y + 36).toInt(),
            TargetHudComponent.backgroundColor.fade().toARGB())

        listOf(
            floatArrayOf(x - 1, y - 1, x + width + 1, y),
            floatArrayOf(x - 1, y + 36, x + width + 1, y + 37),
            floatArrayOf(x - 1, y, x, y + 36),
            floatArrayOf(x + width, y, x + width + 1, y + 36)
        ).forEach { arr ->
            val (sx, sy, ex, ey) = arr
            ctx.fill(
                sx.toInt(), sy.toInt(),
                ex.toInt(), ey.toInt(),
                TargetHudComponent.borderColor.fade().toARGB()
            )
        }

        drawHealthBar(ctx, width, x, y)
        drawPlayerHead(ctx, x.toInt(), y.toInt())
        drawText(ctx, entity, TargetHudComponent.textColor.fade(), width, x, y)
    }

    private fun updateAnimationStates(entity: LivingEntity?, hasActive: Boolean) {
        val delta = mc.renderTickCounter.getTickDelta(true)

        if (entity != null) {
            val health = entity.getActualHealth(true)
            val max = (entity.maxHealth + entity.absorptionAmount).coerceAtLeast(1f).coerceAtLeast(1f)
            lastKnownHealth = health
            lastKnownMax = max

            easingHealth = (easingHealth + (health - easingHealth) * 0.2f * delta).coerceIn(0f, max)
            previousEasingHealth = (previousEasingHealth +
                (easingHealth - previousEasingHealth) * 0.1f * delta).coerceIn(0f, max)
        } else {
            previousEasingHealth = (previousEasingHealth +
                (easingHealth - previousEasingHealth) * 0.1f * delta).coerceIn(0f, lastKnownMax.coerceAtLeast(1f))
        }

        val targetAlpha = if (hasActive) 255 else 0
        alpha = (alpha + (targetAlpha - alpha) * 0.2f * delta).toInt().coerceIn(0, 255)

        if (!hasActive && alpha <= 0) {
            easingHealth = 0f
            previousEasingHealth = 0f
            delayCounter = 0
        }
    }

    @Suppress("LongParameterList")
    private fun drawText(ctx: DrawContext, target: PlayerEntity, color: Color4b, width: Float, x: Float, y: Float) {
        val max = lastKnownMax.coerceAtLeast(1f)
        val percent = (if (max > 0f) (easingHealth / max * 100).toInt().coerceIn(0, 100) else 0).toString() + "%"
        val percentWidth = mc.textRenderer.getWidth(percent) * 0.3f
        val percentX = x + 38f + ((width - 40f) - percentWidth) / 2f
        val percentY = y + 24f + 4f - (mc.textRenderer.fontHeight * 0.3f / 2f) - 1f

        val name = mc.textRenderer.trimToWidth(target.name.string, ((width - 44f) / 0.3f).toInt())

        ctx.drawText(
            mc.textRenderer,
            name,
            (x + 38f).toInt(),
            (y + 6f).toInt(),
            color.toARGB(),
            false
        )
        val percentColor = Color4b.WHITE.with(a = (Color4b.WHITE.a * alpha / 255f).toInt()).toARGB()
        ctx.drawText(
            mc.textRenderer,
            percent,
            percentX.toInt(),
            percentY.toInt(),
            percentColor,
            false
        )
    }

    private fun drawHealthBar(ctx: DrawContext, width: Float, x: Float, y: Float) {

        val barX = x + 38f
        val barY = y + 24f
        val barW = (width - 40f).coerceAtLeast(0f)

        val maxHealth = lastKnownMax.coerceAtLeast(1f)
        val currentHealth = (easingHealth / maxHealth).coerceIn(0f, 1f) * barW
        val previousHealth = (previousEasingHealth / maxHealth).coerceIn(0f, 1f) * barW

        val (start, end) = when (val mode = TargetHudComponent.colorModes.activeChoice) {
            is GenericStaticColorMode,
            is GenericRainbowColorMode -> mode.getColors(mc.player).first.let { it to it }
            else -> mode.getColors(mc.player)
        }

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
        val id = mc.skinProvider.getSkinTextures(target.gameProfile).texture()
        val centerX = x + 8f
        val centerY = y + 8f
        val alphaMask = (alpha.coerceIn(0, 255) shl 24) or 0xFFFFFF

        ctx.matrices.push()
        ctx.matrices.translate(centerX + 1, centerY + 1, 0f)
        ctx.matrices.scale(4.5f, 4.5f, 1f)
        ctx.matrices.translate(-centerX, -centerY, 0f)

        val layer = Function<Identifier, RenderLayer> { RenderLayer.getGuiTextured(it) }

        ctx.drawTexture(layer,
            id, (x + 6), (y + 6),
            8f,
            8f,
            8,
            8,
            64,
            64,
            alphaMask)
        ctx.drawTexture(layer,
            id, (x + 6), (y + 6),
            40f,
            8f,
            8,
            8,
            64,
            64,
            alphaMask)

        ctx.matrices.pop()
    }
}
