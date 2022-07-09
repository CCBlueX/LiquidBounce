package net.ccbluex.liquidbounce.utils.timer

class Cooldown private constructor(private val millis: Int)
{
	private var lastUse: Long = 0

	fun attemptReset(): Boolean
	{
		return if (System.currentTimeMillis() - lastUse > millis.toLong())
		{
			lastUse = System.currentTimeMillis()
			true
		}
		else false
	}

	companion object
	{
		fun createCooldownInMillis(milis: Int): Cooldown = Cooldown(milis)
	}
}
