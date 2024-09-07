package net.ccbluex.liquidbounce.integration.theme.type.web

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.utils.render.Alignment

data class ThemeMetadata(
    val name: String,
    val authors: List<String>,
    val version: String,
    val supports: List<String>,
    val overlays: List<String>,
    val components: Map<String, ComponentMetadata>
)

data class ComponentMetadata(
    val enabled: Boolean,
    val alignment: ComponentAlignmentMetadata,
    val tweaks: List<String>?
)

data class ComponentAlignmentMetadata(
    val horizontal: String,
    val horizontalOffset: Int,
    val vertical: String,
    val verticalOffset: Int
) {
    fun toAlignment() = Alignment(
        Alignment.ScreenAxisX.entries.find { it.choiceName == horizontal } ?: error("axis x"),
        horizontalOffset,
        Alignment.ScreenAxisY.entries.find { it.choiceName == vertical } ?: error("axis y"),
        verticalOffset
    )
}
