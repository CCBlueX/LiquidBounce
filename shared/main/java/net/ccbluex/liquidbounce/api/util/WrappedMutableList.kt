/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

class WrappedMutableList<WRAPPED, UNWRAPPED, out COLLECTION : MutableList<WRAPPED>>(wrapped: COLLECTION, unwrapper: (UNWRAPPED) -> WRAPPED, wrapper: (WRAPPED) -> UNWRAPPED) : WrappedMutableCollection<WRAPPED, UNWRAPPED, COLLECTION>(wrapped, unwrapper, wrapper), MutableList<UNWRAPPED>
{
	override fun get(index: Int): UNWRAPPED = unwrapper(wrapped[index])

	override fun indexOf(element: UNWRAPPED): Int = wrapped.indexOf(wrapper(element))

	override fun lastIndexOf(element: UNWRAPPED): Int = wrapped.lastIndexOf(wrapper(element))

	override fun add(index: Int, element: UNWRAPPED) = wrapped.add(index, wrapper(element))

	override fun addAll(index: Int, elements: Collection<UNWRAPPED>): Boolean = wrapped.addAll(index, elements.map(wrapper))

	override fun listIterator(): MutableListIterator<UNWRAPPED> = WrappedMutableCollectionIterator(wrapped.listIterator(), wrapper, unwrapper)

	override fun listIterator(index: Int): MutableListIterator<UNWRAPPED> = WrappedMutableCollectionIterator(wrapped.listIterator(index), wrapper, unwrapper)

	override fun removeAt(index: Int): UNWRAPPED = unwrapper(wrapped.removeAt(index))

	override fun set(index: Int, element: UNWRAPPED): UNWRAPPED = unwrapper(wrapped.set(index, wrapper(element)))

	override fun subList(fromIndex: Int, toIndex: Int): MutableList<UNWRAPPED> = WrappedMutableList(wrapped.subList(fromIndex, toIndex), wrapper, unwrapper)

	class WrappedMutableCollectionIterator<WRAPPED, UNWRAPPED>(val wrapped: MutableListIterator<WRAPPED>, val wrapper: (UNWRAPPED) -> WRAPPED, val unwrapper: (WRAPPED) -> UNWRAPPED) : MutableListIterator<UNWRAPPED>
	{
		override fun hasNext(): Boolean = wrapped.hasNext()

		override fun hasPrevious(): Boolean = wrapped.hasPrevious()

		override fun next(): UNWRAPPED = unwrapper(wrapped.next())

		override fun nextIndex(): Int = wrapped.nextIndex()

		override fun previous(): UNWRAPPED = unwrapper(wrapped.previous())

		override fun previousIndex(): Int = wrapped.previousIndex()
		override fun add(element: UNWRAPPED) = wrapped.add(wrapper(element))

		override fun remove() = wrapped.remove()

		override fun set(element: UNWRAPPED) = wrapped.set(wrapper(element))
	}
}
