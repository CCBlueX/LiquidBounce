/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event

interface Listenable {

    /**
     * Allows to disable event handling when condition is false.
     */
    fun handleEvents(): Boolean

}

class EventHook<T: Event>(val handlerClass: Listenable, val handler: (T) -> Unit, val ignoresCondition: Boolean)