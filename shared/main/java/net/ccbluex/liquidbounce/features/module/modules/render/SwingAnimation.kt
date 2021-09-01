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

val smoothingMethods = arrayOf("None", "Reverse", "Sqrt", "SqrtSqrt", "Sq", "SqSq", "Linear")

// TODO: Astolfo block animation
@ModuleInfo(name = "SwingAnimation", description = "Customize swing animation.", category = ModuleCategory.RENDER)
class SwingAnimation : Module()
{
	private val equipProgress = ValueGroup("EquipProgress")
	val equipProgressMultiplier = FloatValue("Multiplier", 1f, 0f, 2f, "EquipProgressMultiplier")

	private val equipProgressSmoothingGroup = ValueGroup("Smoothing")
	val equipProgressSmoothingModeValue = ListValue("Mode", arrayOf("None", "Linear", "Square", "Cube", "Quadratic-Function", "Reverse-Quadratic-Function"), "None", "EquipProgressSmoothing")
	val equipProgressSmoothingSpeedModifierValue = IntegerValue("SpeedModifier", 1, 0, 2, "EquipProgressSmoothingSpeedModifier")
	val equipProgressSmoothingDownSpeedMultiplierValue = FloatValue("DownMultiplier", 2.4F, 0.2F, 5F, "EquipProgressSmoothDownSpeedMultiplier")
	val equipProgressSmoothingDownSpeedValue = IntegerValue("Down", 6, 1, 10, "EquipProgressSmoothDownSpeed")
	val equipProgressSmoothingUpSpeedMultiplierValue = FloatValue("UpMultiplier", 1.2F, 0.2F, 5F, "EquipProgressSmoothUpSpeedMultiplier")
	val equipProgressSmoothingUpSpeedValue = IntegerValue("Up", 3, 1, 10, "EquipProgressSmoothUpSpeed")

	private val equipProgressSwingProgressAffectGroup = ValueGroup("SwingProgressAffect")
	val equipProgressSwingProgressAffectEnabled = BoolValue("Enabled", false, "EquipProgressAffectsBlockSwingAnimationIntensity")
	val equipProgressSwingProgressAffectAffectness = IntegerValue("Affectness", 100, 1, 100, "EquipProgressBlockSwingAnimationAffect")

	private val equipProgressTranslationAffectGroup = ValueGroup("TranslationAffect")
	val equipProgressTranslationAffectEnabled = BoolValue("Enabled", false, "EquipProgressAffectsBlockSwingAnimationTranslation")
	val equipProgressTranslationAffectAffectnessX = FloatValue("AffectnessX", 0f, 0f, 1f, "EquipProgressAnimationTranslationAffectnessX")
	val equipProgressTranslationAffectAffectnessY = FloatValue("AffectnessY", .6f, 0f, 1f, "EquipProgressAnimationTranslationAffectnessY")
	val equipProgressTranslationAffectAffectnessZ = FloatValue("AffectnessZ", 0f, 0f, 1f, "EquipProgressAnimationTranslationAffectnessZ")

	private val swingSpeedBoostGroup = ValueGroup("SwingSpeedBoostAfterReequip")
	val swingSpeedBoostAmount = IntegerValue("Amount", 0, -5, 6, "SwingSpeedBoostAfterReequip")
	val swingSpeedBoostFadeTicks = IntegerValue("FadeTicks", 4, 2, 20, "SwingSpeedBoostAfterReequip")

