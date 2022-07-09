/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.*

val smoothingModes = arrayOf("None", "Root", "Power")

// TODO: Astolfo block animation
@ModuleInfo(name = "SwingAnimation", description = "Customize swing animation.", category = ModuleCategory.RENDER)
class SwingAnimation : Module()
{
    private val equipProgress = ValueGroup("EquipProgress")
    val equipProgressMultiplier = FloatValue("Multiplier", 1f, 0f, 2f, "EquipProgressMultiplier")

    private val equipProgressSmoothingGroup = ValueGroup("Smoothing")
    val equipProgressSmoothingModeValue = ListValue("Mode", arrayOf("None", "Linear", "Square", "Cube", "Quadratic-Function", "Reverse-Quadratic-Function"), "None", "EquipProgressSmoothing")
    val equipProgressSmoothingSpeedModifierValue = object : IntegerValue("SpeedModifier", 1, 0, 2, "EquipProgressSmoothingSpeedModifier")
    {
        override fun showCondition(): Boolean = equipProgressSmoothingModeValue.get().equals("Reverse-Quadratic-Function", ignoreCase = true)
    }
    val equipProgressSmoothingDownSpeedMultiplierValue = FloatValue("DownMultiplier", 2.4F, 0.2F, 5F, "EquipProgressSmoothDownSpeedMultiplier")
    val equipProgressSmoothingDownSpeedValue = object : IntegerValue("Down", 6, 1, 10, "EquipProgressSmoothDownSpeed")
    {
        override fun showCondition(): Boolean = equipProgressSmoothingModeValue.get().endsWith("Function", ignoreCase = true)
    }
    val equipProgressSmoothingUpSpeedMultiplierValue = FloatValue("UpMultiplier", 1.2F, 0.2F, 5F, "EquipProgressSmoothUpSpeedMultiplier")
    val equipProgressSmoothingUpSpeedValue = object : IntegerValue("Up", 3, 1, 10, "EquipProgressSmoothUpSpeed")
    {
        override fun showCondition(): Boolean = equipProgressSmoothingModeValue.get().endsWith("Function", ignoreCase = true)
    }

    private val equipProgressInfluenceSwingProgressGroup = ValueGroup("InfluencesSwingProgress")
    val equipProgressInfluenceSwingProgressAffectEnabled = BoolValue("Enabled", false, "EquipProgressAffectsBlockSwingAnimationIntensity")
    val equipProgressInfluenceSwingProgressAffectAffectness = IntegerValue("Affectness", 100, 1, 100, "EquipProgressBlockSwingAnimationAffect")

    private val equipProgressInfluenceTranslationGroup = ValueGroup("InfluencesTranslation")
    val equipProgressInfluenceTranslationEnabled = BoolValue("Enabled", false, "EquipProgressAffectsBlockSwingAnimationTranslation")
    val equipProgressInfluenceTranslationAffectnessX = FloatValue("AffectnessX", 0f, 0f, 1f, "EquipProgressAnimationTranslationAffectnessX")
    val equipProgressInfluenceTranslationAffectnessY = FloatValue("AffectnessY", .6f, 0f, 1f, "EquipProgressAnimationTranslationAffectnessY")
    val equipProgressInfluenceTranslationAffectnessZ = FloatValue("AffectnessZ", 0f, 0f, 1f, "EquipProgressAnimationTranslationAffectnessZ")

    private val equipProgressBoostSmoothingExponentGroup = ValueGroup("BoostsSmoothingExponent")
    val equipProgressBoostSmoothingExponentAmount = FloatValue("Amount", 0f, -2f, 2f)
    val equipProgressBoostSmoothingExponentReturnStep = FloatValue("ReturnStep", 0.5f, 0.05f, 2f)
    val equipProgressBoostSmoothingExponentReturnDelay = IntegerValue("ReturnDelay", 4, 1, 20)

    private val swingSpeedBoostGroup = ValueGroup("SwingSpeedBoostAfterReequip")
    val swingSpeedBoostAmount = IntegerValue("Amount", 0, -5, 6, "SwingSpeedBoostAfterReequip")
    val swingSpeedBoostReturnDelay = IntegerValue("ReturnDelay", 4, 1, 20, "SwingSpeedBoostAfterReequip")

