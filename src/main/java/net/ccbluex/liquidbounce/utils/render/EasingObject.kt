package net.ccbluex.liquidbounce.utils.render

class EasingObject(var lastTime: Long = 0, var lastValue: Float = -1f, var currentValue: Float = -1f) {

    fun update(currentValue: Float): Float {
        if (currentValue != this.currentValue) {
            if (currentValue < this.currentValue) {
                this.lastValue = currentValue
            } else {
                this.lastValue = this.currentValue
            }

            this.currentValue = currentValue

            lastTime = System.currentTimeMillis()
        }

        return AnimationUtils.easeOutElastic(((System.currentTimeMillis() - lastTime) / 500f).coerceIn(0f, 1f)) * (currentValue - lastValue) + lastValue
    }

}