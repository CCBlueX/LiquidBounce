/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.combat.Aimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.BowAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.TpAura
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.drawString
import net.ccbluex.liquidbounce.utils.extensions.equalTo
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_1
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_2
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.easeOutCubic
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A target hud
 */
@ElementInfo(name = "Target")
class Target : Element()
{
    private val minWidthValue = IntegerValue("MinWidth", 180, 160, 300)
    private val heightValue = IntegerValue("Height", 100, 100, 150)

    private val headSizeValue = IntegerValue("HeadSize", 90, 30, 90)

    private val textYOffsetValue = IntegerValue("TextYOffset", 35, 0, 50)
    private val textScaleValue = FloatValue("TextScale", 0.5F, 0.5F, 0.75F)

    private val debugPanelGroup = ValueGroup("DebugPanel")
    private val debugPanelEnabledValue = BoolValue("Enabled", false)
    private val debugPanelWidthValue = IntegerValue("Width", 50, 0, 150)
    private val debugPanelTextFontValue = FontValue("TextFont", Fonts.minecraftFont)
    private val debugPanelTextScaleValue = FloatValue("DebugTextScale", 0.75F, 0.5F, 1F)

    private val playerOnlyValue = BoolValue("PlayerOnly", true)
    private val barWidthSubtractorValue = IntegerValue("BarWidthSubtractor", 2, 0, 5)

    private val damageAnimationColorValue = RGBColorValue("DamageAnimationColor", 252, 185, 65, Triple("DamageAnimationColorRed", "DamageAnimationColorGreen", "DamageAnimationColorBlue"))
    private val healAnimationColorValue = RGBColorValue("HealAnimationColor", 44, 201, 144, Triple("HealAnimationColorRed", "HealAnimationColorGreen", "HealAnimationColorBlue"))

    private val healthFadeStartDelayValue = IntegerValue("HealthFadeStartDelay", 2, 0, 40)
    private val healthFadeSpeedValue = IntegerValue("HealthFadeSpeed", 2, 1, 9)
    private val absorptionFadeSpeedValue = IntegerValue("AbsorptionFadeSpeed", 2, 1, 9)
    private val armorFadeSpeedValue = IntegerValue("ArmorFadeSpeed", 2, 1, 9)

    private val healthTypeValue = ListValue("HealthType", arrayOf("Metadata", "Mineplex", "Hive"), "Metadata")

    private val informationDisplayType = ListValue("InformationDisplayType", arrayOf("Verbose", "Abbreviated"), "Verbose")

    private val renderEquipmentsValue = BoolValue("Armor", true)

    private val backgroundGroup = ValueGroup("Background")
    private val backgroundRainbowCeilValue = BoolValue("RainbowCeil", false, "Background-RainbowCeil")

    private val backgroundColorGroup = ValueGroup("Color")
    private val backgroundColorModeValue = ListValue("Mode", arrayOf("None", "Custom", "Rainbow", "RainbowShader"), "Custom", "Background-Color")
    private val backgroundColorValue = RGBColorValue("Color", 0, 0, 0, Triple("Background-R", "Background-G", "Background-B"))

    private val borderGroup = ValueGroup("Border")
    private val borderWidthValue = FloatValue("Width", 3F, 2F, 5F, "Border-Width")

    private val borderColorGroup = ValueGroup("Color")
    private val borderColorModeValue = ListValue("Mode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom", "Border-Color")
    private val borderColorValue = RGBAColorValue("Color", 0, 0, 0, 0, listOf("Border-R", "Border-G", "Border-B", "Border-Alpha"))

    private val rainbowGroup = object : ValueGroup("Rainbow")
    {
        override fun showCondition() = backgroundColorModeValue.get().equals("Rainbow", ignoreCase = true) || borderColorModeValue.get().equals("Rainbow", ignoreCase = true)
    }
    private val rainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
    private val rainbowSaturationValue = FloatValue("Saturation", 0.9f, 0f, 1f, "HSB-Saturation")
    private val rainbowBrightnessValue = FloatValue("Brightness", 1f, 0f, 1f, "HSB-Brightness")

