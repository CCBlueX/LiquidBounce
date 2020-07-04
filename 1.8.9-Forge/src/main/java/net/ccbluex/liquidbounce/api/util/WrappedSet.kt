/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

class WrappedSet<O, T>(wrapped: Set<O>, unwrapper: (T) -> O, wrapper: (O) -> T) : WrappedCollection<O, T, Collection<O>>(wrapped, unwrapper, wrapper), Set<T>