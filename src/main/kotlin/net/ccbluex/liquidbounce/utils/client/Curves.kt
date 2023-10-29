package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.config.NamedChoice


sealed interface Curve: NamedChoice {
    val at: (Float) -> Float
}

enum class Curves(override val choiceName: String, override val at: (Float) -> Float): Curve {
    LINEAR ("Linear", { t ->
        t
    }),
    EASE_IN ("EaseIn", { t ->
        t * t
    }),
    EASE_OUT ("EaseOut", { t ->
        1 - (1 - t) * (1 - t)
    }),
    Ease_IN_OUT ("EaseInOut", { t ->
        2 * (1 - t) * t * t + t * t
    })
}


