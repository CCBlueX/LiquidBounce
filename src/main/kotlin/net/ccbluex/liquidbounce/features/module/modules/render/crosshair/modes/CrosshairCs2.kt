package net.ccbluex.liquidbounce.features.module.modules.render.crosshair.modes

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.CrosshairColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.CrosshairMode
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.util.Mth

object CrosshairCS2 : CrosshairMode("CS2") {

    private object CrosshairSettings : ValueGroup("Crosshair") {
        val length by float("Length", 5f, 1f..20f)
        val thickness by float("Thickness", 1f, 0.5f..5f)
        val gap by float("Gap", 2f, 0f..10f)
        val dynamicMultiplier by float("DynamicMultiplier", 1f, 0f..10f)
    }

    private val color = CrosshairColorSettings()

    init {
        tree(CrosshairSettings)
        tree(color)
    }

    override fun OverlayRenderEvent.drawCrosshair() {
        val multiplier = dynamicCrosshair(CrosshairSettings.dynamicMultiplier)
        val length = CrosshairSettings.length
        val thickness = CrosshairSettings.thickness
        val gap = CrosshairSettings.gap + multiplier

        // 获取颜色并转换成 Color4b
        val argb = color.getCurrentStepColor(
            color.firstColor,
            color.secondColor,
            color.syncColors,
            color.spinSpeed,
            0f
        ).argb

        val color4b = Color4b(
            (argb shr 16) and 0xFF, // Red
            (argb shr 8) and 0xFF,  // Green
            argb and 0xFF,           // Blue
            (argb shr 24) and 0xFF   // Alpha
        )

        // 上线
        context.drawQuad(
            -thickness / 2f, -gap - length,
            thickness / 2f, -gap,
            fillColor = color4b
        )
        // 下线
        context.drawQuad(
            -thickness / 2f, gap,
            thickness / 2f, gap + length,
            fillColor = color4b
        )
        // 左线
        context.drawQuad(
            -gap - length, -thickness / 2f,
            -gap, thickness / 2f,
            fillColor = color4b
        )
        // 右线
        context.drawQuad(
            gap, -thickness / 2f,
            gap + length, thickness / 2f,
            fillColor = color4b
        )
    }

    private fun OverlayRenderEvent.dynamicCrosshair(multiplier: Float): Float {
        return if (Mth.equal(0f, multiplier)) 0f
        else {
            val cooldown = player.getAttackStrengthScale(tickDelta)
            multiplier * (1f - cooldown)
        }
    }
}
