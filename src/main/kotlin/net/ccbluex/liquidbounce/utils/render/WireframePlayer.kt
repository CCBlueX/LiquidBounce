package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.BoxRenderer
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.joml.Quaternionf

// pixels / (16 + 16)
val LIMB = Box(0.0, 0.0, 0.0, 0.125, 0.375, 0.125)
val BODY = Box(0.0, 0.0, 0.0, 0.25, 0.375, 0.125)
val HEAD = Box(0.0, 0.0, 0.0, 0.25, 0.25, 0.25)

val RENDER_LEFT_LEG: Box = LIMB.offset(-LIMB.maxX, 0.0, 0.0)
val RENDER_RIGHT_LEG: Box = LIMB
val RENDER_BODY: Box = BODY.offset(-LIMB.maxX, LIMB.maxY, 0.0)
val RENDER_LEFT_ARM: Box = LIMB.offset(-2 * LIMB.maxX, LIMB.maxY, 0.0)
val RENDER_RIGHT_ARM: Box = LIMB.offset(BODY.maxX - LIMB.maxX, LIMB.maxY, 0.0)
val RENDER_HEAD: Box = HEAD.offset(-LIMB.maxX, LIMB.maxY * 2, -HEAD.maxZ * 0.25)

data class WireframePlayer(var pos: Vec3d, var yaw: Float, var pitch: Float) {

    fun render(event: WorldRenderEvent, color: Color4b, outlineColor: Color4b) {
        renderEnvironmentForWorld(event.matrixStack) {
            withPositionRelativeToCamera(pos) {
                val matrix = matrixStack.peek().positionMatrix
                matrix.rotate(Quaternionf().rotationY(Math.toRadians(-MathHelper.wrapDegrees(yaw.toDouble())).toFloat()))
                matrix.scale(1.9f)

                BoxRenderer.drawWith(this) {
                    drawBox(RENDER_LEFT_LEG, color, outlineColor)
                    drawBox(RENDER_RIGHT_LEG, color, outlineColor)
                    drawBox(RENDER_BODY, color, outlineColor)
                    drawBox(RENDER_LEFT_ARM, color, outlineColor)
                    drawBox(RENDER_RIGHT_ARM, color, outlineColor)

                    matrix.translate(0f, RENDER_HEAD.minY.toFloat(), 0f)
                    matrix.rotate(Quaternionf().rotationX(Math.toRadians(pitch.toDouble()).toFloat()))
                    matrix.translate(0f, -RENDER_HEAD.minY.toFloat(), 0f)

                    drawBox(RENDER_HEAD, color, outlineColor)
                }
            }
        }
    }

    fun setPosRot(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        this.pos = Vec3d(x, y, z)
        this.yaw = yaw
        this.pitch = pitch
    }

}
