package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.shiftHue
import net.ccbluex.liquidbounce.render.withPosition
import net.ccbluex.liquidbounce.utils.client.Curves
import org.apache.commons.lang3.tuple.MutablePair

object ModuleJumpEffect : Module("JumpEffect", Category.RENDER) {
    private val endRadius by floatRange("EndRadius", 0.15F..0.8F, 0F..3F)

    private val innerColor by color("InnerColor", Color4b(0, 255, 4, 0))
    private val outerColor by color("OuterColor", Color4b(0, 255, 4, 89))

    private val animCurve by curve("AnimCurve", Curves.EASE_OUT)

    private val hueOffsetAnim by int("HueOffsetAnim", 63, -360..360)

    private val lifetime by int("Lifetime", 15, 1..30)

    private var circles = arrayListOf<MutablePair<Vec3, Long>>()

    val repeatable = repeatable {
        circles.forEach { it.right += 1 }
        circles.removeIf { it.getRight() >= lifetime }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        renderEnvironmentForWorld(matrixStack) {
            circles.forEach {
                val progress = animCurve.at((it.right + event.partialTicks) / lifetime)
                withPosition(it.left) {
                    drawGradientCircle(
                        endRadius.endInclusive * progress,
                        endRadius.start * progress,
                        animateColor(outerColor, progress),
                        animateColor(innerColor, progress)
                    )
                }
            }
        }

    }


    private fun animateColor(baseColor: Color4b, progress: Float): Color4b {
        val color = baseColor.alpha((baseColor.a * (1 - progress)).toInt())
        if(hueOffsetAnim == 0){
            return color
        }
        return shiftHue(color, (hueOffsetAnim * progress).toInt())
    }

    val onJump = handler<PlayerJumpEvent> { _ ->
        // Adds new circle when the player jumps
        circles.add(MutablePair(Vec3(player.pos), 0L))
    }


}
