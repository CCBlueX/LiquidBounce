package net.ccbluex.liquidbounce.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class InbuiltEvent(val name: String)
