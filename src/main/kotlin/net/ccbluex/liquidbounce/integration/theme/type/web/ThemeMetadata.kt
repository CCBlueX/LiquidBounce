package net.ccbluex.liquidbounce.integration.theme.type.web

import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.component.AlignmentDeserializer
import net.ccbluex.liquidbounce.utils.render.Alignment

internal val metadataGson = GsonBuilder()
    .registerTypeHierarchyAdapter(Alignment::class.java, AlignmentDeserializer)
    .create()

data class ThemeMetadata(
    val name: String,
    val authors: List<String>,
    val version: String,
    val supports: List<String>,
    val overlays: List<String>,
    val components: List<ComponentFactory.DeserializedComponentFactory>
)
