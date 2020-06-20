/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.Value
import net.minecraft.client.gui.ScaledResolution
import kotlin.math.max
import kotlin.math.min

/**
 * CustomHUD element
 */
abstract class Element(var x: Double = 2.0, var y: Double = 2.0, scale: Float = 1F,
                       var side: Side = Side.default()) : MinecraftInstance() {
    val info = javaClass.getAnnotation(ElementInfo::class.java)
            ?: throw IllegalArgumentException("Passed element with missing element info")

    var scale: Float = 1F
        set(value) {
            if (info.disableScale)
                return

            field = value
        }
        get() {
            if (info.disableScale)
                return 1.0f
            return field
        }

    init {
        this.scale = scale
    }

    val name: String
        get() = info.name

    var renderX: Double
        get() = when (side.horizontal) {
            Side.Horizontal.LEFT -> x
            Side.Horizontal.MIDDLE -> (ScaledResolution(mc).scaledWidth / 2) - x
            Side.Horizontal.RIGHT -> ScaledResolution(mc).scaledWidth - x
        }
        set(value) = when (side.horizontal) {
            Side.Horizontal.LEFT -> {
                x += value
            }
            Side.Horizontal.MIDDLE, Side.Horizontal.RIGHT -> {
                x -= value
            }
        }

    var renderY: Double
        get() = when (side.vertical) {
            Side.Vertical.UP -> y
            Side.Vertical.MIDDLE -> (ScaledResolution(mc).scaledHeight / 2) - y
            Side.Vertical.DOWN -> ScaledResolution(mc).scaledHeight - y
        }
        set(value) = when (side.vertical) {
            Side.Vertical.UP -> {
                y += value
            }
            Side.Vertical.MIDDLE, Side.Vertical.DOWN -> {
                y -= value
            }
        }

    var border: Border? = null

    var drag = false
    var prevMouseX = 0F
    var prevMouseY = 0F

    /**
     * Get all values of element
     */
    open val values: List<Value<*>>
        get() = javaClass.declaredFields.map { valueField ->
            valueField.isAccessible = true
            valueField[this]
        }.filterIsInstance<Value<*>>()

    /**
     * Called when element created
     */
    open fun createElement() = true

    /**
     * Called when element destroyed
     */
    open fun destroyElement() {}

    /**
     * Draw element
     */
    abstract fun drawElement(): Border?

    /**
     * Update element
     */
    open fun updateElement() {}

    /**
     * Check if [x] and [y] is in element border
     */
    open fun isInBorder(x: Double, y: Double): Boolean {
        val border = border ?: return false

        val minX = min(border.x, border.x2)
        val minY = min(border.y, border.y2)

        val maxX = max(border.x, border.x2)
        val maxY = max(border.y, border.y2)

        return minX <= x && minY <= y && maxX >= x && maxY >= y
    }

    /**
     * Called when mouse clicked
     */
    open fun handleMouseClick(x: Double, y: Double, mouseButton: Int) {}

    /**
     * Called when key pressed
     */
    open fun handleKey(c: Char, keyCode: Int) {}

}

/**
 * Element info
 */
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class ElementInfo(val name: String, val single: Boolean = false, val force: Boolean = false, val disableScale: Boolean = false, val priority: Int = 0)

/**
 * CustomHUD Side
 *
 * Allows to change default x and y position by side
 */
class Side(var horizontal: Horizontal, var vertical: Vertical) {

    companion object {

        /**
         * Default element side
         */
        fun default() = Side(Horizontal.LEFT, Vertical.UP)

    }

    /**
     * Horizontal side
     */
    enum class Horizontal(val sideName: String) {

        LEFT("Left"),
        MIDDLE("Middle"),
        RIGHT("Right");

        companion object {

            @JvmStatic
            fun getByName(name: String) = values().find { it.sideName == name }

        }

    }

    /**
     * Vertical side
     */
    enum class Vertical(val sideName: String) {

        UP("Up"),
        MIDDLE("Middle"),
        DOWN("Down");

        companion object {

            @JvmStatic
            fun getByName(name: String) = values().find { it.sideName == name }

        }

    }

}

/**
 * Border of element
 */
data class Border(val x: Float, val y: Float, val x2: Float, val y2: Float) {

    fun draw() = RenderUtils.drawBorderedRect(x, y, x2, y2, 3F, Int.MIN_VALUE, 0)

}