    private val swingProgressEndBoostGroup = ValueGroup("SwingProgressEndBoostAfterReequip")
    val swingProgressEndBoostAmount = IntegerValue("Amount", 0, -6, 6)
    val swingProgressEndBoostReturnDelay = IntegerValue("ReturnDelay", 4, 1, 20)

    private val swingSpeedGroup = ValueGroup("SwingSpeed")
    val swingSpeedSwingSpeed = IntegerValue("SwingSpeed", 0, -4, 20, "SwingSpeed")
    val swingSpeedSwingProgressLimit: IntegerValue = object : IntegerValue("SwingProgressEnd", 3, 1, 20, "SwingProgressLimit")
    {
        override fun onChanged(oldValue: Int, newValue: Int)
        {
            val i = swingSpeedSwingSpeed.get() + 8
            if (newValue > i) this.set(i)
        }
    }

    private val swingGroup = ValueGroup("Swing")

    private val swingTranslationGroup = ValueGroup("Translation")

    private val swingTranslationAbsoluteGroup = ValueGroup("Absolute")
    val swingTranslationAbsoluteX = FloatValue("X", 0f, -1f, 1f, "X-Translation")
    val swingTranslationAbsoluteY = FloatValue("Y", 0f, -1f, 1f, "Y-Translation")
    val swingTranslationAbsoluteZ = FloatValue("Z", 0f, -1f, 1f, "Z-Translation")

    private val swingTranslationRelativeGroup = ValueGroup("Relative")

    private val swingTranslationRelativeXGroup = ValueGroup("X")
    val swingTranslationRelativeX = FloatValue("Value", 0f, -1f, 1f, "X-RelTranslation")
    val swingTranslationRelativeXMode = ListValue("Mode", smoothingModes, "Root", "X-RelTranslationSmoothingMethod")
    val swingTranslationRelativeXExp = FloatValue("Exponent", 2f, 1f, 5f)
    val swingTranslationRelativeXReverse = BoolValue("Reverse", false)

    private val swingTranslationRelativeYGroup = ValueGroup("Y")
    val swingTranslationRelativeY = FloatValue("Value", 0f, -1f, 1f, "Y-RelTranslation")
    val swingTranslationRelativeYMode = ListValue("Mode", smoothingModes, "Sqrt", "Y-RelTranslationSmoothingMethod")
    val swingTranslationRelativeYExp = FloatValue("Exponent", 2f, 1f, 5f)
    val swingTranslationRelativeYReverse = BoolValue("Reverse", false)

    private val swingTranslationRelativeZGroup = ValueGroup("Z")
    val swingTranslationRelativeZ = FloatValue("Value", 0f, -1f, 1f, "Z-RelTranslation")
    val swingTranslationRelativeZMode = ListValue("Mode", smoothingModes, "Sqrt", "Z-RelTranslationSmoothingMethod")
    val swingTranslationRelativeZExp = FloatValue("Exponent", 2f, 1f, 5f)
    val swingTranslationRelativeZReverse = BoolValue("Reverse", false)

    val swingScale = FloatValue("Scale", .4f, 0.2f, 0.6f, "Scale")

    private val swingSmoothingGroup = ValueGroup("Smoothing")

    private val swingSmoothingSqGroup = ValueGroup("Sq")
    val swingSmoothingSqMode = ListValue("Mode", smoothingModes, "Power", "SwingAnimationSqSmoothingMethod")
    val swingSmoothingSqExp = FloatValue("Exponent", 2f, 1f, 5f)

    private val swingSmoothingSqrtGroup = ValueGroup("Sqrt")
    val swingSmoothingSqrtMode = ListValue("Mode", smoothingModes, "Root", "SwingAnimationSqrtSmoothingMethod")
    val swingSmoothingSqrtExp = FloatValue("Exponent", 2f, 1f, 5f)

    val smoothSwing = BoolValue("SmoothSwing", false)

    private val swingStaticSwingProgressGroup = ValueGroup("Static")
    val swingStaticSwingProgressEnabled = BoolValue("Enabled", false, "StaticSwingProgress")
    val swingStaticSwingProgressProgress = FloatValue("Progress", .64f, .11f, .99f, "StaticSwingProgress")

    private val blockGroup = ValueGroup("Block")

    private val blockTranslationGroup = ValueGroup("Translation")

