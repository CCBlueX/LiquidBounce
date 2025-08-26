package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

/**
 * Aspect ratio changer
 *
 * @author sqlerrorthing
 */
@Suppress("MagicNumber")
object ModuleAspect : ClientModule("Aspect", Category.RENDER) {
    private val ratioPercentage by int("Ratio", 100, 1..300, suffix = "%")

    @JvmStatic
    val ratioMultiplier: Float get() = ratioPercentage.toFloat() / 100f
}
