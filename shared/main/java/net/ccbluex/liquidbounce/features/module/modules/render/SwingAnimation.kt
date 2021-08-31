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
	val dummyRangeSlider = IntegerRangeValue("DummyIntRange", 3, 4, 0, 10)
	val dummyColorSlider = ColorValue("DummyColor", 255, 255, 255)

	private val equipProgressGroup = ValueGroup("EquipProgress")
	val equipProgressAffectMultiplier = FloatValue("EquipProgressMultiplier", 1f, 0f, 2f)

	private val equipProgressSmoothingGroup = ValueGroup("Smoothing")
	val equipProgressSmoothingModeValue = ListValue("Mode", arrayOf("None", "Linear", "Square", "Cube", "Quadratic-Function", "Reverse-Quadratic-Function"), "None")
	val equipProgressSmoothingSpeedModifierValue = IntegerValue("SpeedModifier", 1, 0, 2)
	val equipProgressDownSpeedMultiplierValue = FloatValue("DownMultiplier", 2.4F, 0.2F, 5F)
	val equipProgressDownSpeedValue = IntegerValue("Down", 6, 1, 10)
	val equipProgressUpSpeedMultiplierValue = FloatValue("UpMultiplier", 1.2F, 0.2F, 5F)
	val equipProgressUpSpeedValue = IntegerValue("Up", 3, 1, 10)

	private val equipProgressSwingProgressAffectGroup = ValueGroup("SwingProgressAffect")
	val equipProgressAffectsAnimation = BoolValue("Enabled", false)
	val equipProgressAnimationAffectness = IntegerValue("Affectness", 100, 1, 100)

	private val equipProgressTranslationAffectGroup = ValueGroup("TranslationAffect")
	val equipProgressAffectsAnimationTranslation = BoolValue("Enabled", false)
	val equipProgressAnimationTranslationAffectnessX = FloatValue("AffectnessX", 0f, 0f, 1f)
	val equipProgressAnimationTranslationAffectnessY = FloatValue("AffectnessY", .6f, 0f, 1f)
	val equipProgressAnimationTranslationAffectnessZ = FloatValue("AffectnessZ", 0f, 0f, 1f)

	private val swingSpeedBoostGroup = ValueGroup("SwingSpeedBoostAfterReequip")
	val swingSpeedBoostAfterReequipValue = IntegerValue("Amount", 0, -5, 6)
	val swingSpeedBoostFadeTicksValue = IntegerValue("FadeTicks", 4, 2, 20)

	private val swingSpeedGroup = ValueGroup("SwingSpeed")
	val enableCustomSwingSpeed = BoolValue("Enabled", false)
	val swingSpeed = IntegerValue("SwingSpeed", 0, -4, 20)
	val swingProgressLimit: IntegerValue = object : IntegerValue("SwingProgressLimit", 3, 1, 20)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			if (newValue > 8 + (if (enableCustomSwingSpeed.get()) swingSpeed.get() else 0)) this.set(8 + if (enableCustomSwingSpeed.get()) swingSpeed.get() else 0)
		}
	}

	private val swingGroup = ValueGroup("Swing")

	private val translationGroup = ValueGroup("Translation")
	val xTranslation = FloatValue("X", 0f, -1f, 1f)
	val yTranslation = FloatValue("Y", 0f, -1f, 1f)
	val zTranslation = FloatValue("Z", 0f, -1f, 1f)

	private val relTranslationGroup = ValueGroup("RelativeTranslation")
	val xRTranslation = FloatValue("X", 0f, -1f, 1f)
	val xRTranslationSmoothingMethod = ListValue("X-Smoothing", smoothingMethods, "Sqrt")
	val yRTranslation = FloatValue("Y", 0f, -1f, 1f)
	val yRTranslationSmoothingMethod = ListValue("Y-Smoothing", smoothingMethods, "Sqrt")
	val zRTranslation = FloatValue("Z", 0f, -1f, 1f)
	val zRTranslationSmoothingMethod = ListValue("Z-Smoothing", smoothingMethods, "Sqrt")

	val scale = FloatValue("Scale", .4f, 0.2f, 0.6f)

	private val smoothingGroup = ValueGroup("Smoothing")
	val swingSqSmoothingMethod = ListValue("Sq", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sq")
	val swingSqSmoothingSin = BoolValue("Sq-Back", true)
	val swingSqrtSmoothingMethod = ListValue("Sqrt", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sqrt")
	val swingSqrtSmoothingSin = BoolValue("Sqrt-Back", true)

	val smoothSwing = BoolValue("SmoothSwing", false)

	private val staticSwingProgressGroup = ValueGroup("StaticSwingProgress")
	val staticSwingProgress = BoolValue("Enabled", false)
	val staticSwingProgressValue = FloatValue("SwingProgress", .64f, .11f, .99f)

	private val blockGroup = ValueGroup("Block")

	private val blockTranslationGroup = ValueGroup("Translation")
	val blockXTranslation = FloatValue("X", 0f, -1f, 1f)
	val blockYTranslation = FloatValue("Y", 0f, -1f, 1f)
	val blockZTranslation = FloatValue("Z", 0f, -1f, 1f)

	private val blockRelTranslationGroup = ValueGroup("RelativeTranslation")

	/* TIP: I recommand -0.01 for Slide mode; zero or higher value for EXHIBOBO mode */
	val blockXRTranslation = FloatValue("X", 0.02f, -1f, 1f)
	val blockXRTranslationSmoothingMethod = ListValue("X-Smoothing", smoothingMethods, "Sqrt")

	/* TIP: I recommend 0.13 for Slide mode; zero or lower value for EXHIBOBO mode */
	val blockYRTranslation = FloatValue("Y", 0.13f, -1f, 1f)
	val blockYRTranslationSmoothingMethod = ListValue("Y-Smoothing", smoothingMethods, "Sqrt")

	val blockZRTranslation = FloatValue("Z", 0f, -1f, 1f)
	val blockZRTranslationSmoothingMethod = ListValue("Z-Smoothing", smoothingMethods, "Sqrt")

	val blockScale = FloatValue("Scale", .4f, 0.2f, 0.6f)

	val swordBlockRotationAngle = FloatValue("Angle", 80f, 45f, 135f)

	private val blockSmoothingGroup = ValueGroup("Smoothing")
	val blockSqSwingSmoothingMethod = ListValue("Sq", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sq")
	val blockSqSmoothingSin = BoolValue("Sq-Back", true)
	val blockSqrtSwingSmoothingMethod = ListValue("Sqrt", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sqrt")
	val blockSqrtSmoothingSin = BoolValue("Sqrt-Back", true)

	private val blockStatkcSwingProgressGroup = ValueGroup("StaticSwingProgress")
	val blockStaticSwingProgress = BoolValue("Enabled", false)
	val blockStaticSwingProgressValue = FloatValue("SwingProgress", .64f, .11f, .99f)

	private val animationGroup = ValueGroup("Animation")
	val animationMode = ListValue("Mode", arrayOf("LiquidBounce", "1.8", "1.7", "Push", "Tap", "Tap2", "Avatar", "Sigma", "Slide", "Exhibobo", "Lucid", "Luna", "Hooded", "Bump", "Slap"), "LiquidBounce")

	// Sword Block Animation Options
	private val slideGroup: ValueGroup = object : ValueGroup("Slide")
	{
		override fun showCondition(): Boolean = animationMode.get().equals("Slide", ignoreCase = true)
	}
	val slideAngleX = FloatValue("AngleX", 40f, -30f, 80f)
	val slideAngleY = FloatValue("AngleY", 10f, 0f, 135f)
	val slideAngleZ = FloatValue("AngleZ", 15f, 0f, 135f)
	val slideXPos = IntegerValue("PosX", -10, -100, 100)
	val slideYPos = IntegerValue("PosY", 13, -5, 30)

	private val exhiGroup: ValueGroup = object : ValueGroup("Exhibobo")
	{
		override fun showCondition(): Boolean = animationMode.get().equals("Exhibobo", ignoreCase = true)
	}
	val exhiAngleX = FloatValue("SwingAngle", 15f, 0f, 30f)
	val exhiAngleY = FloatValue("PushDepth", 0f, -10f, 20f)
	val exhiAngleZ = FloatValue("Slope", 0f, -10f, 15f)
	val exhiYPushPos = IntegerValue("Y-PushPos", 20, 0, 100)
	val exhiZPushPos = IntegerValue("Z-PushPos", 50, 0, 100)
	val exhiSmooth = IntegerValue("Smooth", 5, -25, 25)

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
