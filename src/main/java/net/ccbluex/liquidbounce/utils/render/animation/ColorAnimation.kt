package net.ccbluex.liquidbounce.utils.render.animation

import java.awt.Color

class ColorAnimation {
    var r = Animation()
    var g = Animation()
    var b = Animation()
    var a = Animation()
    var first = true
    var end: Color? = null

    fun start(start: Color, end: Color, duration: Float, type: AnimationType) {
        this.end = end
        r.start(start.red.toDouble(), end.red.toDouble(), duration, type)
        g.start(start.green.toDouble(), end.green.toDouble(), duration, type)
        b.start(start.blue.toDouble(), end.blue.toDouble(), duration, type)
        a.start(start.alpha.toDouble(), end.alpha.toDouble(), duration, type)
    }

    fun update() {
        end ?: return
        if (first) {
            color = end as Color
            first = false
            return
        }
        r.update()
        g.update()
        b.update()
        a.update()
    }

    fun reset() {
        r.reset()
        g.reset()
        b.reset()
        a.reset()
    }

    var color: Color
        get() = Color(
            r.value.coerceIn(0.0, 255.0).toInt(),
            g.value.coerceIn(0.0, 255.0).toInt(),
            b.value.coerceIn(0.0, 255.0).toInt(),
            a.value.coerceIn(0.0, 255.0).toInt()
        )
        set(color) {
            r.value = color.red.toDouble()
            g.value = color.green.toDouble()
            b.value = color.blue.toDouble()
            a.value = color.alpha.toDouble()
        }

    fun fstart(color: Color, color1: Color, duration: Float, type: AnimationType) {
        end = color1
        r.fstart(color.red.toDouble(), color1.red.toDouble(), duration, type)
        g.fstart(color.green.toDouble(), color1.green.toDouble(), duration, type)
        b.fstart(color.blue.toDouble(), color1.blue.toDouble(), duration, type)
        a.fstart(color.alpha.toDouble(), color1.alpha.toDouble(), duration, type)
    }

    fun base(color: Color) {
        r.value = AnimationUtil.base(r.value, color.red.toDouble(), 0.1)
        g.value = AnimationUtil.base(g.value, color.green.toDouble(), 0.1)
        b.value = AnimationUtil.base(b.value, color.blue.toDouble(), 0.1)
        a.value = AnimationUtil.base(a.value, color.alpha.toDouble(), 0.1)
    }
}