    private val blockTranslationAbsoluteGroup = ValueGroup("Absolute")
    val blockTranslationAbsoluteX = FloatValue("X", 0f, -1f, 1f, "Block-X-Translation")
    val blockTranslationAbsoluteY = FloatValue("Y", 0f, -1f, 1f, "Block-Y-Translation")
    val blockTranslationAbsoluteZ = FloatValue("Z", 0f, -1f, 1f, "Block-Z-Translation")

    private val blockTranslationRelativeGroup = ValueGroup("Relative")

    private val blockTranslationRelativeXGroup = ValueGroup("X")
    val blockTranslationRelativeX = FloatValue("Value", 0f, -1f, 1f, "X-RelTranslation")
    val blockTranslationRelativeXMode = ListValue("Mode", smoothingModes, "Root", "X-RelTranslationSmoothingMethod")
    val blockTranslationRelativeXExp = FloatValue("Exponent", 2f, 1f, 5f)
    val blockTranslationRelativeXReverse = BoolValue("Reverse", false)

    private val blockTranslationRelativeYGroup = ValueGroup("Y")
    val blockTranslationRelativeY = FloatValue("Value", 0f, -1f, 1f, "Y-RelTranslation")
    val blockTranslationRelativeYMode = ListValue("Mode", smoothingModes, "Sqrt", "Y-RelTranslationSmoothingMethod")
    val blockTranslationRelativeYExp = FloatValue("Exponent", 2f, 1f, 5f)
    val blockTranslationRelativeYReverse = BoolValue("Reverse", false)

    private val blockTranslationRelativeZGroup = ValueGroup("Z")
    val blockTranslationRelativeZ = FloatValue("Value", 0f, -1f, 1f, "Z-RelTranslation")
    val blockTranslationRelativeZMode = ListValue("Mode", smoothingModes, "Sqrt", "Z-RelTranslationSmoothingMethod")
    val blockTranslationRelativeZExp = FloatValue("Exponent", 2f, 1f, 5f)
    val blockTranslationRelativeZReverse = BoolValue("Reverse", false)

    val blockScale = FloatValue("Scale", .4f, 0.2f, 0.6f, "Block-Scale")

    val blockAngle = FloatValue("Angle", 80f, 45f, 135f, "BlockAngle")

    private val blockSmoothingGroup = ValueGroup("Smoothing")

    private val blockSmoothingSqGroup = ValueGroup("Sq")
    val blockSmoothingSqMode = ListValue("Mode", smoothingModes, "Power", "blockAnimationSqSmoothingMethod")
    val blockSmoothingSqExp = FloatValue("Exponent", 2f, 1f, 5f)

    private val blockSmoothingSqrtGroup = ValueGroup("Sqrt")
    val blockSmoothingSqrtMode = ListValue("Mode", smoothingModes, "Root", "blockAnimationSqrtSmoothingMethod")
    val blockSmoothingSqrtExp = FloatValue("Exponent", 2f, 1f, 5f)

    private val blockStaticSwingProgressGroup = ValueGroup("Static")
    val blockStaticSwingProgressEnabled = BoolValue("Enabled", false, "Block-StaticSwingProgress")
    val blockStaticSwingProgressProgress = FloatValue("Progress", .64f, .11f, .99f, "Block-StaticSwingProgress")

    private val blockAnimationGroup = ValueGroup("Animation")
    val blockAnimationMode = ListValue("Mode", arrayOf("LiquidBounce", "1.8", "1.7", "Push", "Tap", "Tap2", "Avatar", "Sigma", "Slide", "Exhibobo", "Lucid", "Luna", "Hooded", "Bump", "Slap", "Eat"), "LiquidBounce", "BlockSwingAnimation")

    // Sword Block Animation Options
    private val blockAnimationSlideGroup: ValueGroup = object : ValueGroup("Slide")
    {
        override fun showCondition(): Boolean = blockAnimationMode.get().equals("Slide", ignoreCase = true)
    }
    val blockAnimationSlideAngleX = FloatValue("AngleX", 40f, -30f, 80f, "Slide-AngleX")
    val blockAnimationSlidePosX = FloatValue("PosX", 1f, 0f, 20f)
    val blockAnimationSlideAngleY = FloatValue("AngleY", 0f, 0f, 135f, "Slide-AngleY")
    val blockAnimationSlidePosY = FloatValue("PosY", 1f, 0f, 20f)
    val blockAnimationSlideAngleZ = FloatValue("AngleZ", 15f, 0f, 135f, "Slide-AngleZ")
    val blockAnimationSlidePosZ = FloatValue("PosZ", 1f, 0f, 20f)
    val blockAnimationSlidePushX = FloatValue("PushX", -10f, -100f, 100f, "Slide-X-Pos")
    val blockAnimationSlidePushY = FloatValue("PushY", 13f, -5f, 30f, "Slide-Y-Pos")
    val blockAnimationSlidePushZ = FloatValue("PushZ", 0f, -5f, 30f)

