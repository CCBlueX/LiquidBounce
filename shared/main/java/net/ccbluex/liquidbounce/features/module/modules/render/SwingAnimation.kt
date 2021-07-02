/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

// TODO: Astolfo block animation
@ModuleInfo(name = "SwingAnimation", description = "Customize swing animation.", category = ModuleCategory.RENDER)
class SwingAnimation : Module()
{
	val equipProgressSmoothingModeValue = ListValue("EquipProgressSmoothing", arrayOf("None", "Linear", "Square", "Cube", "Quadratic-Function", "Reverse-Quadratic-Function"), "None")
	val equipProgressSmoothingSpeedModifierValue = IntegerValue("EquipProgressSmoothingSpeedModifier", 1, 0, 2)
	val equipProgressDownSpeedMultiplierValue = FloatValue("EquipProgressSmoothDownSpeedMultiplier", 2.4F, 0.2F, 5F)
	val equipProgressUpSpeedMultiplierValue = FloatValue("EquipProgressSmoothUpSpeedMultiplier", 1.2F, 0.2F, 5F)
	val equipProgressDownSpeedValue = IntegerValue("EquipProgressSmoothDownSpeed", 6, 1, 10)
	val equipProgressUpSpeedValue = IntegerValue("EquipProgressSmoothUpSpeed", 3, 1, 10)

	// EquipProgress-Based Animation Options
	val equipProgressAffectMultiplier = FloatValue("EquipProgressMultiplier", 1f, 0f, 2f)
	val equipProgressAffectsAnimation = BoolValue("EquipProgressAffectsBlockSwingAnimationIntensity", false)
	val equipProgressAnimationAffectness = IntegerValue("EquipProgressBlockSwingAnimationAffect", 100, 1, 100)

	val swingSpeedBoostAfterReequipValue = IntegerValue("SwingSpeedBoostAfterReequip", 0, -5, 6)
	val swingSpeedBoostFadeTicksValue = IntegerValue("SwingSpeedBoostAfterReequip-FadeTicks", 4, 2, 20)

	val equipProgressAffectsAnimationTranslation = BoolValue("EquipProgressAffectsBlockSwingAnimationTranslation", false)
	val equipProgressAnimationTranslationAffectnessX = FloatValue("EquipProgressAnimationTranslationAffectnessX", 0f, 0f, 1f)
	val equipProgressAnimationTranslationAffectnessY = FloatValue("EquipProgressAnimationTranslationAffectnessY", .6f, 0f, 1f)
	val equipProgressAnimationTranslationAffectnessZ = FloatValue("EquipProgressAnimationTranslationAffectnessZ", 0f, 0f, 1f)

	// Translation, Scaling Options
	val xTranslation = FloatValue("X-Translation", 0f, -1f, 1f)
	val yTranslation = FloatValue("Y-Translation", 0f, -1f, 1f)
	val zTranslation = FloatValue("Z-Translation", 0f, -1f, 1f)

	val xRTranslation = FloatValue("X-RelTranslation", 0f, -1f, 1f)
	val xRTranslationSmoothingMethod = ListValue("X-RelTranslationSmoothingMethod", arrayOf("None", "Reverse", "Sqrt", "SqrtSqrt", "Sq", "SqSq", "Linear"), "Sqrt")

	val yRTranslation = FloatValue("Y-RelTranslation", 0f, -1f, 1f)
	val yRTranslationSmoothingMethod = ListValue("Y-RelTranslationSmoothingMethod", arrayOf("None", "Reverse", "Sqrt", "SqrtSqrt", "Sq", "SqSq", "Linear"), "Sqrt")

	val zRTranslation = FloatValue("Z-RelTranslation", 0f, -1f, 1f)
	val zRTranslationSmoothingMethod = ListValue("Z-RelTranslationSmoothingMethod", arrayOf("None", "Reverse", "Sqrt", "SqrtSqrt", "Sq", "SqSq", "Linear"), "Sqrt")

	val scale = FloatValue("Scale", .4f, 0.2f, 0.6f)

	val blockXTranslation = FloatValue("Block-X-Translation", 0f, -1f, 1f)
	val blockYTranslation = FloatValue("Block-Y-Translation", 0f, -1f, 1f)
	val blockZTranslation = FloatValue("Block-Z-Translation", 0f, -1f, 1f)

	/* TIP: I recommand -0.01 for Slide mode; zero or higher value for EXHIBOBO mode */
	val blockXRTranslation = FloatValue("Block-X-RelTranslation", 0.02f, -1f, 1f)
	val blockXRTranslationSmoothingMethod = ListValue("Block-X-RelTranslationSmoothingMethod", arrayOf("None", "Reverse", "Sqrt", "SqrtSqrt", "Sq", "SqSq", "Linear"), "Sqrt")

