/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.PacketCounter
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.moveDirectionDegrees
import net.ccbluex.liquidbounce.utils.extensions.ping
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_2
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.text.SimpleDateFormat
import kotlin.math.hypot

/**
 * CustomHUD text element
 *
 * Allows to draw custom text
 */
@ElementInfo(name = "Text")
class Text(x: Double = 10.0, y: Double = 10.0, scale: Float = 1F, side: Side = Side.default()) : Element(x, y, scale, side)
{

    companion object
    {

        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
        val HOUR_FORMAT = SimpleDateFormat("HH:mm")

        /**
         * Create default element
         */
        fun defaultClient(): Text
        {
            val text = Text(x = 2.0, y = 2.0, scale = 2F)

            text.displayString.set("%clientName%")
            text.textShadowValue.set(true)
            text.fontValue.set(Fonts.font40)
            text.setColor(Color(0, 111, 255))

            return text
        }
    }

    private val displayString = TextValue("DisplayText", "")

    private val textGroup = ValueGroup("Text")

    private val textColorGroup = ValueGroup("Color")
    private val textColorModeValue = ListValue("Mode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom", "ColorMode")
    private val textColorValue = RGBAColorValue("Color", 255, 255, 255, 255, listOf("Red", "Green", "Blue", "Alpha"))

    private val textShadowValue = BoolValue("Shadow", true, "Shadow")

    private val rectGroup = ValueGroup("Rect")
    private val rectModeValue = ListValue("Mode", arrayOf("None", "Left", "Right"), "None", "Rect")
    private val rectWidthValue = FloatValue("Width", 3F, 1.5F, 5F, "Rect-Width")

    private val rectColorGroup = ValueGroup("Color")
    private val rectColorModeValue = ListValue("Mode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Rainbow", "Rect-Color")
    private val rectColorValue = RGBAColorValue("Color", 255, 255, 255, 255, listOf("Rect-R", "Rect-G", "Rect-B", "Rect-Alpha"))

    private val backgroundGroup = ValueGroup("Background")

    private val backgroundColorGroup = ValueGroup("Color")
    private val backgroundColorModeValue = ListValue("Mode", arrayOf("None", "Custom", "Rainbow", "RainbowShader"), "Custom", "Background-Color")
    private val backgroundColorValue = RGBAColorValue("Color", 0, 0, 0, 0, listOf("Background-R", "Background-G", "Background-B", "Background-Alpha"))

    private val backgroundRainbowCeilValue = BoolValue("RainbowCeil", false, "Background-RainbowCeil")

    private val borderGroup = ValueGroup("Border")
    private val borderWidthValue = FloatValue("Width", 3F, 1.5F, 5F, "Border-Width")
    private val borderExpandValue = FloatValue("Expand", 2F, 0.5F, 4F, "BorderExpand")

    private val borderColorGroup = ValueGroup("Color")
    private val borderColorModeValue = ListValue("Mode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom", "Border-Color")
    private val borderColorValue = RGBAColorValue("Color", 32, 32, 32, 0, listOf("Border-R", "Border-G", "Border-B", "Border-Alpha"))

    private val rainbowGroup = object : ValueGroup("Rainbow")
    {
        override fun showCondition() = arrayOf(textColorModeValue.get(), rectColorModeValue.get(), backgroundColorModeValue.get(), borderColorModeValue.get()).any { it.equals("Rainbow", ignoreCase = true) }
    }
    private val rainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
    private val rainbowSaturationValue = FloatValue("Saturation", 0.9f, 0f, 1f, "HSB-Saturation")
    private val rainbowBrightnessValue = FloatValue("Brightness", 1f, 0f, 1f, "HSB-Brightness")

    private val rainbowShaderGroup = object : ValueGroup("RainbowShader")
    {
        override fun showCondition() = arrayOf(textColorModeValue.get(), rectColorModeValue.get(), backgroundColorModeValue.get(), borderColorModeValue.get()).any { it.equals("RainbowShader", ignoreCase = true) }
    }
    private val rainbowShaderXValue = FloatValue("X", -1000F, -2000F, 2000F, "RainbowShader-X")
    private val rainbowShaderYValue = FloatValue("Y", -1000F, -2000F, 2000F, "RainbowShader-Y")

