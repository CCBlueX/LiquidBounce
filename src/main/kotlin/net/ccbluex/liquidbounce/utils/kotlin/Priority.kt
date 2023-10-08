package net.ccbluex.liquidbounce.utils.kotlin

enum class Priority(val priority: Int) {
    IMPORTANT_FOR_USER_SAFETY(60),
    IMPORTANT_FOR_PLAYER_LIFE(40),
    IMPORTANT_FOR_USAGE(20),
    NORMAL(0),
    NOT_IMPORTANT(-20),
}
