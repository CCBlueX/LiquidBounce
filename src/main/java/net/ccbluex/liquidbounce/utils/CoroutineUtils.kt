package net.ccbluex.liquidbounce.utils

@Suppress("ControlFlowWithEmptyBody")
object CoroutineUtils {
	fun waitUntil(condition: () -> Boolean) {
		while (!condition()) {}
	}
}