    private val rainbowShaderGroup = object : ValueGroup("RainbowShader")
    {
        override fun showCondition() = backgroundColorModeValue.get().equals("RainbowShader", ignoreCase = true) || borderColorModeValue.get().equals("RainbowShader", ignoreCase = true)
    }
    private val rainbowShaderXValue = FloatValue("X", -1000F, -2000F, 2000F, "RainbowShader-X")
    private val rainbowShaderYValue = FloatValue("Y", -1000F, -2000F, 2000F, "RainbowShader-Y")

    private val nameFontValue = FontValue("NameFont", Fonts.font40)
    private val textFontValue = FontValue("TextFont", Fonts.font35)

    init
    {
        backgroundColorGroup.addAll(backgroundColorModeValue, backgroundColorValue)
        backgroundGroup.addAll(backgroundRainbowCeilValue, backgroundColorGroup)

        debugPanelGroup.addAll(debugPanelEnabledValue, debugPanelWidthValue, debugPanelTextFontValue, debugPanelTextScaleValue)

        borderColorGroup.addAll(borderColorModeValue, borderColorValue)
        borderGroup.addAll(borderWidthValue, borderColorGroup)

        rainbowGroup.addAll(rainbowSpeedValue, rainbowSaturationValue, rainbowBrightnessValue)
        rainbowShaderGroup.addAll(rainbowShaderXValue, rainbowShaderYValue)
    }

    private var easingHealth: Float = 0F
    private var healthAnimationDelay = 0
    private var easingAbsorption: Float = 0F
    private var easingArmor: Float = 0F
    private var lastTarget: Entity? = null
    private var prevTargetHealth = 0F
    private var prevTick = 0

