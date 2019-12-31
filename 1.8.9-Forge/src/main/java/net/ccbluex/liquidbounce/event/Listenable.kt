package net.ccbluex.liquidbounce.event

import java.lang.reflect.Method

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */

interface Listenable {
    fun handleEvents(): Boolean
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class EventTarget(val ignoreCondition: Boolean = false)

internal class EventHook(val eventClass: Listenable, val method: Method, eventTarget: EventTarget) {
    val isIgnoreCondition = eventTarget.ignoreCondition
}