	/* TIP: I recommend 0.13 for Slide mode; zero or lower value for EXHIBOBO mode */
	val blockYRTranslation = FloatValue("Block-Y-RelTranslation", 0.13f, -1f, 1f)
	val blockYRTranslationSmoothingMethod = ListValue("Block-Y-RelTranslationSmoothingMethod", arrayOf("None", "Reverse", "Sqrt", "SqrtSqrt", "Sq", "SqSq", "Linear"), "Sqrt")

	val blockZRTranslation = FloatValue("Block-Z-RelTranslation", 0f, -1f, 1f)
	val blockZRTranslationSmoothingMethod = ListValue("Block-Z-RelTranslationSmoothingMethod", arrayOf("None", "Reverse", "Sqrt", "SqrtSqrt", "Sq", "SqSq", "Linear"), "Sqrt")

	val blockScale = FloatValue("Block-Scale", .4f, 0.2f, 0.6f)

	val customSwingSpeed = BoolValue("CustomSwingSpeed", false)
	val swingSpeed = IntegerValue("SwingSpeed", 0, -4, 20)

	val swingProgressLimit: IntegerValue = object : IntegerValue("SwingProgressLimit", 3, 1, 20)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			if (newValue > 8 + (if (customSwingSpeed.get()) swingSpeed.get() else 0)) this.set(8 + if (customSwingSpeed.get()) swingSpeed.get() else 0)
		}
	}

	val swordBlockRotationAngle = FloatValue("BlockAngle", 80f, 45f, 135f)

	// Smoothing
	val swingSqSmoothingMethod = ListValue("SwingAnimationSqSmoothingMethod", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sq")
	val swingSqSmoothingSin = BoolValue("SwingAnimationSqSmoothingBack", true)
	val swingSqrtSmoothingMethod = ListValue("SwingAnimationSqrtSmoothingMethod", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sqrt")
	val swingSqrtSmoothingSin = BoolValue("SwingAnimationSqrtSmoothingBack", true)

	val blockSqSwingSmoothingMethod = ListValue("BlockAnimationSqSmoothingMethod", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sq")
	val blockSqSmoothingSin = BoolValue("BlockAnimationSqSmoothingBack", true)
	val blockSqrtSwingSmoothingMethod = ListValue("BlockAnimationSqrtSmoothingMethod", arrayOf("None", "Sqrt", "SqrtSqrt", "Sq", "SqSq"), "Sqrt")
	val blockSqrtSmoothingSin = BoolValue("BlockAnimationSqrtSmoothingBack", true)

	// Sword Block Animation
	val animationMode = ListValue("BlockSwingAnimation", arrayOf("LiquidBounce", "1.8", "1.7", "Push", "Tap", "Tap2", "Avatar", "Sigma", "Slide", "Exhibobo", "Lucid", "Luna", "Hooded", "Bump", "Slap"), "LiquidBounce")

	// Sword Block Animation Options
	val slideAngleX = FloatValue("Slide-AngleX", 40f, -30f, 80f)
	val slideAngleY = FloatValue("Slide-AngleY", 10f, 0f, 135f)
	val slideAngleZ = FloatValue("Slide-AngleZ", 15f, 0f, 135f)
	val slideXPos = IntegerValue("Slide-X-Pos", -10, -100, 100)
	val slideYPos = IntegerValue("Slide-Y-Pos", 13, -5, 30)

	val exhiAngleX = FloatValue("Exhibobo-SwingAngle", 15f, 0f, 30f)
	val exhiAngleY = FloatValue("Exhibobo-PushDepth", 0f, -10f, 20f)
	val exhiAngleZ = FloatValue("Exhibobo-Slope", 0f, -10f, 15f)

	val exhiYPushPos = IntegerValue("Exhibobo-Y-PushPos", 20, 0, 100)
	val exhiZPushPos = IntegerValue("Exhibobo-Z-PushPos", 50, 0, 100)

	val exhiSmooth = IntegerValue("Exhibobo-Smooth", 5, -25, 25)

	val smoothSwing = BoolValue("SmoothSwing", false)

	val staticSwingProgress = BoolValue("StaticSwingProgress", false)
	val staticSwingProgressValue = FloatValue("StaticSwingProgress", .64f, .11f, .99f)

	val blockStaticSwingProgress = BoolValue("Block-StaticSwingProgress", false)
	val blockStaticSwingProgressValue = FloatValue("Block-StaticSwingProgress", .64f, .11f, .99f)

	@JvmField
	var swingSpeedBoost = 0

	override val tag: String
		get() = "${animationMode.get()}${if (staticSwingProgress.get()) " static_" + staticSwingProgressValue.get() else ""}${if (blockStaticSwingProgress.get()) " blockstatic_" + blockStaticSwingProgressValue.get() else ""}"
}