    override fun drawElement(): Border?
    {
        val thePlayer = mc.thePlayer ?: return null
        val renderItem = mc.renderItem
        val netHandler = mc.netHandler
        val textureManager = mc.textureManager

        val targetInfo = queryTarget()
        var target = targetInfo?.first
        val debug = targetInfo?.second

        val drawDebug = debugPanelEnabledValue.get() && debug != null
        val debugWidth = debugPanelWidthValue.get() + 150f

        if (targetInfo == null && mc.currentScreen is GuiHudDesigner) target = thePlayer

        val minWidth = minWidthValue.get().toFloat()
        val height = heightValue.get().toFloat()

        val rainbowSpeed = rainbowSpeedValue.get()
        val saturation = rainbowSaturationValue.get()
        val brightness = rainbowBrightnessValue.get()

        val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
        val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
        val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

        val rainbowRGB = ColorUtils.rainbowRGB(speed = rainbowSpeed, saturation = saturation, brightness = brightness)

        val backgroundColorMode = backgroundColorModeValue.get()
        val backgroundRainbowCeil = backgroundRainbowCeilValue.get()
        val backgroundRainbowShader = backgroundColorMode.equals("RainbowShader", ignoreCase = true)
        val backgroundColor = when
        {
            backgroundRainbowShader -> 0
            backgroundColorMode.equals("Rainbow", ignoreCase = true) -> rainbowRGB
            else -> backgroundColorValue.get()
        }

        val borderWidth = borderWidthValue.get()
        val borderColorMode = borderColorModeValue.get()
        val borderColorAlpha = borderColorValue.getAlpha()
        val borderRainbowShader = borderColorMode.equals("RainbowShader", ignoreCase = true)
        val shouldDrawBorder = borderColorAlpha > 0
        val borderColor = if (shouldDrawBorder) when
        {
            borderRainbowShader -> 0
            borderColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.applyAlphaChannel(rainbowRGB, borderColorAlpha)
            else -> borderColorValue.get()
        }
        else 0

        if (target != null && target.isEntityAlive)
        {
            val isPlayer = target is EntityPlayer

            if (isPlayer || !playerOnlyValue.get())
            {
                val headRenderSize = headSizeValue.get()

                val nameFont = nameFontValue.get()
                val textFont = textFontValue.get()

                val targetAbsorption = target.absorptionAmount
                var targetArmor = 0
                var targetHealth = target.health + targetAbsorption
                if (prevTargetHealth != targetHealth) healthAnimationDelay = healthFadeStartDelayValue.get()
                prevTargetHealth = targetHealth

                val targetMaxHealth = target.maxHealth + targetAbsorption

                // Damage/Heal animation color
                val damageColor = damageAnimationColorValue.get()
                val healColor = healAnimationColorValue.get()

                val verbose = informationDisplayType.get().equals("Verbose", ignoreCase = true)

                val metaDataBuilder = StringJoiner("\u00A7r | ", " | ", "\u00A7r").setEmptyValue("")

                // TODO: Simplify duplicate codes
                if (target.isInvisible) metaDataBuilder.add("\u00A77\u00A7o${if (verbose) "Invisible" else "invis"}") // TODO: isInvisibleToPlayer(thePlayer)
                if (target.isBurning) metaDataBuilder.add("\u00A7c${if (verbose) "Burning" else "burn"}")
                if (target.isEating) metaDataBuilder.add("\u00A7e${if (verbose) "Using Item" else "use"}")
                if (target.isSilent) metaDataBuilder.add("\u00A78${if (verbose) "Silent" else "silent"}")

                val headBoxYSize = headRenderSize + 6F

                var textXOffset = 2
                var healthBarYOffset = height - 3F /*107F*/

                val name = target.displayName.formattedText
                val targetHealthPercentage = targetHealth / targetMaxHealth

                if (target is EntityPlayer)
                {
                    val healthMethod = healthTypeValue.get().lowercase(Locale.getDefault())
                    if (healthMethod.equals("Mineplex", ignoreCase = true) || healthMethod.equals("Hive", ignoreCase = true)) targetHealth = EntityUtils.getPlayerHealthFromScoreboard(target.gameProfile.name, isMineplex = healthTypeValue.get().equals("Mineplex", true)).toFloat()

                    targetArmor = target.totalArmorValue

                    textXOffset = headRenderSize + 10
                    healthBarYOffset = height - 6F /*104F*/
                }

                var textYOffset = 10

                // Reset easing
                val targetChanged = target != lastTarget
                if (targetChanged || easingHealth < 0 || easingHealth > targetMaxHealth || abs(easingHealth - targetHealth) < 0.01) easingHealth = targetHealth
                if (targetChanged || easingAbsorption < 0 || easingAbsorption > targetAbsorption || abs(easingAbsorption - targetAbsorption) < 0.01) easingAbsorption = targetAbsorption
                if (isPlayer && (targetChanged || easingArmor < 0 || easingArmor > 20 || abs(easingArmor - targetArmor) < 0.01)) easingArmor = targetArmor.toFloat()
                val suspendAnimation = healthAnimationDelay > 0
                if (suspendAnimation && thePlayer.ticksExisted != prevTick && healthAnimationDelay > 0) healthAnimationDelay--
                prevTick = thePlayer.ticksExisted

                val healthText = "${if (targetHealthPercentage < 0.25) "\u00A7c" else if (targetHealthPercentage < 0.5) "\u00A7e" else "\u00A7a"}${DECIMALFORMAT_2.format(targetHealth.toDouble())} (${DECIMALFORMAT_1.format(targetHealthPercentage * 100.0)}%)\u00A7r"
                val armorText = "${if (targetArmor > 0) "\u00A7b" else "\u00A77"}$targetArmor (${DECIMALFORMAT_2.format(targetArmor / 20.0 * 100.0)}%)\u00A7r"

                val distanceText = DECIMALFORMAT_2.format(thePlayer.getDistanceToEntityBox(target))
                val yawText = "${DECIMALFORMAT_2.format(target.rotationYaw % 360f)} (${StringUtils.getHorizontalFacingAdv(target.rotationYaw)})"
                val pitchText = DECIMALFORMAT_2.format(target.rotationPitch)

                val velocityText = "${DECIMALFORMAT_2.format(target.motionX)}, ${DECIMALFORMAT_2.format(target.motionY)}, ${DECIMALFORMAT_2.format(target.motionZ)}"

                val healthColor = ColorUtils.getHealthColor(easingHealth, targetMaxHealth)

                val width = (textXOffset.toFloat() + nameFont.getStringWidth(name) + 10).coerceAtLeast(minWidth)

                // Draw Body Rect
                if (shouldDrawBorder)
                {
                    RainbowShader.begin(borderRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
                        RenderUtils.drawRect(-borderWidth, -borderWidth, width + borderWidth, height + borderWidth, borderColor)

                        if (drawDebug) RenderUtils.drawRect(width - borderWidth + 10f, -borderWidth, width + borderWidth + debugWidth, height + borderWidth, borderColor)
                    }
                }

                RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
                    RenderUtils.drawRect(0F, 0F, width, height, backgroundColor)

                    if (drawDebug) RenderUtils.drawRect(width + 10f, 0f, width + debugWidth, height, backgroundColor)
                }

                if (backgroundRainbowCeil) RainbowShader.begin(true, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
                    RenderUtils.drawRect(0F, -1F, width, 0F, 0)

                    if (drawDebug) RenderUtils.drawRect(width + 10f, -1f, width + debugWidth, 0f, 0)
                }

                val barWidthSubtractor = barWidthSubtractorValue.get().toFloat()
                val barWidth = width - barWidthSubtractor
                val gradationWidth = barWidth - barWidthSubtractor

                // Draw Absorption
                RenderUtils.drawRect(((easingHealth / targetMaxHealth) * barWidth) - ((easingAbsorption / targetMaxHealth) * barWidth), healthBarYOffset - 2, (easingHealth / targetMaxHealth) * barWidth, healthBarYOffset - 1, -256)

                // Draw Damage animation
                if (easingHealth > targetHealth) RenderUtils.drawRect(barWidthSubtractor, healthBarYOffset, max((easingHealth / targetMaxHealth) * barWidth, barWidthSubtractor), healthBarYOffset + 2, damageColor)

                // Draw Health bar
                RenderUtils.drawRect(barWidthSubtractor, healthBarYOffset, max(targetHealthPercentage * barWidth, barWidthSubtractor), healthBarYOffset + 2, healthColor.rgb)

                // Draw Heal animation
                if (easingHealth < targetHealth) RenderUtils.drawRect((easingHealth / targetMaxHealth) * barWidth, healthBarYOffset, targetHealthPercentage * barWidth, healthBarYOffset + 2, healColor)

                // Draw Health Gradations
                val limitedMaxHealth = targetMaxHealth.coerceAtMost(50F)
                val healthGradationGap = gradationWidth / limitedMaxHealth
                for (index in 1 until limitedMaxHealth.roundToInt()) RenderUtils.drawRect(healthGradationGap * index + barWidthSubtractor, healthBarYOffset - 2, healthGradationGap * index + 1 + barWidthSubtractor, healthBarYOffset + 2, -16777216)

                if (isPlayer)
                {
                    // Draw Head Box
                    RenderUtils.drawRect(2F, 2F, textXOffset - 4F, headBoxYSize, -12566464)

                    // Draw Total Armor bar
                    RenderUtils.drawRect(barWidthSubtractor, height - 2F /*108F*/, max(easingArmor * barWidth * 0.05f, barWidthSubtractor), height - 1F /*109F*/, -16711681)

                    // Draw Armor Gradations
                    val armorGradationGap = gradationWidth * 0.05f
                    for (index in 1 until 20) RenderUtils.drawRect(armorGradationGap * index + barWidthSubtractor, height - 2F /*108F*/, armorGradationGap * index + 1 + barWidthSubtractor, height - 1F /*109F*/, -16777216)

                    val skinResource: ResourceLocation
                    val ping: Int
                    val pingTextColor: Int

                    val playerInfo = netHandler.getPlayerInfo(target.uniqueID)
                    if (playerInfo != null)
                    {
                        ping = playerInfo.responseTime.coerceAtLeast(0)
                        pingTextColor = if (ping > 300) 0xff0000 else ColorUtils.blendColors(floatArrayOf(0.0F, 0.5F, 1.0F), arrayOf(Color.GREEN, Color.YELLOW, Color.RED), ping / 300.0F).rgb
                        skinResource = playerInfo.locationSkin
                    }
                    else
                    {
                        ping = -1
                        pingTextColor = 0x808080
                        skinResource = DefaultPlayerSkin.getDefaultSkin(target.uniqueID)
                    }

                    // Draw head
                    RenderUtils.resetColor()
                    textureManager.bindTexture(skinResource)
                    RenderUtils.drawScaledCustomSizeModalRect(4f, 4f, 8F, 8F, 8f, 8f, headRenderSize.toFloat(), headRenderSize.toFloat(), 64F, 64F)

                    // Reset color after drawing head
                    RenderUtils.glColor(Color.white)

                    val pingLevelImageID: Int = when
                    {
                        ping < 0L -> 5
                        ping < 150L -> 0
                        ping < 300L -> 1
                        ping < 600L -> 2
                        ping < 1000L -> 3
                        else -> 4
                    }

                    // Draw Ping level
                    textureManager.bindTexture(RenderUtils.ICONS)
                    RenderUtils.drawModalRectWithCustomSizedTexture(textXOffset.toFloat(), 20f, 0f, (176 + (pingLevelImageID shl 3)).toFloat(), 10f, 8f, 256f, 256f)

                    textYOffset = 20

                    // Draw Ping text
                    textFont.drawString("${ping}ms", textXOffset + 12, 22, pingTextColor)

                    easingArmor = easeOutCubic(easingArmor, targetArmor.toFloat(), armorFadeSpeedValue.get())
                }

                // Render equipments
                if (renderEquipmentsValue.get())
                {
                    textYOffset += 15

                    val prevZLevel = renderItem.zLevel
                    renderItem.zLevel = -147F

                    repeat(5) { index ->
                        val isHeldItem = index == 0

                        val equipmentX = textXOffset + (4 - index) * 20 + if (isHeldItem) 5 else 0

                        RenderUtils.drawRect(equipmentX, textYOffset, equipmentX + 16, textYOffset + 16, -12566464)

                        val armor = target.getEquipmentInSlot(index) ?: return@repeat

                        RenderUtils.glColor(Color.white) // Reset Color

                        RenderUtils.drawItemStack(armor, equipmentX, textYOffset)
                    }

                    renderItem.zLevel = prevZLevel
                }

                RenderUtils.glColor(Color.white) // Reset Color

                if (!suspendAnimation) easingHealth = easeOutCubic(easingHealth, targetHealth, healthFadeSpeedValue.get())
                easingAbsorption = easeOutCubic(easingAbsorption, targetAbsorption, absorptionFadeSpeedValue.get())

                // Draw Target Name
                nameFont.drawString(name, textXOffset, 3, 0xffffff)

                // Render Target Stats

                val scale = textScaleValue.get()
                val reverseScale = 1.0F / scale

                // val yPos = max(headRenderSize + 10f, textYOffset + 20f)
                val min = min(textYOffset + 20f, 45f)
                val scaledXPos = (if (headRenderSize <= min) 2 else textXOffset) * reverseScale
                val scaledYPos = (textYOffset + 20f + textYOffsetValue.get()) * reverseScale

                GL11.glScalef(scale, scale, scale)

                // Draw CustomNameTag
                if (target.customNameTag.isNotBlank()) textFont.drawString("(${target.customNameTag})", textXOffset * reverseScale, (nameFont.FONT_HEIGHT + 5) * reverseScale, Color.gray.rgb)

                textFont.drawString(arrayOf(

                    "${if (verbose) "Health" else "h"}: $healthText", // Health
                    "${if (verbose) "Absorption" else "h_a"}: ${if (targetAbsorption > 0) "\u00A7e" else "\u00A77"}${DECIMALFORMAT_1.format(targetAbsorption)}\u00A7r", // Absorption
                    "${if (verbose) "Armor" else "a"}: $armorText" // Armor
                ).joinToString(separator = " | "), scaledXPos, scaledYPos, 0xffffff)

                textFont.drawString(arrayOf(

                    "${if (verbose) "Distance" else "d"}: $distanceText${if (verbose) "m" else ""}", // Distance
                    // TODO: Simplify duplicate codes
                    if (verbose) "${if (target.onGround) "\u00A7a" else "\u00A7c"}Ground\u00A7r" else "g: ${target.onGround}", // Ground
                    if (verbose) "${if (!target.isSprinting) "\u00A7c" else "\u00A7a"}Sprinting\u00A7r" else "sp: ${target.isSprinting}", // Sprinting
                    if (verbose) "${if (!target.isSneaking) "\u00A7c" else "\u00A7a"}Sneaking\u00A7r" else "sn: ${target.isSneaking}" // Sneaking
                ).joinToString(separator = " | "), scaledXPos, scaledYPos + 12, 0xffffff)

                textFont.drawString(arrayOf(

                    "${if (verbose) "Yaw" else "y"}: $yawText", // Yaw
                    "${if (verbose) "Pitch" else "p"}: $pitchText", // Pitch
                    "${if (verbose) "Velocity" else "v"}: [$velocityText]" // Velocity
                ).joinToString(separator = " | "), scaledXPos, scaledYPos + 24, 0xffffff)

                // Hurt-related
                textFont.drawString(arrayOf(

                    if (verbose) "HurtTime: ${if (target.hurtTime > 0) "\u00A7c" else "\u00A7a"}${target.hurtTime}\u00A7r" else "ht: ${target.hurtTime}", // HurtTime
                    if (verbose) "HurtResisTime: ${if (target.hurtResistantTime > 0) "\u00A7c" else "\u00A7a"}${target.hurtResistantTime}\u00A7r" else "hrt: ${target.hurtResistantTime}" // HurtResistantTime
                ).joinToString(separator = " | "), scaledXPos, scaledYPos + 34, 0xffffff)

                // Metadata-related
                textFont.drawString("EntityID: ${target.entityId}$metaDataBuilder", scaledXPos, scaledYPos + 44, 0xffffff)

                GL11.glScalef(reverseScale, reverseScale, reverseScale)

                if (drawDebug)
                {
                    val debugScale = debugPanelTextScaleValue.get()
                    val reverseDebugScale = 1.0F / debugScale

                    val debugFont = debugPanelTextFontValue.get()
                    val scaledDebugXPos = (width + borderWidth + 15f) * reverseDebugScale
                    val scaledDebugYPos = 5f * reverseDebugScale

                    // debug data
                    GL11.glScalef(debugScale, debugScale, debugScale)

                    debug?.let {
                        var yOffset = 10
                        for (arr in debug)
                        {
                            for (string in arr)
                            {
                                debugFont.drawString(string, scaledDebugXPos, scaledDebugYPos + yOffset, 0xffffff)
                                yOffset += 10
                            }
                            yOffset += 8
                        }
                    }

                    GL11.glScalef(reverseDebugScale, reverseDebugScale, reverseDebugScale)
                }
            }
        }

