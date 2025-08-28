package net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.modes

import it.unimi.dsi.fastutil.objects.ObjectLongMutablePair
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.JumpEffectMode
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.ModuleJumpEffect.colorMode
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.utils.shiftHue
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.util.math.Vec3d

object RenderMode : JumpEffectMode("Render") {

    private val endRadius by floatRange("EndRadius", 0.9F..1F, 0F..3F)
    private val innerAlpha by int("InnerAlpha", 0, 0..255)
    private val outerAlpha by int("OuterAlpha", 91, 0..255)
    private val animCurve by easing("AnimCurve", Easing.QUAD_OUT)
    private val hueOffsetAnim by int("HueOffsetAnim", 0, -360..360)
    private val lifetime by int("Lifetime", 30, 1..30)
    private val circles = ArrayDeque<ObjectLongMutablePair<Vec3d>>()

    override fun enable() {
        circles.clear()
    }

    val repeatable = tickHandler {
        with(circles.iterator()) {
            while (hasNext()) {
                val pair = next()
                val newValue = pair.valueLong() + 1L
                if (newValue >= lifetime) {
                    remove()
                    continue
                }
                pair.value(newValue)
            }
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        renderEnvironmentForWorld(event.matrixStack) {
            circles.forEach {
                val progress = animCurve
                    .transform((it.valueLong() + event.partialTicks) / lifetime)
                    .coerceIn(0f..1f)

                val (outerColorBase, innerColorBase) = colorMode.activeChoice.getColors(null)
                val outerColor = animateColor(outerColorBase.withAlpha(outerAlpha), progress)
                val innerColor = animateColor(innerColorBase.withAlpha(innerAlpha), progress)

                withPositionRelativeToCamera(it.key()) {
                    drawGradientCircle(
                        endRadius.endInclusive * progress,
                        endRadius.start * progress,
                        outerColor,
                        innerColor
                    )
                }
            }
        }
    }

    private fun animateColor(baseColor: Color4b, progress: Float): Color4b {
        val color = baseColor.fade(1.0F - progress)
        return if (hueOffsetAnim == 0) color else shiftHue(color, (hueOffsetAnim * progress).toInt())
    }

    @Suppress("unused")
    val onJump = handler<PlayerJumpEvent> {
        circles.add(ObjectLongMutablePair(player.pos, 0L))
    }
}