    private var fontValue = FontValue("Font", Fonts.font40)

    private var editMode = false
    private var editTicks = 0
    private var prevClick = 0L

    private var displayText = display

    private val display: String
        get()
        {
            val value = displayString.get()
            return multiReplace(if (value.isEmpty() && !editMode) "Text Element" else value)
        }

    private var cursor = displayText.length

    // Workaround
    private var deltaX = 0.0
    private var deltaY = 0.0
    private var deltaZ = 0.0

    private var lastTick = -1

    init
    {
        textColorGroup.addAll(textColorModeValue, textColorValue)
        textGroup.addAll(textColorGroup, textShadowValue)

        rectGroup.addAll(rectModeValue, rectWidthValue, rectColorGroup)
        rectColorGroup.addAll(rectColorModeValue, rectColorValue)

        backgroundGroup.addAll(backgroundColorGroup, backgroundRainbowCeilValue)
        backgroundColorGroup.addAll(backgroundColorModeValue, backgroundColorValue)

        borderGroup.addAll(borderWidthValue, borderColorGroup)
        borderColorGroup.addAll(borderColorModeValue, borderColorValue)

        rainbowGroup.addAll(rainbowSpeedValue, rainbowSaturationValue, rainbowBrightnessValue)

        rainbowShaderGroup.addAll(rainbowShaderXValue, rainbowShaderYValue)
    }

