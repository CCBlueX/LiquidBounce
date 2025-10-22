package net.ccbluex.liquidbounce.utils.kotlin

/**
 * Used for mixin interfaces (i.e. [net.ccbluex.liquidbounce.interfaces.LightmapTextureManagerAddition])
 */
inline fun <reified B> mixinInterfaceCast(a: Any): B {
    check(a is B) { "${a.javaClass.name} does not implement the mixin interface ${B::class}?!" }

    return a
}

/**
 * See [mixinInterfaceCast]
 */
inline fun <reified B> mixinInterfaceCastNullable(a: Any?): B? {
    if (a == null) {
        return null
    }

    return mixinInterfaceCast(a)
}
