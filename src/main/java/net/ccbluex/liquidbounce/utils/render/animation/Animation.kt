package net.ccbluex.liquidbounce.utils.render.animation

class Animation {
    private var duration: Long = 0
    private var startTime: Long = 0
    private var start = 0.0
    var value = 0.0
    var end = 0.0
    private var type = AnimationType.LINEAR
    var isStarted = false
        private set

    fun start(start: Double, end: Double, duration: Float, type: AnimationType) {
        if (isStarted) {
            return
        }
        if (start != this.start || end != this.end || (duration * 1000).toLong() != this.duration || type != this.type) {
            this.duration = (duration * 1000).toLong()
            this.start = start
            startTime = System.currentTimeMillis()
            value = start
            this.end = end
            this.type = type
            isStarted = true
        }
    }

    fun update() {
        if (!isStarted) return
        val result: Double = when (type) {
            AnimationType.LINEAR -> AnimationUtil.linear(
                startTime,
                duration,
                start,
                end
            )

            AnimationType.EASE_IN_QUAD -> AnimationUtil.easeInQuad(
                startTime,
                duration,
                start,
                end
            )

            AnimationType.EASE_OUT_QUAD -> AnimationUtil.easeOutQuad(
                startTime,
                duration,
                start,
                end
            )

            AnimationType.EASE_IN_OUT_QUAD -> AnimationUtil.easeInOutQuad(
                startTime,
                duration,
                start,
                end
            )

            AnimationType.EASE_IN_ELASTIC -> AnimationUtil.easeInElastic(
                (System.currentTimeMillis() - startTime).toDouble(),
                start,
                end - start,
                duration.toDouble()
            )

            AnimationType.EASE_OUT_ELASTIC -> AnimationUtil.easeOutElastic(
                (System.currentTimeMillis() - startTime).toDouble(),
                start,
                end - start,
                duration.toDouble()
            )

            AnimationType.EASE_IN_OUT_ELASTIC -> AnimationUtil.easeInOutElastic(
                (System.currentTimeMillis() - startTime).toDouble(),
                start,
                end - start,
                duration.toDouble()
            )

            AnimationType.EASE_IN_BACK -> AnimationUtil.easeInBack(
                (System.currentTimeMillis() - startTime).toDouble(),
                start,
                end - start,
                duration.toDouble()
            )

            AnimationType.EASE_OUT_BACK -> AnimationUtil.easeOutBack(
                (System.currentTimeMillis() - startTime).toDouble(),
                start,
                end - start,
                duration.toDouble()
            )

            else -> value // Default to the current value if type is unknown
        }
        value = result
        if (System.currentTimeMillis() - startTime > duration) {
            isStarted = false
            value = end
        }
    }

    fun reset() {
        value = 0.0
        start = 0.0
        end = 0.0
        startTime = System.currentTimeMillis()
        isStarted = false
    }

    fun fstart(start: Double, end: Double, duration: Float, type: AnimationType) {
        isStarted = false
        start(start, end, duration, type)
    }
}
