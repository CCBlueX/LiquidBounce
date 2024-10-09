package net.ccbluex.liquidbounce.integration.theme.type.web

import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory

data class ThemeMetadata(
    val name: String,
    val authors: List<String>,
    val version: String,
    val supports: List<String>,
    val overlays: List<String>,
    val components: List<ComponentFactory.JsonComponentFactory>
)