    private fun getReplacement(str: String): String?
    {
        val thePlayer = mc.thePlayer

        val s = str.toLowerCase()

        if (thePlayer != null)
        {
            val defaultTPS = 20.0

            when (s)
            {
                "x" -> return DECIMALFORMAT_2.format(thePlayer.posX)
                "y" -> return DECIMALFORMAT_2.format(thePlayer.posY)
                "z" -> return DECIMALFORMAT_2.format(thePlayer.posZ)
                "xdp" -> return thePlayer.posX.toString()
                "ydp" -> return thePlayer.posY.toString()
                "zdp" -> return thePlayer.posZ.toString()

                "mx" -> return DECIMALFORMAT_2.format(thePlayer.motionX)
                "my" -> return DECIMALFORMAT_2.format(thePlayer.motionY)
                "mz" -> return DECIMALFORMAT_2.format(thePlayer.motionZ)

                "dx" -> return DECIMALFORMAT_2.format(deltaX)
                "dy" -> return DECIMALFORMAT_2.format(deltaY)
                "dz" -> return DECIMALFORMAT_2.format(deltaZ)

                "mxpersec" -> return DECIMALFORMAT_2.format(thePlayer.motionX * defaultTPS)
                "mypersec" -> return DECIMALFORMAT_2.format(thePlayer.motionY * defaultTPS)
                "mzpersec" -> return DECIMALFORMAT_2.format(thePlayer.motionZ * defaultTPS)

                "dxpersec" -> return DECIMALFORMAT_2.format((deltaX) * defaultTPS)
                "dypersec" -> return DECIMALFORMAT_2.format((deltaY) * defaultTPS)
                "dzpersec" -> return DECIMALFORMAT_2.format((deltaZ) * defaultTPS)

                "mxdp" -> return thePlayer.motionX.toString()
                "mydp" -> return thePlayer.motionY.toString()
                "mzdp" -> return thePlayer.motionZ.toString()

                "dxdp" -> return "$deltaX"
                "dydp" -> return "$deltaY"
                "dzdp" -> return "$deltaZ"

                "mxdppersec" -> return "${thePlayer.motionX * defaultTPS}"
                "mydppersec" -> return "${thePlayer.motionY * defaultTPS}"
                "mzdppersec" -> return "${thePlayer.motionZ * defaultTPS}"

                "dxdppersec" -> return "${deltaX * defaultTPS}"
                "dydppersec" -> return "${deltaY * defaultTPS}"
                "dzdppersec" -> return "${deltaZ * defaultTPS}"

                "velocity" -> return DECIMALFORMAT_2.format(hypot(thePlayer.motionX, thePlayer.motionZ))
                "velocitypersec" -> return DECIMALFORMAT_2.format(hypot(thePlayer.motionX, thePlayer.motionZ) * defaultTPS)

                "move" -> return DECIMALFORMAT_2.format(hypot(deltaX, deltaZ))
                "movepersec" -> return DECIMALFORMAT_2.format(hypot(deltaX, deltaZ) * defaultTPS)

                "velocitydp" -> return "${hypot(thePlayer.motionX, thePlayer.motionZ)}"
                "velocitydppersec" -> return "${hypot(thePlayer.motionX, thePlayer.motionZ) * defaultTPS}"

                "movedp" -> return "${hypot(deltaX, deltaZ)}"
                "movedppersec" -> return "${hypot(deltaX, deltaZ) * defaultTPS}"

                "ping" -> return thePlayer.ping.toString()
                "health" -> return DECIMALFORMAT_2.format(thePlayer.health)
                "maxhealth" -> return DECIMALFORMAT_2.format(thePlayer.maxHealth)
                "food" -> return thePlayer.foodStats.foodLevel.toString()

                "facing" -> return StringUtils.getHorizontalFacing(thePlayer.rotationYaw)
                "facingadv" -> return StringUtils.getHorizontalFacingAdv(thePlayer.rotationYaw)
                "facingvector" -> return StringUtils.getHorizontalFacingTowards(thePlayer.rotationYaw)

                "movingdir" -> return if (thePlayer.isMoving) StringUtils.getHorizontalFacing(thePlayer.moveDirectionDegrees) else "NONE"
                "movingdirvector" -> return if (thePlayer.isMoving) StringUtils.getHorizontalFacingTowards(thePlayer.moveDirectionDegrees) else "NONE"

                "jumpmovementfactor" -> return StringUtils.DECIMALFORMAT_6.format(thePlayer.jumpMovementFactor)
                "speedinair" -> return StringUtils.DECIMALFORMAT_6.format(thePlayer.speedInAir)
            }
        }

        return when (s)
        {
            "username" -> mc.session.username

            "clientname" -> LiquidBounce.CLIENT_NAME
            "clientversion" -> "b${LiquidBounce.CLIENT_VERSION}"
            "clientcreator" -> LiquidBounce.CLIENT_CREATOR

            "fps" -> mc.debugFPS.toString()

            "date" -> DATE_FORMAT.format(System.currentTimeMillis())
            "time" -> HOUR_FORMAT.format(System.currentTimeMillis())

            "serverip" -> ServerUtils.remoteIp

            "lcs", "cps", "lcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.LEFT).toString()
            "mcs", "mcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.MIDDLE).toString()
            "rcs", "rcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.RIGHT).toString()

            "timer" -> return mc.timer.timerSpeed.toString()

            "packetin", "ppsin" -> return PacketCounter.getPacketCount(PacketCounter.PacketType.INBOUND, 1000L).toString()
            "packetout", "ppsout" -> return PacketCounter.getPacketCount(PacketCounter.PacketType.OUTBOUND, 1000L).toString()

            else -> null // Null = don't replace
        }
    }

    private fun multiReplace(str: String): String
    {
        var lastReplacementChar = -1
        val result = StringBuilder()
        for (i in str.indices)
        {
            if (str[i] == '%')
            {
                if (lastReplacementChar != -1)
                {
                    if (lastReplacementChar + 1 != i)
                    {
                        val replacement = getReplacement(str.substring(lastReplacementChar + 1, i))

                        if (replacement != null)
                        {
                            result.append(replacement)
                            lastReplacementChar = -1
                            continue
                        }
                    }
                    result.append(str, lastReplacementChar, i)
                }
                lastReplacementChar = i
            }
            else if (lastReplacementChar == -1) result.append(str[i])
        }

        if (lastReplacementChar != -1) result.append(str, lastReplacementChar, str.length)

        return "$result"
    }