	private val swingSpeedGroup = ValueGroup("SwingSpeed")
	val swingSpeedEnabled = BoolValue("Enabled", false, "CustomSwingSpeed")
	val swingSpeedSwingSpeed = IntegerValue("SwingSpeed", 0, -4, 20, "SwingSpeed")
	val swingSpeedSwingProgressLimit: IntegerValue = object : IntegerValue("SwingProgressLimit", 3, 1, 20, "SwingProgressLimit")
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			if (newValue > 8 + (if (swingSpeedEnabled.get()) swingSpeedSwingSpeed.get() else 0)) this.set(8 + if (swingSpeedEnabled.get()) swingSpeedSwingSpeed.get() else 0)
		}
	}

	private val swingGroup = ValueGroup("Swing")

	private val swingTranslationGroup = ValueGroup("Translation")
	val swingTranslationX = FloatValue("X", 0f, -1f, 1f, "X-Translation")
	val swingTranslationY = FloatValue("Y", 0f, -1f, 1f, "Y-Translation")
	val swingTranslationZ = FloatValue("Z", 0f, -1f, 1f, "Z-Translation")

	private val swingRelTranslationGroup = ValueGroup("RelativeTranslation")
	val swingRelTranslationX = FloatValue("X", 0f, -1f, 1f, "X-RelTranslation")
	val swingRelTranslationXSmoothing = ListValue("X-Smoothing", smoothingMethods, "Sqrt", "X-RelTranslationSmoothingMethod")
	val swingRelTranslationY = FloatValue("Y", 0f, -1f, 1f, "Y-RelTranslation")
	val swingRelTranslationYSmoothing = ListValue("Y-Smoothing", smoothingMethods, "Sqrt", "Y-RelTranslationSmoothingMethod")
	val swingRelTranslationZ = FloatValue("Z", 0f, -1f, 1f, "Z-RelTranslation")
	val swingRelTranslationZSmoothing = ListValue("Z-Smoothing", smoothingMethods, "Sqrt", "Z-RelTranslationSmoothingMethod")

	val swingScale = FloatValue("Scale", .4f, 0.2f, 0.6f, "Scale")

	private val swingSmoothingGroup = ValueGroup("Smoothing")
	val swingSmoothingSq = ListValue("Sq", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sq", "SwingAnimationSqSmoothingMethod")
	val swingSmoothingSqSin = BoolValue("Sq-Sinusoidal", true, "SwingAnimationSqSmoothingBack")
	val swingSmoothingSqrt = ListValue("Sqrt", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sqrt", "SwingAnimationSqrtSmoothingMethod")
	val swingSmoothingSqrtSin = BoolValue("Sqrt-Sinusoidal", true, "SwingAnimationSqrtSmoothingBack")

	val smoothSwing = BoolValue("SmoothSwing", false)

	private val swingStaticSwingProgressGroup = ValueGroup("StaticSwingProgress")
	val swingStaticSwingProgressEnabled = BoolValue("Enabled", false, "StaticSwingProgress")
	val swingStaticSwingProgressProgress = FloatValue("Progress", .64f, .11f, .99f, "StaticSwingProgress")

	private val blockGroup = ValueGroup("Block")

	private val blockTranslationGroup = ValueGroup("Translation")
	val blockTranslationX = FloatValue("X", 0f, -1f, 1f, "Block-X-Translation")
	val blockTranslationY = FloatValue("Y", 0f, -1f, 1f, "Block-Y-Translation")
	val blockTranslationZ = FloatValue("Z", 0f, -1f, 1f, "Block-Z-Translation")

	private val blockRelTranslationGroup = ValueGroup("RelativeTranslation")

	/* TIP: I recommand -0.01 for Slide mode; zero or higher value for EXHIBOBO mode */
	val blockRelTranslationX = FloatValue("X", 0.02f, -1f, 1f, "Block-X-RelTranslation")
	val blockRelTranslationXSmoothing = ListValue("X-Smoothing", smoothingMethods, "Sqrt", "Block-X-RelTranslationSmoothingMethod")

	/* TIP: I recommend 0.13 for Slide mode; zero or lower value for EXHIBOBO mode */
	val blockRelTranslationY = FloatValue("Y", 0.13f, -1f, 1f, "Block-Y-RelTranslation")
	val blockRelTranslationYSmoothing = ListValue("Y-Smoothing", smoothingMethods, "Sqrt", "Block-Y-RelTranslationSmoothingMethod")

	val blockRelTranslationZ = FloatValue("Z", 0f, -1f, 1f, "Block-Z-RelTranslation")
	val blockRelTranslationZSmoothing = ListValue("Z-Smoothing", smoothingMethods, "Sqrt", "Block-Z-RelTranslationSmoothingMethod")

	val blockScale = FloatValue("Scale", .4f, 0.2f, 0.6f, "Block-Scale")

	val blockAngle = FloatValue("Angle", 80f, 45f, 135f, "BlockAngle")

	private val blockSmoothingGroup = ValueGroup("Smoothing")
	val blockSmoothingSq = ListValue("Sq", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sq", "BlockAnimationSqSmoothingMethod")
	val blockSmoothingSqSin = BoolValue("Sq-Sinusoidal", true, "BlockAnimationSqSmoothingBack")
	val blockSmoothingSqrt = ListValue("Sqrt", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sqrt", "BlockAnimationSqrtSmoothingMethod")
	val blockSmoothingSqrtSin = BoolValue("Sqrt-Sinusoidal", true, "BlockAnimationSqrtSmoothingBack")

	private val blockStaticSwingProgressGroup = ValueGroup("StaticSwingProgress")
	val blockStaticSwingProgressEnabled = BoolValue("Enabled", false, "Block-StaticSwingProgress")
	val blockStaticSwingProgressProgress = FloatValue("Progress", .64f, .11f, .99f, "Block-StaticSwingProgress")

	private val blockAnimationGroup = ValueGroup("Animation")
	val blockAnimationMode = ListValue("Mode", arrayOf("LiquidBounce", "1.8", "1.7", "Push", "Tap", "Tap2", "Avatar", "Sigma", "Slide", "Exhibobo", "Lucid", "Luna", "Hooded", "Bump", "Slap"), "LiquidBounce", "BlockSwingAnimation")

	// Sword Block Animation Options
	private val blockAnimationSlideGroup: ValueGroup = object : ValueGroup("Slide")
	{
		override fun showCondition(): Boolean = blockAnimationMode.get().equals("Slide", ignoreCase = true)
	}
	val blockAnimationSlideAngleX = FloatValue("AngleX", 40f, -30f, 80f, "Slide-AngleX")
	val blockAnimationSlideAngleY = FloatValue("AngleY", 10f, 0f, 135f, "Slide-AngleY")
	val blockAnimationSlideAngleZ = FloatValue("AngleZ", 15f, 0f, 135f, "Slide-AngleZ")
	val blockAnimationSlideXPos = IntegerValue("PosX", -10, -100, 100, "Slide-X-Pos")
	val blockAnimationSlideYPos = IntegerValue("PosY", 13, -5, 30, "Slide-Y-Pos")

	private val blockAnimationExhiGroup: ValueGroup = object : ValueGroup("Exhibobo")
	{
		override fun showCondition(): Boolean = blockAnimationMode.get().equals("Exhibobo", ignoreCase = true)
	}
	val blockAnimationExhiAngleX = FloatValue("SwingAngle", 15f, 0f, 30f, "Exhibobo-SwingAngle")
	val blockAnimationExhiAngleY = FloatValue("PushDepth", 0f, -10f, 20f, "Exhibobo-PushDepth")
	val blockAnimationExhiAngleZ = FloatValue("Slope", 0f, -10f, 15f, "Exhibobo-Slope")
	val blockAnimationExhiYPushPos = IntegerValue("Y-PushPos", 20, 0, 100, "Exhibobo-Y-PushPos")
	val blockAnimationExhiZPushPos = IntegerValue("Z-PushPos", 50, 0, 100, "Exhibobo-Z-PushPos")
	val blockAnimationExhiSmooth = IntegerValue("Smooth", 5, -25, 25, "Exhibobo-Smooth")

	init
	{
		equipProgressSmoothingGroup.addAll(equipProgressSmoothingModeValue, equipProgressSmoothingSpeedModifierValue, equipProgressSmoothingDownSpeedMultiplierValue, equipProgressSmoothingDownSpeedValue, equipProgressSmoothingUpSpeedMultiplierValue, equipProgressSmoothingUpSpeedValue)
		equipProgressSwingProgressAffectGroup.addAll(equipProgressSwingProgressAffectEnabled, equipProgressSwingProgressAffectAffectness)
		equipProgressTranslationAffectGroup.addAll(equipProgressTranslationAffectEnabled, equipProgressTranslationAffectAffectnessX, equipProgressTranslationAffectAffectnessY, equipProgressTranslationAffectAffectnessZ)

		equipProgress.addAll(equipProgressMultiplier, equipProgressSmoothingGroup, equipProgressSwingProgressAffectGroup, equipProgressTranslationAffectGroup)
		swingSpeedBoostGroup.addAll(swingSpeedBoostAmount, swingSpeedBoostFadeTicks)
		swingSpeedGroup.addAll(swingSpeedBoostGroup, swingSpeedEnabled, swingSpeedSwingSpeed, swingSpeedSwingProgressLimit)

		swingTranslationGroup.addAll(swingTranslationX, swingTranslationY, swingTranslationZ)
		swingRelTranslationGroup.addAll(swingRelTranslationX, swingRelTranslationXSmoothing, swingRelTranslationY, swingRelTranslationYSmoothing, swingRelTranslationZ, swingRelTranslationZSmoothing)
		swingSmoothingGroup.addAll(swingSmoothingSq, swingSmoothingSqSin, swingSmoothingSqrt, swingSmoothingSqrtSin)
		swingStaticSwingProgressGroup.addAll(swingStaticSwingProgressEnabled, swingStaticSwingProgressProgress)

		swingGroup.addAll(swingTranslationGroup, swingRelTranslationGroup, swingScale, swingSmoothingGroup, swingStaticSwingProgressGroup)

		blockTranslationGroup.addAll(blockTranslationX, blockTranslationY, blockTranslationZ)
		blockRelTranslationGroup.addAll(blockRelTranslationX, blockRelTranslationXSmoothing, blockRelTranslationY, blockRelTranslationYSmoothing, blockRelTranslationZ, blockRelTranslationZSmoothing)
		blockSmoothingGroup.addAll(blockSmoothingSq, blockSmoothingSqSin, blockSmoothingSqrt, blockSmoothingSqrtSin)
		blockStaticSwingProgressGroup.addAll(blockStaticSwingProgressEnabled, blockStaticSwingProgressProgress)

		blockAnimationSlideGroup.addAll(blockAnimationSlideAngleX, blockAnimationSlideAngleY, blockAnimationSlideAngleZ, blockAnimationSlideXPos, blockAnimationSlideYPos)
		blockAnimationExhiGroup.addAll(blockAnimationExhiAngleX, blockAnimationExhiAngleY, blockAnimationExhiAngleZ, blockAnimationExhiYPushPos, blockAnimationExhiZPushPos, blockAnimationExhiSmooth)

		blockAnimationGroup.addAll(blockAnimationMode, blockAnimationSlideGroup, blockAnimationExhiGroup)

		blockGroup.addAll(blockAngle, blockTranslationGroup, blockScale, blockRelTranslationGroup, blockSmoothingGroup, blockStaticSwingProgressGroup, blockAnimationGroup)
	}

	@JvmField
	var swingSpeedBoost = 0

	override val tag: String
		get() = "${blockAnimationMode.get()}${if (swingStaticSwingProgressEnabled.get()) " static_" + swingStaticSwingProgressProgress.get() else ""}${if (blockStaticSwingProgressEnabled.get()) " blockstatic_" + blockStaticSwingProgressProgress.get() else ""}"
}