    private val blockAnimationExhiGroup: ValueGroup = object : ValueGroup("Exhibobo")
    {
        override fun showCondition(): Boolean = blockAnimationMode.get().equals("Exhibobo", ignoreCase = true)
    }
    val blockAnimationExhiAngleX = FloatValue("AngleX", 30f, 0f, 45f, "Exhibobo-SwingAngle")
    val blockAnimationExhiPosX = FloatValue("PosX", 1f, 0f, 20f)
    val blockAnimationExhiAngleY = FloatValue("AngleY", 6.25f, -10f, 30f, "Exhibobo-PushDepth")
    val blockAnimationExhiPosY = FloatValue("PosY", 1f, 0f, 20f)
    val blockAnimationExhiAngleZ = FloatValue("AngleZ", 12.5f, -10f, 30f, "Exhibobo-Slope")
    val blockAnimationExhiPosZ = FloatValue("PosZ", 1f, 0f, 20f)
    val blockAnimationExhiPushXPos = FloatValue("PushX", 45f, 0f, 90f)
    val blockAnimationExhiPushYPos = FloatValue("PushY", 40f, 0f, 90f, "Exhibobo-Y-PushPos")
    val blockAnimationExhiPushZPos = FloatValue("PushZ", 20f, 0f, 90f, "Exhibobo-Z-PushPos")
    val blockAnimationExhiSmoothX = FloatValue("SmoothX", 0f, -25f, 25f)
    val blockAnimationExhiSmoothY = FloatValue("SmoothY", 5f, -25f, 25f, "Exhibobo-Smooth")
    val blockAnimationExhiSmoothZ = FloatValue("SmoothZ", 0f, -25f, 25f)

