package net.ccbluex.liquidbounce.features.cosmetic

import com.google.gson.annotations.SerializedName

data class Cosmetic(val category: CosmeticCategory, private val extra: String? = null) {

    /**
     * Split up extra using the format name:value into a hashmap and then return the value of the given name
     */
    fun getExtra(name: String) = extra
        ?.split(";")
        ?.associate { it.split(":").let { (key, value) -> key to value } }
        ?.get(name)

}

@Suppress("SpellCheckingInspection")
enum class CosmeticCategory {
    @SerializedName("NametagLogo")
    NAMETAG_LOGO,
    @SerializedName("Cape")
    CAPE,
    @SerializedName("Deadmau5Ears")
    DEADMAU5_EARS,
    @SerializedName("Dinnerbone")
    DINNERBONE,
}
