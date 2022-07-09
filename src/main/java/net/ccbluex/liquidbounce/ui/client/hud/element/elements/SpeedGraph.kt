/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.easeOutCubic
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import kotlin.math.hypot

/**
 * CustomHUD Speed-graph element
 *
 * Allows to draw custom speed graph
 * TODO: Auto-scaling
 */
@ElementInfo(name = "SpeedGraph")
class SpeedGraph(x: Double = 75.0, y: Double = 110.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)) : Element(x, y, scale, side)
{
    private val widthValue = IntegerValue("Width", 150, 100, 300)
    private val heightValue = IntegerValue("Height", 50, 30, 300)

    private val speedGroup = ValueGroup("Speed")
    private val speedScaleFadeSpeedValue = IntegerValue("ScaleFadeSpeed", 2, 1, 9)
    private val speedMultiplier = FloatValue("Multiplier", 7F, 1F, 50F, "Speed-yMultiplier")
    private val speedThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "Speed-Thickness")
    private val speedColorValue = RGBColorValue("Color", 0, 255, 72, Triple("Speed-R", "Speed-G", "Speed-B"))

    private val yspeedGroup = ValueGroup("YSpeed")
    private val yspeedScaleFadeSpeedValue = IntegerValue("ScaleFadeSpeed", 2, 1, 9)
    private val yspeedMultiplier = FloatValue("Multiplier", 7F, 1F, 50F, "YSpeed-yMultiplier")
    private val yspeedPos = FloatValue("Offset", 20F, 0F, 150F, "YSpeed-yPos")
    private val yspeedThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "YSpeed-Thickness")
    private val yspeedColorValue = RGBColorValue("Color", 0, 0, 255, Triple("YSpeed-R", "YSpeed-G", "YSpeed-B"))

    private val timerGroup = ValueGroup("Timer")
    private val timerEnabled = BoolValue("Enabled", true, "Timer")
    private val timerScaleFadeSpeedValue = IntegerValue("ScaleFadeSpeed", 2, 1, 9)
    private val timerYMultiplier = FloatValue("Multiplier", 7F, 1F, 50F, "Timer-yMultiplier")
    private val timerThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "Timer-Thickness")
    private val timerColorValue = RGBColorValue("Color", 111, 0, 255, Triple("Timer-R", "Timer-G", "Timer-B"))

    private val motionGroup = ValueGroup("Motion")
    private val motionEnabled = BoolValue("Enabled", true, "Motion")
    private val motionScaleFadeSpeedValue = IntegerValue("ScaleFadeSpeed", 2, 1, 9)
    private val motionyMultiplier = FloatValue("Multiplier", 7F, 1F, 50F, "Motion-yMultiplier")
    private val motionThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "Motion-Thickness")
    private val motionColorValue = RGBColorValue("Color", 0, 255, 180, Triple("Motion-R", "Motion-G", "Motion-B"))

    private val ymotionGroup = ValueGroup("YMotion")
    private val ymotionEnabled = BoolValue("Enabled", true, "YMotion")
    private val ymotionScaleFadeSpeedValue = IntegerValue("ScaleFadeSpeed", 2, 1, 9)
    private val ymotionYMultiplier = FloatValue("Multiplier", 7F, 1F, 50F, "YMotion-yMultiplier")
    private val ymotionYPos = FloatValue("Offset", 20F, 0F, 150F, "YMotion-yPos")
    private val ymotionThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "YMotion-Thickness")
    private val ymotionColorValue = RGBColorValue("Color", 0, 180, 255, Triple("YMotion-R", "YMotion-G", "YMotion-B"))

    private val speedList = ArrayList<Double>()
    private var speedPeak = 0.0
    private var speedScale = 0.0f

    private val yspeedList = ArrayList<Double>()
    private var yspeedPeak = 0.0
    private var yspeedScale = 0.0f

    private val timerList = ArrayList<Float>()
    private var timerPeak = 0.0f
    private var timerScale = 0.0f

    private val motionList = ArrayList<Double>()
    private var motionPeak = 0.0
    private var motionScale = 0.0f

    private val ymotionList = ArrayList<Double>()
    private var ymotionPeak = 0.0
    private var ymotionScale = 0.0f

    private var lastTick = -1

    init
    {
        speedGroup.addAll(speedScaleFadeSpeedValue, speedMultiplier, speedThicknessValue, speedColorValue)
        yspeedGroup.addAll(yspeedScaleFadeSpeedValue, yspeedMultiplier, yspeedPos, yspeedThicknessValue, yspeedColorValue)
        timerGroup.addAll(timerEnabled, timerScaleFadeSpeedValue, timerYMultiplier, timerThicknessValue, timerColorValue)
        motionGroup.addAll(motionEnabled, motionScaleFadeSpeedValue, motionyMultiplier, motionThicknessValue, motionColorValue)
        ymotionGroup.addAll(ymotionEnabled, ymotionScaleFadeSpeedValue, ymotionYMultiplier, ymotionYPos, ymotionThicknessValue, ymotionColorValue)
    }

    override fun drawElement(): Border?
    {
        val thePlayer = mc.thePlayer ?: return null

        val width = widthValue.get()
        val height = heightValue.get().toFloat()

        val timerEnabled = timerEnabled.get()
        val motionEnabled = motionEnabled.get()
        val ymotionEnabled = ymotionEnabled.get()

        if (lastTick != thePlayer.ticksExisted)
        {
            lastTick = thePlayer.ticksExisted

            speedList.add(hypot(thePlayer.posX - thePlayer.prevPosX, thePlayer.posZ - thePlayer.prevPosZ))
            yspeedList.add(thePlayer.posY - thePlayer.prevPosY)

            while (speedList.size > width) speedList.removeAt(0)
            while (yspeedList.size > width) yspeedList.removeAt(0)

            speedPeak = speedList.max() ?: 0.0
            yspeedPeak = yspeedList.max() ?: 0.0

            if (timerEnabled)
            {
                timerList.add(mc.timer.timerSpeed)
                while (timerList.size > width) timerList.removeAt(0)
                timerPeak = timerList.max() ?: 0.0f
            }

            if (motionEnabled)
            {
                motionList.add(hypot(thePlayer.motionX, thePlayer.motionZ))
                while (motionList.size > width) motionList.removeAt(0)
                motionPeak = motionList.max() ?: 0.0
            }

            if (ymotionEnabled)
            {
                ymotionList.add(thePlayer.motionY)
                while (ymotionList.size > width) ymotionList.removeAt(0)
                ymotionPeak = ymotionList.max() ?: 0.0
            }
        }

        speedScale = easeOutCubic(speedScale, speedPeak.toFloat(), speedScaleFadeSpeedValue.get())
        yspeedScale = easeOutCubic(yspeedScale, yspeedPeak.toFloat(), yspeedScaleFadeSpeedValue.get())

        if (timerEnabled) timerScale = easeOutCubic(timerScale, timerPeak, timerScaleFadeSpeedValue.get())
        if (motionEnabled) motionScale = easeOutCubic(motionScale, motionPeak.toFloat(), motionScaleFadeSpeedValue.get())
        if (ymotionEnabled) ymotionScale = easeOutCubic(ymotionScale, ymotionPeak.toFloat(), ymotionScaleFadeSpeedValue.get())

        val speedYMul = (height / speedScale).coerceAtMost(speedMultiplier.get() * 10)
        val yspeedYMul = (height / yspeedScale).coerceAtMost(yspeedMultiplier.get() * 10)
        val timerYMul = (height / timerScale).coerceAtMost(timerYMultiplier.get() * 10)
        val motionYMul = (height / motionScale).coerceAtMost(motionyMultiplier.get() * 10)
        val ymotionYMul = (height / ymotionScale).coerceAtMost(ymotionYMultiplier.get() * 10)

        val speedColor = speedColorValue.get()
        val yspeedColor = yspeedColorValue.get()
        val timerColor = timerColorValue.get()
        val motionColor = motionColorValue.get()
        val ymotionColor = ymotionColorValue.get()

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(speedThicknessValue.get())
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        // Draw Speed
        GL11.glBegin(GL11.GL_LINES)

        run {
            val speedListSize = speedList.size

            val speedListStart = (if (speedListSize > width) speedListSize - width else 0)
            for (i in speedListStart until speedListSize - 1)
            {
                val speedY = speedList[i] * speedYMul
                val speedNextY = speedList[i + 1] * speedYMul

                RenderUtils.glColor(speedColor)
                GL11.glVertex2d(i.toDouble() - speedListStart, height + 1 - speedY.coerceAtMost(height.toDouble()))
                GL11.glVertex2d(i + 1.0 - speedListStart, height + 1 - speedNextY.coerceAtMost(height.toDouble()))
            }
        }

        GL11.glEnd()

        // Draw YSpeed
        GL11.glLineWidth(yspeedThicknessValue.get())
        GL11.glBegin(GL11.GL_LINES)

        run {
            val yspeedListSize = yspeedList.size
            val ypos = yspeedPos.get().toDouble()

            val yspeedListStart = (if (yspeedListSize > width) yspeedListSize - width else 0)
            for (i in yspeedListStart until yspeedListSize - 1)
            {
                val yspeedY = yspeedList[i] * yspeedYMul
                val yspeedNextY = yspeedList[i + 1] * yspeedYMul

                RenderUtils.glColor(yspeedColor)
                GL11.glVertex2d(i.toDouble() - yspeedListStart, height + 1 - ypos - yspeedY.coerceIn(-ypos, height.toDouble() - ypos))
                GL11.glVertex2d(i + 1.0 - yspeedListStart, height + 1 - ypos - yspeedNextY.coerceIn(-ypos, height.toDouble() - ypos))
            }
        }

        GL11.glEnd()

        // Draw Timer
        if (timerEnabled)
        {
            GL11.glLineWidth(timerThicknessValue.get())
            GL11.glBegin(GL11.GL_LINES)

            run {
                val timerListSize = timerList.size

                val timerListStart = (if (timerListSize > width) timerListSize - width else 0)
                for (i in timerListStart until timerListSize - 1)
                {
                    val timerY = timerList[i] * timerYMul
                    val timerNextY = timerList[i + 1] * timerYMul

                    RenderUtils.glColor(timerColor)
                    GL11.glVertex2f(i.toFloat() - timerListStart, height + 1 - timerY.coerceAtMost(height))
                    GL11.glVertex2f(i + 1.0F - timerListStart, height + 1 - timerNextY.coerceAtMost(height))
                }
            }

            GL11.glEnd()
        }

        // Draw Motion
        if (motionEnabled)
        {
            GL11.glLineWidth(motionThicknessValue.get())
            GL11.glBegin(GL11.GL_LINES)

            run {
                val motionListSize = motionList.size

                val motionListStart = (if (motionListSize > width) motionListSize - width else 0)
                for (i in motionListStart until motionListSize - 1)
                {
                    val motionY = motionList[i] * motionYMul
                    val motionNextY = motionList[i + 1] * motionYMul

                    RenderUtils.glColor(motionColor)
                    GL11.glVertex2d(i.toDouble() - motionListStart, height + 1 - motionY.coerceAtMost(height.toDouble()))
                    GL11.glVertex2d(i + 1.0 - motionListStart, height + 1 - motionNextY.coerceAtMost(height.toDouble()))
                }
            }

            GL11.glEnd()
        }

        // Draw YMotion
        if (ymotionEnabled)
        {
            GL11.glLineWidth(ymotionThicknessValue.get())
            GL11.glBegin(GL11.GL_LINES)

            run {
                val ymotionListSize = ymotionList.size
                val ypos = ymotionYPos.get().toDouble()

                val ymotionListStart = (if (ymotionListSize > width) ymotionListSize - width else 0)
                for (i in ymotionListStart until ymotionListSize - 1)
                {
                    val ymotionY = ymotionList[i] * ymotionYMul
                    val ymotionNextY = ymotionList[i + 1] * ymotionYMul

                    RenderUtils.glColor(ymotionColor)
                    GL11.glVertex2d(i.toDouble() - ymotionListStart, height + 1 - ypos - ymotionY.coerceIn(-ypos, height.toDouble() - ypos))
                    GL11.glVertex2d(i + 1.0 - ymotionListStart, height + 1 - ypos - ymotionNextY.coerceIn(-ypos, height.toDouble() - ypos))
                }
            }

            GL11.glEnd()
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)
        classProvider.GlStateManager.resetColor()

        return Border(0F, 0F, width.toFloat(), height + 2.0f)
    }
}
