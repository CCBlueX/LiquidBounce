/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

import java.util.*

open class WrappedMap<OK, OV, WK, WV, out M : Map<OK, OV>>(private val wrappedMap: M, val keyWrapper: (OK) -> WK, val keyUnwrapper: (WK) -> OK, val valueWrapper: (OV) -> WV, val valueUnwrapper: (WV) -> OV) : Map<WK, WV>
{
	override val entries: Set<Map.Entry<WK, WV>>
		get() = wrappedMap.entries.map { AbstractMap.SimpleImmutableEntry(keyWrapper(it.key), valueWrapper(it.value)) }.toSet()
	override val size: Int
		get() = wrappedMap.size
	override val keys: Set<WK>
		get() = wrappedMap.keys.map(keyWrapper).toSet()
	override val values: Collection<WV>
		get() = wrappedMap.values.map(valueWrapper)

	override fun containsKey(key: WK): Boolean = wrappedMap.containsKey(keyUnwrapper(key))

	override fun containsValue(value: WV): Boolean = wrappedMap.containsValue(valueUnwrapper(value))

	override fun get(key: WK): WV?
	{
		return valueWrapper(wrappedMap[keyUnwrapper(key)] ?: return null)
	}

	override fun isEmpty(): Boolean = size == 0
}
