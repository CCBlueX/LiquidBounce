package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Color4b.Companion.hslToRgb
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.minecraft.block.BlockState
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.BlockPos

abstract class GenericColorMode<in T>(name: String) : Choice(name) {

    abstract fun getColors(param: T): Pair<Color4b, Color4b>
    open fun getColor(param: T): Color4b = getColors(param).first

    open fun getColor(param: T, angle: Int): Color4b {

        return getColor(param)
    }
}

class GenericStaticColorMode(
    override val parent: ChoiceConfigurable<*>,
    defaultColor: Color4b
) : GenericColorMode<Any?>("Static") {

    private val staticColor by color("Color", defaultColor)

    override fun getColors(param: Any?) = staticColor to staticColor
}

class GenericSyncColorMode(
    override val parent: ChoiceConfigurable<*>,
    defaultStartAlpha: Int = 255,
    defaultEndAlpha: Int = 255
) : GenericColorMode<Any?>("Sync") {

    private val startAlpha by int("StartAlpha", defaultStartAlpha, 0..255)
    private val endAlpha by int("EndAlpha", defaultEndAlpha, 0..255)

    override fun getColors(param: Any?): Pair<Color4b, Color4b> {
        val themeColor = ModuleHud.getThemeColor()
        val start = themeColor.first.with(a = startAlpha)
        val end = themeColor.second.with(a = endAlpha)
        return  end to start
    }

    override fun getColor(param: Any?, angle: Int): Color4b {
        return getColors(param).first
    }
}



class GenericRainbowColorMode(
    override val parent: ChoiceConfigurable<*>,
    defaultAlpha: Int = 50,
    defaultSaturation: Float = 0.95f,
    defaultLightness: Float = 0.65f
) : GenericColorMode<Any?>("Rainbow") {

    private val alpha by int("Alpha", defaultAlpha, 0..255)
    private val saturation by float("Saturation", defaultSaturation, 0f..1f)
    private val lightness by float("Lightness", defaultLightness, 0f..1f)

    override fun getColors(param: Any?): Pair<Color4b, Color4b> {
        val hue1 = ((System.currentTimeMillis() % 4000) / 4000f)
        val hue2 = (hue1 + 0.25f) % 1f
        return hslToRgb(hue1, saturation, lightness, alpha) to
            hslToRgb(hue2, saturation, lightness, alpha)
    }

    override fun getColor(param: Any?, angle: Int): Color4b {
        val hue = ((System.currentTimeMillis() + angle) % 4000L) / 4000f
        return hslToRgb(hue, saturation, lightness, alpha)
    }
}


class GenericCustomColorMode(
    override val parent: ChoiceConfigurable<*>,
    startColor: Color4b,
    endColor: Color4b
) : GenericColorMode<Any?>("Custom") {

    private val customStartColor by color("Start", startColor)
    private val customEndColor by color("End", endColor)

    override fun getColors(param: Any?) = customStartColor to customEndColor


    override fun getColor(param: Any?, angle: Int): Color4b {
        val progress = angle / 360f
        return Color4b(
            interpolate(customStartColor.r, customEndColor.r, progress),
            interpolate(customStartColor.g, customEndColor.g, progress),
            interpolate(customStartColor.b, customEndColor.b, progress),
            interpolate(customStartColor.a, customEndColor.a, progress)
        )
    }

    private fun interpolate(start: Int, end: Int, progress: Float): Int {
        return (start + (end - start) * progress).toInt().coerceIn(0..255)
    }
}

class MapColorMode(
    override val parent: ChoiceConfigurable<*>,
    private val alpha: Int = 100
) : GenericColorMode<Pair<BlockPos, BlockState>>("MapColor") {

    override fun getColors(param: Pair<BlockPos, BlockState>): Pair<Color4b, Color4b> {
        val (pos, state) = param
        val color = Color4b(state.getMapColor(world, pos).color).with(a = alpha)
        return color to color
    }

    // Override to provide single color directly
    override fun getColor(param: Pair<BlockPos, BlockState>): Color4b {
        val (pos, state) = param
        return Color4b(state.getMapColor(world, pos).color).with(a = alpha)
    }
}

class GenericEntityHealthColorMode(
    override val parent: ChoiceConfigurable<*>
) : GenericColorMode<LivingEntity>("Health") {

    override fun getColors(param: LivingEntity): Pair<Color4b, Color4b> {
        val color = calculateHealthColor(param)
        return color to color
    }

    // Override to provide single color directly
    override fun getColor(param: LivingEntity): Color4b {
        return calculateHealthColor(param)
    }

    private fun calculateHealthColor(entity: LivingEntity): Color4b {
        val maxHealth = entity.maxHealth
        val health = entity.getActualHealth().coerceAtMost(maxHealth)
        val healthPercentage = health / maxHealth

        val red = (255 * (1 - healthPercentage)).toInt().coerceIn(0..255)
        val green = (255 * healthPercentage).toInt().coerceIn(0..255)

        return Color4b(red, green, 0)
    }
}

