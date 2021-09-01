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
	private val equipProgressGroup = ValueGroup("EquipProgress")
	val equipProgressAffectMultiplier = FloatValue("Multiplier", 1f, 0f, 2f, "EquipProgressMultiplier")

	private val equipProgressSmoothingGroup = ValueGroup("Smoothing")
	val equipProgressSmoothingModeValue = ListValue("Mode", arrayOf("None", "Linear", "Square", "Cube", "Quadratic-Function", "Reverse-Quadratic-Function"), "None", "EquipProgressSmoothing")
	val equipProgressSmoothingSpeedModifierValue = IntegerValue("SpeedModifier", 1, 0, 2, "EquipProgressSmoothingSpeedModifier")
	val equipProgressDownSpeedMultiplierValue = FloatValue("DownMultiplier", 2.4F, 0.2F, 5F, "EquipProgressSmoothDownSpeedMultiplier")
	val equipProgressDownSpeedValue = IntegerValue("Down", 6, 1, 10, "EquipProgressSmoothDownSpeed")
	val equipProgressUpSpeedMultiplierValue = FloatValue("UpMultiplier", 1.2F, 0.2F, 5F, "EquipProgressSmoothUpSpeedMultiplier")
	val equipProgressUpSpeedValue = IntegerValue("Up", 3, 1, 10, "EquipProgressSmoothUpSpeed")

	private val equipProgressSwingProgressAffectGroup = ValueGroup("SwingProgressAffect")
	val equipProgressAffectsAnimation = BoolValue("Enabled", false, "EquipProgressAffectsBlockSwingAnimationIntensity")
	val equipProgressAnimationAffectness = IntegerValue("Affectness", 100, 1, 100, "EquipProgressBlockSwingAnimationAffect")

	private val equipProgressTranslationAffectGroup = ValueGroup("TranslationAffect")
	val equipProgressAffectsAnimationTranslation = BoolValue("Enabled", false, "EquipProgressAffectsBlockSwingAnimationTranslation")
	val equipProgressAnimationTranslationAffectnessX = FloatValue("AffectnessX", 0f, 0f, 1f, "EquipProgressAnimationTranslationAffectnessX")
	val equipProgressAnimationTranslationAffectnessY = FloatValue("AffectnessY", .6f, 0f, 1f, "EquipProgressAnimationTranslationAffectnessY")
	val equipProgressAnimationTranslationAffectnessZ = FloatValue("AffectnessZ", 0f, 0f, 1f, "EquipProgressAnimationTranslationAffectnessZ")

	private val swingSpeedBoostGroup = ValueGroup("SwingSpeedBoostAfterReequip")
	val swingSpeedBoostAfterReequipValue = IntegerValue("Amount", 0, -5, 6, "SwingSpeedBoostAfterReequip")
	val swingSpeedBoostFadeTicksValue = IntegerValue("FadeTicks", 4, 2, 20, "SwingSpeedBoostAfterReequip")

	private val swingSpeedGroup = ValueGroup("SwingSpeed")
	val enableCustomSwingSpeed = BoolValue("Enabled", false, "CustomSwingSpeed")
	val swingSpeed = IntegerValue("SwingSpeed", 0, -4, 20, "SwingSpeed")
	val swingProgressLimit: IntegerValue = object : IntegerValue("SwingProgressLimit", 3, 1, 20, "SwingProgressLimit")
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			if (newValue > 8 + (if (enableCustomSwingSpeed.get()) swingSpeed.get() else 0)) this.set(8 + if (enableCustomSwingSpeed.get()) swingSpeed.get() else 0)
		}
	}

	private val swingGroup = ValueGroup("Swing")

	private val translationGroup = ValueGroup("Translation")
	val xTranslation = FloatValue("X", 0f, -1f, 1f, "X-Translation")
	val yTranslation = FloatValue("Y", 0f, -1f, 1f, "Y-Translation")
	val zTranslation = FloatValue("Z", 0f, -1f, 1f, "Z-Translation")

	private val relTranslationGroup = ValueGroup("RelativeTranslation")
	val xRTranslation = FloatValue("X", 0f, -1f, 1f, "X-RelTranslation")
	val xRTranslationSmoothingMethod = ListValue("X-Smoothing", smoothingMethods, "Sqrt", "X-RelTranslationSmoothingMethod")
	val yRTranslation = FloatValue("Y", 0f, -1f, 1f, "Y-RelTranslation")
	val yRTranslationSmoothingMethod = ListValue("Y-Smoothing", smoothingMethods, "Sqrt", "Y-RelTranslationSmoothingMethod")
	val zRTranslation = FloatValue("Z", 0f, -1f, 1f, "Z-RelTranslation")
	val zRTranslationSmoothingMethod = ListValue("Z-Smoothing", smoothingMethods, "Sqrt", "Z-RelTranslationSmoothingMethod")

	val scale = FloatValue("Scale", .4f, 0.2f, 0.6f, "Scale")

	private val smoothingGroup = ValueGroup("Smoothing")
	val swingSqSmoothingMethod = ListValue("Sq", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sq", "SwingAnimationSqSmoothingMethod")
	val swingSqSmoothingSin = BoolValue("Sq-Back", true, "SwingAnimationSqSmoothingBack")
	val swingSqrtSmoothingMethod = ListValue("Sqrt", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sqrt", "SwingAnimationSqrtSmoothingMethod")
	val swingSqrtSmoothingSin = BoolValue("Sqrt-Back", true, "SwingAnimationSqrtSmoothingBack")

	val smoothSwing = BoolValue("SmoothSwing", false)

	private val staticSwingProgressGroup = ValueGroup("StaticSwingProgress")
	val staticSwingProgress = BoolValue("Enabled", false, "StaticSwingProgress")
	val staticSwingProgressValue = FloatValue("SwingProgress", .64f, .11f, .99f, "StaticSwingProgress")

	private val blockGroup = ValueGroup("Block")

	private val blockTranslationGroup = ValueGroup("Translation")
	val blockXTranslation = FloatValue("X", 0f, -1f, 1f, "Block-X-Translation")
	val blockYTranslation = FloatValue("Y", 0f, -1f, 1f, "Block-Y-Translation")
	val blockZTranslation = FloatValue("Z", 0f, -1f, 1f, "Block-Z-Translation")

	private val blockRelTranslationGroup = ValueGroup("RelativeTranslation")

	/* TIP: I recommand -0.01 for Slide mode; zero or higher value for EXHIBOBO mode */
	val blockXRTranslation = FloatValue("X", 0.02f, -1f, 1f, "Block-X-RelTranslation")
	val blockXRTranslationSmoothingMethod = ListValue("X-Smoothing", smoothingMethods, "Sqrt", "Block-X-RelTranslationSmoothingMethod")

	/* TIP: I recommend 0.13 for Slide mode; zero or lower value for EXHIBOBO mode */
	val blockYRTranslation = FloatValue("Y", 0.13f, -1f, 1f, "Block-Y-RelTranslation")
	val blockYRTranslationSmoothingMethod = ListValue("Y-Smoothing", smoothingMethods, "Sqrt", "Block-Y-RelTranslationSmoothingMethod")

	val blockZRTranslation = FloatValue("Z", 0f, -1f, 1f, "Block-Z-RelTranslation")
	val blockZRTranslationSmoothingMethod = ListValue("Z-Smoothing", smoothingMethods, "Sqrt", "Block-Z-RelTranslationSmoothingMethod")

	val blockScale = FloatValue("Scale", .4f, 0.2f, 0.6f, "Block-Scale")

	val swordBlockRotationAngle = FloatValue("Angle", 80f, 45f, 135f, "BlockAngle")

	private val blockSmoothingGroup = ValueGroup("Smoothing")
	val blockSqSwingSmoothingMethod = ListValue("Sq", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sq", "BlockAnimationSqSmoothingMethod")
	val blockSqSmoothingSin = BoolValue("Sq-Back", true, "BlockAnimationSqSmoothingBack")
	val blockSqrtSwingSmoothingMethod = ListValue("Sqrt", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sqrt", "BlockAnimationSqrtSmoothingMethod")
	val blockSqrtSmoothingSin = BoolValue("Sqrt-Back", true, "BlockAnimationSqrtSmoothingBack")

	private val blockStatkcSwingProgressGroup = ValueGroup("StaticSwingProgress")
	val blockStaticSwingProgress = BoolValue("Enabled", false, "Block-StaticSwingProgress")
	val blockStaticSwingProgressValue = FloatValue("SwingProgress", .64f, .11f, .99f, "Block-StaticSwingProgress")

	private val animationGroup = ValueGroup("Animation")
	val animationMode = ListValue("Mode", arrayOf("LiquidBounce", "1.8", "1.7", "Push", "Tap", "Tap2", "Avatar", "Sigma", "Slide", "Exhibobo", "Lucid", "Luna", "Hooded", "Bump", "Slap"), "LiquidBounce", "BlockSwingAnimation")

	// Sword Block Animation Options
	private val slideGroup: ValueGroup = object : ValueGroup("Slide")
	{
		override fun showCondition(): Boolean = animationMode.get().equals("Slide", ignoreCase = true)
	}
	val slideAngleX = FloatValue("AngleX", 40f, -30f, 80f, "Slide-AngleX")
	val slideAngleY = FloatValue("AngleY", 10f, 0f, 135f, "Slide-AngleY")
	val slideAngleZ = FloatValue("AngleZ", 15f, 0f, 135f, "Slide-AngleZ")
	val slideXPos = IntegerValue("PosX", -10, -100, 100, "Slide-X-Pos")
	val slideYPos = IntegerValue("PosY", 13, -5, 30, "Slide-Y-Pos")

	private val exhiGroup: ValueGroup = object : ValueGroup("Exhibobo")
	{
		override fun showCondition(): Boolean = animationMode.get().equals("Exhibobo", ignoreCase = true)
	}
	val exhiAngleX = FloatValue("SwingAngle", 15f, 0f, 30f, "Exhibobo-SwingAngle")
	val exhiAngleY = FloatValue("PushDepth", 0f, -10f, 20f, "Exhibobo-PushDepth")
	val exhiAngleZ = FloatValue("Slope", 0f, -10f, 15f, "Exhibobo-Slope")
	val exhiYPushPos = IntegerValue("Y-PushPos", 20, 0, 100, "Exhibobo-Y-PushPos")
	val exhiZPushPos = IntegerValue("Z-PushPos", 50, 0, 100, "Exhibobo-Z-PushPos")
	val exhiSmooth = IntegerValue("Smooth", 5, -25, 25, "Exhibobo-Smooth")

	init
	{
		equipProgressSmoothingGroup.addAll(equipProgressSmoothingModeValue, equipProgressSmoothingSpeedModifierValue, equipProgressDownSpeedMultiplierValue, equipProgressDownSpeedValue, equipProgressUpSpeedMultiplierValue, equipProgressUpSpeedValue)
		equipProgressSwingProgressAffectGroup.addAll(equipProgressAffectsAnimation, equipProgressAnimationAffectness)
		equipProgressTranslationAffectGroup.addAll(equipProgressAffectsAnimationTranslation, equipProgressAnimationTranslationAffectnessX, equipProgressAnimationTranslationAffectnessY, equipProgressAnimationTranslationAffectnessZ)

		equipProgressGroup.addAll(equipProgressAffectMultiplier, equipProgressSmoothingGroup, equipProgressSwingProgressAffectGroup, equipProgressTranslationAffectGroup)
		swingSpeedBoostGroup.addAll(swingSpeedBoostAfterReequipValue, swingSpeedBoostFadeTicksValue)
		swingSpeedGroup.addAll(swingSpeedBoostGroup, enableCustomSwingSpeed, swingSpeed, swingProgressLimit)

		translationGroup.addAll(xTranslation, yTranslation, zTranslation)
		relTranslationGroup.addAll(xRTranslation, xRTranslationSmoothingMethod, yRTranslation, yRTranslationSmoothingMethod, zRTranslation, zRTranslationSmoothingMethod)
		smoothingGroup.addAll(swingSqSmoothingMethod, swingSqSmoothingSin, swingSqrtSmoothingMethod, swingSqrtSmoothingSin)
		staticSwingProgressGroup.addAll(staticSwingProgress, staticSwingProgressValue)

		swingGroup.addAll(translationGroup, relTranslationGroup, scale, smoothingGroup, staticSwingProgressGroup)

		blockTranslationGroup.addAll(blockXTranslation, blockYTranslation, blockZTranslation)
		blockRelTranslationGroup.addAll(blockXRTranslation, blockXRTranslationSmoothingMethod, blockYRTranslation, blockYRTranslationSmoothingMethod, blockZRTranslation, blockZRTranslationSmoothingMethod)
		blockSmoothingGroup.addAll(blockSqSwingSmoothingMethod, blockSqSmoothingSin, blockSqrtSwingSmoothingMethod, blockSqrtSmoothingSin)
		blockStatkcSwingProgressGroup.addAll(blockStaticSwingProgress, blockStaticSwingProgressValue)

		slideGroup.addAll(slideAngleX, slideAngleY, slideAngleZ, slideXPos, slideYPos)
		exhiGroup.addAll(exhiAngleX, exhiAngleY, exhiAngleZ, exhiYPushPos, exhiZPushPos, exhiSmooth)

		animationGroup.addAll(animationMode, slideGroup, exhiGroup)

		blockGroup.addAll(swordBlockRotationAngle, blockTranslationGroup, blockRelTranslationGroup, blockSmoothingGroup, blockStatkcSwingProgressGroup, blockScale, animationGroup)
	}

	@JvmField
	var swingSpeedBoost = 0

	override val tag: String
		get() = "${animationMode.get()}${if (staticSwingProgress.get()) " static_" + staticSwingProgressValue.get() else ""}${if (blockStaticSwingProgress.get()) " blockstatic_" + blockStaticSwingProgressValue.get() else ""}"
}
