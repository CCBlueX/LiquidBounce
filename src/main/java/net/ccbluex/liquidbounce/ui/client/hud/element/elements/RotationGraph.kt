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
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import kotlin.math.abs
import kotlin.math.hypot

/**
 * CustomHUD rotation-graph element
 *
 * Allows to draw custom rotation-graph
 */
@ElementInfo(name = "RotationGraph")
class RotationGraph(x: Double = 75.0, y: Double = 110.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)) : Element(x, y, scale, side)
{
    private val widthValue = IntegerValue("Width", 150, 100, 300)
    private val heightValue = IntegerValue("Height", 50, 30, 300)

    private val rotationGroup = ValueGroup("Rotation")
    private val rotationMultiplier = FloatValue("Multiplier", 2F, 0.5F, 5F, "Rotation-yMultiplier")
    private val rotationThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "Rotation-Thickness")
    private val rotationColorValue = RGBColorValue("Color", 0, 255, 72, Triple("Rotation-R", "Rotation-G", "Rotation-B"))

    private val yawMovementGroup = ValueGroup("YawMovement")
    private val yawMovementEnabledValue = BoolValue("Enabled", false, "YawMovement")
    private val yawMovementMultiplierValue = FloatValue("Multiplier", 2F, 0.5F, 5F, "YawMovement-yMultiplier")
    private val yawMovementThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "YawMovement-Thickness")
    private val yawMovementColorValue = RGBColorValue("Color", 0, 0, 255, Triple("YawMovement-R", "YawMovement-G", "YawMovement-B"))

    private val pitchMovementGroup = ValueGroup("PitchMovement")
    private val pitchMovementEnabledValue = BoolValue("Enabled", false, "PitchMovement")
    private val pitchMovementMultiplierValue = FloatValue("Multiplier", 2F, 0.5F, 5F, "PitchMovement-yMultiplier")
    private val pitchMovementThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "PitchMovement-Thickness")
    private val pitchMovementColorValue = RGBColorValue("Color", 111, 0, 255, Triple("PitchMovement-R", "PitchMovement-G", "PitchMovement-B"))

    private val rotationConsistencyGroup = ValueGroup("RotationConsistency")
    private val rotationConsistencyEnabledValue = BoolValue("Enabled", false, "RotationConsistency")
    private val rotationConsistencyMultiplierValue = FloatValue("Multiplier", 2F, 0.5F, 5F, "RotationConsistency-yMultiplier")
    private val rotationConsistencyThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "RotationConsistency-Thickness")
    private val rotationConsistencyColorValue = RGBColorValue("Color", 0, 255, 72, Triple("RotationConsistency-R", "RotationConsistency-G", "RotationConsistency-B"))

    private val yawConsistencyGroup = ValueGroup("YawConsistency")
    private val yawConsistencyEnabledValue = BoolValue("Enabled", false, "YawConsistency")
    private val yawConsistencyMultiplierValue = FloatValue("Multiplier", 2F, 0.5F, 5F, "YawConsistency-yMultiplier")
    private val yawConsistencyThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "YawConsistency-Thickness")
    private val yawConsistencyColorValue = RGBColorValue("Color", 0, 255, 180, Triple("YawConsistency-R", "YawConsistency-G", "YawConsistency-B"))

    private val pitchConsistencyGroup = ValueGroup("PitchConsistency")
    private val pitchConsistencyEnabled = BoolValue("Enabled", false, "PitchConsistency")
    private val pitchConsistencyMultiplier = FloatValue("Multiplier", 2F, 0.5F, 5F, "PitchConsistency-yMultiplier")
    private val pitchConsistencyThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "PitchConsistency-Thickness")
    private val pitchConsistencyColorValue = RGBColorValue("Color", 0, 180, 255, Triple("PitchConsistency-R", "PitchConsistency-G", "PitchConsistency-B"))

    init
    {
        rotationGroup.addAll(rotationMultiplier, rotationThicknessValue, rotationColorValue)
        yawMovementGroup.addAll(yawMovementEnabledValue, yawMovementMultiplierValue, yawMovementThicknessValue, yawMovementColorValue)
        pitchMovementGroup.addAll(pitchMovementEnabledValue, pitchMovementMultiplierValue, pitchMovementThicknessValue, pitchMovementColorValue)
        rotationConsistencyGroup.addAll(rotationConsistencyEnabledValue, rotationConsistencyMultiplierValue, rotationConsistencyThicknessValue, rotationConsistencyColorValue)
        yawConsistencyGroup.addAll(yawConsistencyEnabledValue, yawConsistencyMultiplierValue, yawConsistencyThicknessValue, yawConsistencyColorValue)
        pitchConsistencyGroup.addAll(pitchConsistencyEnabled, pitchConsistencyMultiplier, pitchConsistencyThicknessValue, pitchConsistencyColorValue)
    }

    private val rotationList = ArrayList<Float>()
    private val yawMovementList = ArrayList<Float>()
    private val pitchMovementList = ArrayList<Float>()
    private val rotationConsistencyList = ArrayList<Float>()
    private val yawConsistencyList = ArrayList<Float>()
    private val pitchConsistencyList = ArrayList<Float>()

    private var lastTick = -1

    private var lastRotation = 0.0F
    private var lastYawMovement = 0.0F
    private var lastPitchMovement = 0.0F

    override fun drawElement(): Border?
    {
        val thePlayer = mc.thePlayer ?: return null

        val width = widthValue.get()
        val height = heightValue.get().toFloat()

        val yawMovementEnabled = yawMovementEnabledValue.get()
        val pitchMovementEnabled = pitchMovementEnabledValue.get()

        val rotationConsistencyEnabled = rotationConsistencyEnabledValue.get()

        val yawConsistencyEnabled = yawConsistencyEnabledValue.get()
        val pitchConsistencyEnabled = pitchConsistencyEnabled.get()

        if (lastTick != thePlayer.ticksExisted)
        {
            // Update rotation

            lastTick = thePlayer.ticksExisted

            val serverRotation = RotationUtils.serverRotation
            val prevServerRotation = RotationUtils.lastServerRotation

            val yawMovement = abs(serverRotation.yaw - prevServerRotation.yaw)
            val pitchMovement = abs(serverRotation.pitch - prevServerRotation.pitch)

            val rotation = hypot(yawMovement, pitchMovement)

            rotationList.add(rotation)

            while (rotationList.size > width) rotationList.removeAt(0)

            if (yawMovementEnabled)
            {
                yawMovementList.add(yawMovement)

                while (yawMovementList.size > width) yawMovementList.removeAt(0)
            }

            if (pitchMovementEnabled)
            {
                pitchMovementList.add(pitchMovement)

                while (pitchMovementList.size > width) pitchMovementList.removeAt(0)
            }

            if (rotationConsistencyEnabled)
            {
                rotationConsistencyList.add(abs(rotation - lastRotation))

                while (rotationConsistencyList.size > width) rotationConsistencyList.removeAt(0)
            }

            if (yawConsistencyEnabled)
            {
                yawConsistencyList.add(abs(yawMovement - lastYawMovement))

                while (yawConsistencyList.size > width) yawConsistencyList.removeAt(0)
            }

            if (pitchConsistencyEnabled)
            {
                pitchConsistencyList.add(abs(pitchMovement - lastPitchMovement))

                while (pitchMovementList.size > width) pitchMovementList.removeAt(0)
            }

            lastRotation = rotation
            lastYawMovement = yawMovement
            lastPitchMovement = pitchMovement
        }

        val rotationYMul = rotationMultiplier.get()
        val yawMovementYMul = yawMovementMultiplierValue.get()
        val pitchMovementYMul = pitchMovementMultiplierValue.get()
        val rotationConsistencyYMul = rotationConsistencyMultiplierValue.get()
        val yawConsistencyYMul = yawConsistencyMultiplierValue.get()
        val pitchConsistencyYMul = pitchConsistencyMultiplier.get()

        val rotationColor = rotationColorValue.get()
        val yawMovementColor = yawMovementColorValue.get()
        val pitchMovementColor = pitchMovementColorValue.get()
        val rotationConsistencyColor = rotationConsistencyColorValue.get()
        val yawConsistencyColor = yawConsistencyColorValue.get()
        val pitchConsistencyColor = pitchConsistencyColorValue.get()

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(rotationThicknessValue.get())
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        // Draw Rotation

        GL11.glBegin(GL11.GL_LINES)

        run {
            val rotationListSize = rotationList.size

            val rotationListStart = (if (rotationListSize > width) rotationListSize - width else 0)
            for (i in rotationListStart until rotationListSize - 1)
            {
                val rotationY = rotationList[i] * 10 * rotationYMul
                val rotationNextY = rotationList[i + 1] * 10 * rotationYMul

                RenderUtils.glColor(rotationColor)
                GL11.glVertex2f(i.toFloat() - rotationListStart, height + 1 - rotationY.coerceAtMost(height))
                GL11.glVertex2f(i + 1.0F - rotationListStart, height + 1 - rotationNextY.coerceAtMost(height))
            }
        }

        GL11.glEnd()

        if (yawMovementEnabled)
        {
            // Draw Yaw Movements

            GL11.glLineWidth(yawMovementThicknessValue.get())
            GL11.glBegin(GL11.GL_LINES)

            run {
                val yawMovementListSize = yawMovementList.size

                val yawMovementListStart = (if (yawMovementListSize > width) yawMovementListSize - width else 0)
                for (i in yawMovementListStart until yawMovementListSize - 1)
                {
                    val yawMovementY = yawMovementList[i] * 10 * yawMovementYMul
                    val yawMovementNextY = yawMovementList[i + 1] * 10 * yawMovementYMul

                    RenderUtils.glColor(yawMovementColor)
                    GL11.glVertex2f(i.toFloat() - yawMovementListStart, height + 1 - yawMovementY.coerceAtMost(height))
                    GL11.glVertex2f(i + 1.0F - yawMovementListStart, height + 1 - yawMovementNextY.coerceAtMost(height))
                }
            }

            GL11.glEnd()
        }

        if (pitchMovementEnabled)
        {
            // Draw Pitch Movements

            GL11.glLineWidth(pitchMovementThicknessValue.get())
            GL11.glBegin(GL11.GL_LINES)

            run {
                val pitchMovementListSize = pitchMovementList.size

                val pitchMovementListStart = (if (pitchMovementListSize > width) pitchMovementListSize - width else 0)
                for (i in pitchMovementListStart until pitchMovementListSize - 1)
                {
                    val pitchMovementY = pitchMovementList[i] * 10 * pitchMovementYMul
                    val pitchMovementNextY = pitchMovementList[i + 1] * 10 * pitchMovementYMul

                    RenderUtils.glColor(pitchMovementColor)
                    GL11.glVertex2f(i.toFloat() - pitchMovementListStart, height + 1 - pitchMovementY.coerceAtMost(height))
                    GL11.glVertex2f(i + 1.0F - pitchMovementListStart, height + 1 - pitchMovementNextY.coerceAtMost(height))
                }
            }

            GL11.glEnd()
        }

        // Draw Rotation Consistency

        GL11.glLineWidth(rotationConsistencyThicknessValue.get())
        GL11.glBegin(GL11.GL_LINES)

        run {
            val rotationConsistencyListSize = rotationConsistencyList.size

            val rotationConsistencyListStart = (if (rotationConsistencyListSize > width) rotationConsistencyListSize - width else 0)
            for (i in rotationConsistencyListStart until rotationConsistencyListSize - 1)
            {
                val rotationConsistencyY = rotationConsistencyList[i] * 10 * rotationConsistencyYMul
                val rotationConsistencyNextY = rotationConsistencyList[i + 1] * 10 * rotationConsistencyYMul

                RenderUtils.glColor(rotationConsistencyColor)
                GL11.glVertex2f(i.toFloat() - rotationConsistencyListStart, height + 1 - rotationConsistencyY.coerceAtMost(height))
                GL11.glVertex2f(i + 1.0F - rotationConsistencyListStart, height + 1 - rotationConsistencyNextY.coerceAtMost(height))
            }
        }

        GL11.glEnd()

        // Draw Yaw Consistency
        if (yawConsistencyEnabled)
        {
            GL11.glLineWidth(yawConsistencyThicknessValue.get())
            GL11.glBegin(GL11.GL_LINES)

            run {
                val yawConsistencyListSize = yawConsistencyList.size

                val yawConsistencyListStart = (if (yawConsistencyListSize > width) yawConsistencyListSize - width else 0)
                for (i in yawConsistencyListStart until yawConsistencyListSize - 1)
                {
                    val yawConsistencyY = yawConsistencyList[i] * 10 * yawConsistencyYMul
                    val yawConsistencyNextY = yawConsistencyList[i + 1] * 10 * yawConsistencyYMul

                    RenderUtils.glColor(yawConsistencyColor)
                    GL11.glVertex2f(i.toFloat() - yawConsistencyListStart, height + 1 - yawConsistencyY.coerceAtMost(height))
                    GL11.glVertex2f(i + 1.0F - yawConsistencyListStart, height + 1 - yawConsistencyNextY.coerceAtMost(height))
                }
            }

            GL11.glEnd()
        }

        // Draw Pitch Consistency
        if (pitchConsistencyEnabled)
        {
            GL11.glLineWidth(pitchConsistencyThicknessValue.get())
            GL11.glBegin(GL11.GL_LINES)

            run {
                val pitchConsistencyListSize = pitchConsistencyList.size

                val pitchConsistencyListStart = (if (pitchConsistencyListSize > width) pitchConsistencyListSize - width else 0)
                for (i in pitchConsistencyListStart until pitchConsistencyListSize - 1)
                {
                    val pitchConsistencyY = pitchConsistencyList[i] * 10 * pitchConsistencyYMul
                    val pitchConsistencyNextY = pitchConsistencyList[i + 1] * 10 * pitchConsistencyYMul

                    RenderUtils.glColor(pitchConsistencyColor)
                    GL11.glVertex2f(i.toFloat() - pitchConsistencyListStart, height + 1 - pitchConsistencyY.coerceAtMost(height))
                    GL11.glVertex2f(i + 1.0F - pitchConsistencyListStart, height + 1 - pitchConsistencyNextY.coerceAtMost(height))
                }
            }

            GL11.glEnd()
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)
        GlStateManager.resetColor()

        return Border(0F, 0F, width.toFloat(), height + 2.0f)
    }
}
