package net.ccbluex.liquidbounce.utils.timer

class Cooldown private constructor(private val COOLDOWN_IN_MS: Int)
{
	private var lastUse: Long = 0

	fun attemptReset(): Boolean
	{
		return if (System.currentTimeMillis() - lastUse > COOLDOWN_IN_MS.toLong())
		{
			lastUse = System.currentTimeMillis()
			true
		}
		else false
	}

	companion object
	{
		fun getNewCooldownMiliseconds(milis: Int): Cooldown = Cooldown(milis)
	}
}
