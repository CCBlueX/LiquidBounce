/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event

import java.util.*

class EventManager
{

	private val registry = HashMap<Class<out Event>, MutableList<EventHook>>()

	/**
	 * Register [listener]
	 */
	fun registerListener(listener: Listenable)
	{
		listener.javaClass.declaredMethods.asSequence().filter { it.isAnnotationPresent(EventTarget::class.java) && it.parameterTypes.size == 1 }.forEach { method ->
			if (!method.isAccessible) method.isAccessible = true

			@Suppress("UNCHECKED_CAST")
			val eventClass = method.parameterTypes[0] as? Class<out Event> ?: return@forEach
			val eventTarget = method.getAnnotation(EventTarget::class.java)

			val invokableEventTargets = registry.getOrDefault(eventClass, ArrayList())

			invokableEventTargets.add(EventHook(listener, method, eventTarget))
			registry[eventClass] = invokableEventTargets
		}
	}

	/**
	 * Unregister listener
	 *
	 * @param listenable for unregister
	 */
	fun unregisterListener(listenable: Listenable)
	{
		registry.filter { (_, targets) -> targets.removeIf { it.eventClass == listenable } }.forEach { (key, targets) -> registry[key] = targets }
	}

	/**
	 * Call event to listeners
	 *
	 * @param event to call
	 */
	fun callEvent(event: Event)
	{
		val targets = registry[event.javaClass] ?: return

		targets.asSequence().filterNot { !it.eventClass.handleEvents() && !it.isIgnoreCondition }.forEach {
			try
			{
				it.method.invoke(it.eventClass, event)
			}
			catch (throwable: Throwable)
			{
				throwable.printStackTrace()
			}
		}
	}
}
