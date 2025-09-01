package net.ccbluex.liquidbounce.utils.aiming.point.exempts

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.minecraft.util.math.Vec3d

enum class ExemptBoxPart(override val choiceName: String) : NamedChoice, ExemptPoint {

    HEAD("Head"),
    BODY("Body"),
    FEET("Feet");

    override fun predicate(context: ExemptContext, point: Vec3d): Boolean {
        val length = context.box.lengthY / entries.size
        return when (this) {
            HEAD -> point.y <= context.box.maxY && point.y > context.box.maxY - length
            BODY -> point.y <= context.box.maxY - length && point.y >= context.box.minY + length
            FEET -> point.y >= context.box.minY && point.y < context.box.minY + length
        }
    }

    /**
     * Check if this part of the box is higher than the other by the index of the enum.
     * So please DO NOT change the order of the enum.
     */
    fun isHigherThan(other: ExemptBoxPart) = entries.indexOf(this) < entries.indexOf(other)
}

