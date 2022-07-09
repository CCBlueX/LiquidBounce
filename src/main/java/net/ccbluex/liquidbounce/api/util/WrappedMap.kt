/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

import java.util.*

open class WrappedMap<WRAPPED_KEY, WRAPPED_VALUE, UNWRAPPED_KEY, UNWRAPPED_VALUE, out MAP : Map<WRAPPED_KEY, WRAPPED_VALUE>>(private val wrappedMap: MAP, val keyWrapper: (UNWRAPPED_KEY) -> WRAPPED_KEY, val keyUnwrapper: (WRAPPED_KEY) -> UNWRAPPED_KEY, val valueWrapper: (UNWRAPPED_VALUE) -> WRAPPED_VALUE, val valueUnwrapper: (WRAPPED_VALUE) -> UNWRAPPED_VALUE) : Map<UNWRAPPED_KEY, UNWRAPPED_VALUE>
{
	override val entries: Set<Map.Entry<UNWRAPPED_KEY, UNWRAPPED_VALUE>>
		get() = wrappedMap.entries.map { AbstractMap.SimpleImmutableEntry(keyUnwrapper(it.key), valueUnwrapper(it.value)) }.toSet()
	override val size: Int
		get() = wrappedMap.size
	override val keys: Set<UNWRAPPED_KEY>
		get() = wrappedMap.keys.map(keyUnwrapper).toSet()
	override val values: Collection<UNWRAPPED_VALUE>
		get() = wrappedMap.values.map(valueUnwrapper)

	override fun containsKey(key: UNWRAPPED_KEY): Boolean = wrappedMap.containsKey(keyWrapper(key))

	override fun containsValue(value: UNWRAPPED_VALUE): Boolean = wrappedMap.containsValue(valueWrapper(value))

	override fun get(key: UNWRAPPED_KEY): UNWRAPPED_VALUE?
	{
		return valueUnwrapper(wrappedMap[keyWrapper(key)] ?: return null)
	}

	override fun isEmpty(): Boolean = size == 0
}