    init
    {
        equipProgressSmoothingGroup.addAll(equipProgressSmoothingModeValue, equipProgressSmoothingSpeedModifierValue, equipProgressSmoothingDownSpeedMultiplierValue, equipProgressSmoothingDownSpeedValue, equipProgressSmoothingUpSpeedMultiplierValue, equipProgressSmoothingUpSpeedValue)
        equipProgressInfluenceSwingProgressGroup.addAll(equipProgressInfluenceSwingProgressAffectEnabled, equipProgressInfluenceSwingProgressAffectAffectness)
        equipProgressInfluenceTranslationGroup.addAll(equipProgressInfluenceTranslationEnabled, equipProgressInfluenceTranslationAffectnessX, equipProgressInfluenceTranslationAffectnessY, equipProgressInfluenceTranslationAffectnessZ)
        equipProgressBoostSmoothingExponentGroup.addAll(equipProgressBoostSmoothingExponentAmount, equipProgressBoostSmoothingExponentReturnStep, equipProgressBoostSmoothingExponentReturnDelay)

        equipProgress.addAll(equipProgressMultiplier, equipProgressSmoothingGroup, equipProgressInfluenceSwingProgressGroup, equipProgressInfluenceTranslationGroup, equipProgressBoostSmoothingExponentGroup)
        swingSpeedBoostGroup.addAll(swingSpeedBoostAmount, swingSpeedBoostReturnDelay)
        swingProgressEndBoostGroup.addAll(swingProgressEndBoostAmount, swingProgressEndBoostReturnDelay)
        swingSpeedGroup.addAll(swingSpeedBoostGroup, swingProgressEndBoostGroup, swingSpeedSwingSpeed, swingSpeedSwingProgressLimit)

        swingTranslationGroup.addAll(swingTranslationAbsoluteGroup, swingTranslationRelativeGroup)
        swingTranslationAbsoluteGroup.addAll(swingTranslationAbsoluteX, swingTranslationAbsoluteY, swingTranslationAbsoluteZ)

        swingTranslationRelativeXGroup.addAll(swingTranslationRelativeX, swingTranslationRelativeXMode, swingTranslationRelativeXExp, swingTranslationRelativeXReverse)
        swingTranslationRelativeYGroup.addAll(swingTranslationRelativeY, swingTranslationRelativeYMode, swingTranslationRelativeYExp, swingTranslationRelativeYReverse)
        swingTranslationRelativeZGroup.addAll(swingTranslationRelativeZ, swingTranslationRelativeZMode, swingTranslationRelativeZExp, swingTranslationRelativeZReverse)
        swingTranslationRelativeGroup.addAll(swingTranslationRelativeXGroup, swingTranslationRelativeYGroup, swingTranslationRelativeZGroup)

        swingSmoothingSqGroup.addAll(swingSmoothingSqMode, swingSmoothingSqExp)
        swingSmoothingSqrtGroup.addAll(swingSmoothingSqrtMode, swingSmoothingSqrtExp)
        swingSmoothingGroup.addAll(swingSmoothingSqGroup, swingSmoothingSqrtGroup)

        swingStaticSwingProgressGroup.addAll(swingStaticSwingProgressEnabled, swingStaticSwingProgressProgress)

        swingGroup.addAll(swingTranslationGroup, swingScale, swingSmoothingGroup, swingStaticSwingProgressGroup)

        blockTranslationAbsoluteGroup.addAll(blockTranslationAbsoluteX, blockTranslationAbsoluteY, blockTranslationAbsoluteZ)
        blockTranslationRelativeXGroup.addAll(blockTranslationRelativeX, blockTranslationRelativeXMode, blockTranslationRelativeXExp, blockTranslationRelativeXReverse)
        blockTranslationRelativeYGroup.addAll(blockTranslationRelativeY, blockTranslationRelativeYMode, blockTranslationRelativeYExp, blockTranslationRelativeYReverse)
        blockTranslationRelativeZGroup.addAll(blockTranslationRelativeZ, blockTranslationRelativeZMode, blockTranslationRelativeZExp, blockTranslationRelativeZReverse)
        blockTranslationRelativeGroup.addAll(blockTranslationRelativeXGroup, blockTranslationRelativeYGroup, blockTranslationRelativeZGroup)
        blockTranslationGroup.addAll(blockTranslationAbsoluteGroup, blockTranslationRelativeGroup)
        blockSmoothingSqGroup.addAll(blockSmoothingSqMode, blockSmoothingSqExp)
        blockSmoothingSqrtGroup.addAll(blockSmoothingSqrtMode, blockSmoothingSqrtExp)
        blockSmoothingGroup.addAll(blockSmoothingSqGroup, blockSmoothingSqrtGroup)
        blockStaticSwingProgressGroup.addAll(blockStaticSwingProgressEnabled, blockStaticSwingProgressProgress)

        blockAnimationSlideGroup.addAll(blockAnimationSlideAngleX, blockAnimationSlidePosX, blockAnimationSlideAngleY, blockAnimationSlidePosY, blockAnimationSlideAngleZ, blockAnimationSlidePosZ, blockAnimationSlidePushX, blockAnimationSlidePushY, blockAnimationSlidePushZ)
        blockAnimationExhiGroup.addAll(blockAnimationExhiAngleX, blockAnimationExhiPosX, blockAnimationExhiAngleY, blockAnimationExhiPosY, blockAnimationExhiAngleZ, blockAnimationExhiPosZ, blockAnimationExhiPushXPos, blockAnimationExhiPushYPos, blockAnimationExhiPushZPos, blockAnimationExhiSmoothX, blockAnimationExhiSmoothY, blockAnimationExhiSmoothZ)

        blockAnimationGroup.addAll(blockAnimationMode, blockAnimationSlideGroup, blockAnimationExhiGroup)

        blockGroup.addAll(blockAngle, blockTranslationGroup, blockScale, blockSmoothingGroup, blockStaticSwingProgressGroup, blockAnimationGroup)
    }

    @JvmField
    var swingSpeedBoost = 0

    @JvmField
    var swingProgressEndBoost = 0

    @JvmField
    var exponentBoost = 0f

    override val tag: String
        get() = "${blockAnimationMode.get()}${if (swingStaticSwingProgressEnabled.get()) " static_" + swingStaticSwingProgressProgress.get() else ""}${if (blockStaticSwingProgressEnabled.get()) " blockstatic_" + blockStaticSwingProgressProgress.get() else ""}"
}
