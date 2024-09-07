package net.ccbluex.liquidbounce.integration.theme.type.web

import net.ccbluex.liquidbounce.config.util.decode

import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.web.components.WebComponent
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File

class WebTheme(val folder: File) : Theme {

    override val name: String = folder.name
    override val components: List<Component> = parseComponents()

    private val metadata: ThemeMetadata = run {
        val metadataFile = File(folder, "metadata.json")
        if (!metadataFile.exists()) {
            error("Theme $name does not contain a metadata file")
        }

        decode<ThemeMetadata>(metadataFile.readText())
    }

    private val url: String
        get() = "${ClientInteropServer.url}/$name/#/"

    override fun route(screenType: VirtualScreenType?) =
        "$url${screenType?.routeName ?: ""}".let { url ->
            RouteType.Web(
                type = screenType,
                theme = this,
                url = if (screenType?.isStatic == true) {
                    "$url?static"
                } else {
                    url
                }
            )
        }

    override fun doesSupport(type: VirtualScreenType?) =
        type != null && metadata.supports.contains(type.routeName)

    override fun doesOverlay(type: VirtualScreenType?) =
        type != null && metadata.overlays.contains(type.routeName)

    private fun parseComponents(): MutableList<Component> {
        val themeComponent = metadata.components
        val componentList = mutableListOf<Component>()

        for ((name, componentMeta) in themeComponent) {
            runCatching {
                componentList += WebComponent(this, name, componentMeta)
            }.onFailure {
                logger.error("Failed to create component $name", it)
            }
        }

        return mutableListOf()
    }

}
