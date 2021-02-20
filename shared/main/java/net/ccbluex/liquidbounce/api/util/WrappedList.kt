/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

open class WrappedList<WRAPPED, UNWRAPPED, out COLLECTION : List<WRAPPED>>(wrapped: COLLECTION, wrapper: (UNWRAPPED) -> WRAPPED, unwrapper: (WRAPPED) -> UNWRAPPED) : WrappedCollection<WRAPPED, UNWRAPPED, COLLECTION>(wrapped, wrapper, unwrapper), List<UNWRAPPED>
{
	override fun get(index: Int): UNWRAPPED = unwrapper(wrapped[index])

	override fun indexOf(element: UNWRAPPED): Int = wrapped.indexOf(wrapper(element))

	override fun lastIndexOf(element: UNWRAPPED): Int = wrapped.indexOf(wrapper(element))

	override fun listIterator(): ListIterator<UNWRAPPED> = WrappedCollectionIterator(wrapped.listIterator(), unwrapper)

	override fun listIterator(index: Int): ListIterator<UNWRAPPED> = WrappedCollectionIterator(wrapped.listIterator(index), unwrapper)

	override fun subList(fromIndex: Int, toIndex: Int): List<UNWRAPPED> = WrappedList(wrapped.subList(fromIndex, toIndex), wrapper, unwrapper)

	class WrappedCollectionIterator<WRAPPED, out UNWRAPPED>(val wrapped: ListIterator<WRAPPED>, val wrapper: (WRAPPED) -> UNWRAPPED) : ListIterator<UNWRAPPED>
	{
		override fun hasNext(): Boolean = wrapped.hasNext()

		override fun hasPrevious(): Boolean = wrapped.hasPrevious()

		override fun next(): UNWRAPPED = wrapper(wrapped.next())

		override fun nextIndex(): Int = wrapped.nextIndex()

		override fun previous(): UNWRAPPED = wrapper(wrapped.previous())

		override fun previousIndex(): Int = wrapped.previousIndex()
	}
}
