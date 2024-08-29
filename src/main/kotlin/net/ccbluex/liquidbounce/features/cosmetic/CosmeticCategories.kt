package net.ccbluex.liquidbounce.features.cosmetic

import com.google.gson.annotations.SerializedName

data class Cosmetic(val category: CosmeticCategory, val extra: String? = null)

@Suppress("SpellCheckingInspection")
enum class CosmeticCategory {
    @SerializedName("Cape")
    CAPE,
    @SerializedName("Deadmau5Ears")
    DEADMAU5_EARS,
    @SerializedName("Dinnerbone")
    DINNERBONE,
}
