/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.LiquidBounce

class EventManager
{

	private val registry = HashMap<Class<out Event>, MutableList<EventHook>>()

	/**
	 * Register [listener]
	 */
	fun registerListener(listener: Listenable)
	{
		listener.javaClass.declaredMethods.asSequence().filter { it.isAnnotationPresent(EventTarget::class.java) }.filter { it.parameterTypes.size == 1 }.forEach { method ->
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
		registry.filter { it.value.removeIf { hook -> hook.eventClass == listenable } }.forEach { registry[it.key] = it.value }
	}

	/**
	 * Call event to listeners
	 *
	 * @param event to call
	 */
	@JvmOverloads
	fun callEvent(event: Event, profile: Boolean = false)
	{
		val profiler = if (profile) LiquidBounce.wrapper.minecraft.mcProfiler else null
		val targets = registry[event.javaClass] ?: return

		targets.filter { it.isIgnoreCondition || it.eventClass.handleEvents() }.forEach {
			val name = it.eventClass.javaClass.simpleName.ifEmpty { "anonymous" }
			profiler?.startSection(name)

			try
			{
				it.method.invoke(it.eventClass, event)
			}
			catch (t: Throwable)
			{
				t.printStackTrace()
			}

			profiler?.endSection()
		}
	}
}
