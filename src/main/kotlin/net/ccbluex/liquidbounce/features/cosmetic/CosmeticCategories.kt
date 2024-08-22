package net.ccbluex.liquidbounce.features.cosmetic

data class Cosmetic(val category: CosmeticCategory, val extra: String? = null)

enum class CosmeticCategory {
    CAPE,
    DEADMAU5_EARS,
    DINNERBONE,
}