        lastTarget = target

        val borderExpanded = if (shouldDrawBorder) borderWidth else 0F
        return Border(0F - borderExpanded, 0F - borderExpanded, minWidth + borderExpanded, height + borderExpanded)
    }

    private fun queryTarget(): Pair<EntityLivingBase?, Array<Array<String>>?>?
    {
        val moduleManager = LiquidBounce.moduleManager

        val tpAura = moduleManager[TpAura::class.java] as TpAura
        val killAura = moduleManager[KillAura::class.java] as KillAura
        val aimbot = moduleManager[Aimbot::class.java] as Aimbot
        val bowAimbot = moduleManager[BowAimbot::class.java] as BowAimbot

        val killAuraTarget = killAura.target
        val aimbotTarget = aimbot.target
        val bowAimbotTarget = bowAimbot.target

        return when
        {
            tpAura.state && tpAura.maxTargetsValue.get() == 1 && tpAura.currentTarget != null -> tpAura.currentTarget to arrayOf(arrayOf("type=TpAura${tpAura.debug?.let { ", $it" }}"))
            killAuraTarget != null -> killAuraTarget to run {
                val list = mutableListOf(arrayOf("type" equalTo "KillAura"))
                killAura.updateHitableDebug?.let { list.add(arrayOf("updateHitable", *it)) }
                killAura.updateRotationsDebug?.let { list.add(arrayOf("updateRotations", *it)) }
                killAura.startBlockingDebug?.let { list.add(arrayOf("startBlocking", *it)) }
                list.toTypedArray()
            }
            aimbotTarget != null -> aimbotTarget to arrayOf(arrayOf("type=Aimbot"))
            bowAimbotTarget != null -> bowAimbotTarget to arrayOf(arrayOf("type=BowAimbot"))
            else -> null
        }
    }
}