    /**
     * Draw element
     */
    override fun drawElement(): Border
    {
        val colorMode = textColorModeValue.get()

        // Text
        val textColorAlpha = textColorValue.getAlpha()
        val textCustomColor = textColorValue.get()

        val shadow = textShadowValue.get()

        // Rect
        val rectMode = rectModeValue.get()
        val rectColorMode = rectColorModeValue.get()
        val rectColorAlpha = rectColorValue.getAlpha()

        val rectWidth = rectWidthValue.get()

        // Background
        val backgroundColorMode = backgroundColorModeValue.get()
        val backgroundColorAlpha = backgroundColorValue.getAlpha()

        val backgroundRainbowCeil = backgroundRainbowCeilValue.get()

        val borderWidth = borderWidthValue.get()
        val borderColorMode = borderColorModeValue.get()
        val borderColorAlpha = borderColorValue.getAlpha()

        val fontRenderer = fontValue.get()

        // Rainbow
        val rainbowSpeed = rainbowSpeedValue.get()
        val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
        val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
        val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

        val saturation = rainbowSaturationValue.get()
        val brightness = rainbowBrightnessValue.get()

        val horizontalSide = side.horizontal

        val textWidth = fontRenderer.getStringWidth(displayText)

        // Workaround
        mc.thePlayer?.let { thePlayer ->
            if (lastTick != thePlayer.ticksExisted)
            {
                lastTick = thePlayer.ticksExisted

                deltaX = thePlayer.posX - thePlayer.prevPosX
                deltaY = thePlayer.posY - thePlayer.prevPosY
                deltaZ = thePlayer.posZ - thePlayer.prevPosZ
            }
        }

        // Border
        val borderExpand = borderExpandValue.get()
        val (borderXStart, borderXEnd) = when (horizontalSide)
        {
            Side.Horizontal.LEFT -> -borderExpand to textWidth + borderExpand
            Side.Horizontal.MIDDLE -> -borderExpand - textWidth * 0.5F to borderExpand + textWidth * 0.5F
            Side.Horizontal.RIGHT -> -borderExpand - textWidth to borderExpand
        }

        var borderYStart = -borderExpand
        val borderYEnd = fontRenderer.fontHeight.toFloat() + borderExpand

        // Rect mode
        val leftRect = rectMode.equals("Left", ignoreCase = true)
        val rightRect = rectMode.equals("Right", ignoreCase = true)

        val backgroundXStart = borderXStart + if (!leftRect) 0f else rectWidth
        val backgroundXEnd = borderXEnd + if (rightRect) 0f else rectWidth

        val textX = (if (rightRect) 0f else if (leftRect) rectWidth else rectWidth * 0.5F) + (borderXStart + borderExpand)

        val rainbowRGB = ColorUtils.rainbowRGB(speed = rainbowSpeed, saturation = saturation, brightness = brightness)

        // Background Color
        val backgroundRainbowShader = backgroundColorMode.equals("RainbowShader", ignoreCase = true)
        val backgroundColor = if (backgroundColorAlpha > 0) when
        {
            backgroundRainbowShader -> 0
            backgroundColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.applyAlphaChannel(rainbowRGB, backgroundColorAlpha)
            else -> backgroundColorValue.get()
        }
        else 0

        // Second Background Color
        val borderRainbowShader = borderColorMode.equals("RainbowShader", ignoreCase = true)
        val shouldDrawBorder = backgroundColorAlpha > 0 && borderColorAlpha > 0
        val borderColor = if (shouldDrawBorder)
        {
            when
            {
                borderRainbowShader -> 0
                borderColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.applyAlphaChannel(rainbowRGB, borderColorAlpha)
                else -> borderColorValue.get()
            }
        }
        else 0

        // Rect Color
        val rectRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)
        val rectColor = if (rectColorAlpha > 0) when
        {
            rectRainbowShader -> 0
            rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.applyAlphaChannel(rainbowRGB, rectColorAlpha)
            else -> rectColorValue.get()
        }
        else 0

