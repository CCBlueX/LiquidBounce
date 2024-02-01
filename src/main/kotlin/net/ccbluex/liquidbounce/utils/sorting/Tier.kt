package net.ccbluex.liquidbounce.utils.sorting

enum class Tier: Comparable<Tier> {
    F, E, D, C, B, A, S;

    val score: Int = this.ordinal
}
