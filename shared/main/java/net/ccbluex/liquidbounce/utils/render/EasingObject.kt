package net.ccbluex.liquidbounce.utils.render

class EasingObject(private var lastTime: Long = 0, var lastValue: Float = -1.0f, private var currentValue: Float = -1.0f)
{
	fun update(currentValue: Float): Float
	{
		if (currentValue != this.currentValue)
		{
			lastValue = if (currentValue < this.currentValue) currentValue else this.currentValue

			this.currentValue = currentValue

			lastTime = System.currentTimeMillis()
		}

		return AnimationUtils.easeOutElastic(((System.currentTimeMillis() - lastTime) * 0.002f).coerceIn(0.0f, 1.0f)) * (currentValue - lastValue) + lastValue
	}
}
