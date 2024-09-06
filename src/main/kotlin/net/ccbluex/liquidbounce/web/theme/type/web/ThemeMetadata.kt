package net.ccbluex.liquidbounce.web.theme.type.web

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName

data class ThemeMetadata(
    val name: String,
    val author: String,
    val version: String,
    val supports: List<String>,
    val overlays: List<String>,
    @SerializedName("components")
    val rawComponents: JsonArray
)
