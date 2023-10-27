package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.shiftHue
import net.ccbluex.liquidbounce.render.withPosition
import net.ccbluex.liquidbounce.utils.client.Curves
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.util.math.Vec3d
import org.apache.commons.lang3.tuple.MutablePair

object ModuleJumpEffect : Module("JumpEffect", Category.RENDER) {
    private val endRadius by floatRange("EndRadius", 0.4F..0.9F, 0.1F..3F)

    private val innerColor by color("InnerColor", Color4b.BLUE.alpha(0))
    private val outerColor by color("OuterColor", Color4b.BLUE)
    private val rainbow by boolean("Rainbow", false)

    private val animCurve by curve("AnimCurve", Curves.LINEAR)

    private val hueOffsetAnim by int("hueOffsetAnim", 0, 0..360)

    private val lifetime by int("Lifetime", 10, 1..30)

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

//    private fun curve(t: Float) =
//        t * t * t


    private fun animateColor(baseColor: Color4b, progress: Float): Color4b {
        val color = baseColor.alpha((baseColor.a * (1 - progress)).toInt())
        if(hueOffsetAnim == 0){
            return color
        }
        return shiftHue(color, (hueOffsetAnim * progress).toInt())
    }

    val onJump = handler<PlayerJumpEvent> { event ->
        // Add new circle when the player jumps
        circles.add(MutablePair(Vec3(player.pos), 0L))
    }


}