        // Text Color
        val textRainbowShader = colorMode.equals("RainbowShader", ignoreCase = true)
        val textColor = when
        {
            textRainbowShader -> 0
            colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(alpha = textColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
            else -> textCustomColor
        }

        // Render Background
        if (backgroundColorAlpha > 0)
        {
            if (shouldDrawBorder)
            {
                RainbowShader.begin(borderRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
                    RenderUtils.drawRect(backgroundXStart - borderWidth, borderYStart - borderWidth, backgroundXEnd + borderWidth, borderYEnd + borderWidth, borderColor)
                }
            }

            RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
                RenderUtils.drawRect(backgroundXStart, borderYStart, backgroundXEnd, borderYEnd, backgroundColor)
            }

            if (backgroundRainbowCeil) RainbowShader.begin(true, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
                RenderUtils.drawRect(backgroundXStart, borderYStart - 1, backgroundXEnd, borderYStart, 0)
                borderYStart--
            }
        }

        // Render Rect
        if (leftRect || rightRect) RainbowShader.begin(rectRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
            if (leftRect) RenderUtils.drawRect(backgroundXStart - rectWidth, borderYStart, backgroundXStart, borderYEnd, rectColor) else RenderUtils.drawRect(borderXEnd, borderYStart, borderXEnd + rectWidth, borderYEnd, rectColor)
        }

        // Render Text
        RainbowFontShader.begin(textRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
            fontRenderer.drawString(displayText, textX, 0F, textColor, shadow)

            if (editMode && classProvider.isGuiHudDesigner(mc.currentScreen) && editTicks <= 10) fontRenderer.drawString("_", (if (rightRect) 0f else if (leftRect) rectWidth else rectWidth * 0.5F) + (borderXStart + borderExpand) + fontRenderer.getStringWidth(displayText.take(cursor)) + 2F, 0F, textColor, shadow)
        }

        // Disable edit mode when current gui is not HUD Designer
        if (editMode && !classProvider.isGuiHudDesigner(mc.currentScreen))
        {
            editMode = false
            updateElement()
        }

        val borderExpanded = if (shouldDrawBorder) borderWidth else 0F
        return Border(backgroundXStart - (if (leftRect) rectWidth else 0F) - borderExpanded, -borderExpand - borderExpanded, backgroundXEnd + (if (rightRect) rectWidth else 0F) + borderExpanded, fontRenderer.fontHeight.toFloat() + borderExpand + borderExpanded)
    }

    override fun updateElement()
    {
        if (editTicks++ > 20) editTicks = 0

        displayText = if (editMode) displayString.get() else display
    }

    override fun handleMouseClick(x: Double, y: Double, mouseButton: Int)
    {
        if (isInBorder(x, y) && mouseButton == 0)
        {
            if (System.currentTimeMillis() - prevClick <= 250L) editMode = true

            prevClick = System.currentTimeMillis()
        }
        else editMode = false
    }

    override fun handleKey(c: Char, keyCode: Int)
    {
        if (editMode && classProvider.isGuiHudDesigner(mc.currentScreen))
        {
            val string = displayString.get()

            when (keyCode)
            {
                Keyboard.KEY_BACK -> if (string.isNotEmpty() && cursor > 0)
                {
                    if (cursor >= string.length) displayString.set(string.dropLast(1)) else displayString.set("${string.take(cursor - 1)}${string.substring(cursor, string.length)}")
                    cursor--
                    updateElement()
                }

                Keyboard.KEY_LEFT -> cursor = (cursor - 1).coerceAtLeast(0)

                Keyboard.KEY_RIGHT -> cursor = (cursor + 1).coerceAtMost(string.length)

                else -> if (ColorUtils.isAllowedCharacter(c) || c == '\u00A7')
                {
                    if (cursor >= string.length) displayString.set(string + c) else displayString.set("${string.take(cursor)}$c${string.substring(cursor, string.length)}")
                    cursor++
                    updateElement()
                }
            }
        }
    }

    fun setColor(c: Color): Text
    {
        textColorValue.set(c.red, c.green, c.blue, c.alpha)
        return this
    }
}
