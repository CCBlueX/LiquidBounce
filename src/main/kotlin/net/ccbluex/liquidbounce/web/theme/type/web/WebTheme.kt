package net.ccbluex.liquidbounce.web.theme.type.web

import net.ccbluex.liquidbounce.config.util.decode

import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.interop.ClientInteropServer
import net.ccbluex.liquidbounce.web.theme.component.Component
import net.ccbluex.liquidbounce.web.theme.type.RouteType
import net.ccbluex.liquidbounce.web.theme.type.Theme
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

    override fun doesAccept(type: VirtualScreenType?) =
        doesSupport(type) || doesOverlay(type)

    override fun doesSupport(type: VirtualScreenType?) =
        type != null && metadata.supports.contains(type.routeName)

    override fun doesOverlay(type: VirtualScreenType?) =
        type != null && metadata.overlays.contains(type.routeName)

    private fun parseComponents(): MutableList<Component> {
//        val themeComponent = metadata.rawComponents
//            .map { it.asJsonObject }
//            .associateBy { it["name"].asString!! }
//
//        val componentList = mutableListOf<Component>()
//
//        for ((name, obj) in themeComponent) {
//            runCatching {
//                val componentType = ComponentType.byName(name) ?: error("Unknown component type: $name")
//                val component = componentType.createComponent(this)
//
//                runCatching {
//                    ConfigSystem.deserializeConfigurable(component, obj)
//                }.onFailure {
//                    logger.error("Failed to deserialize component $name", it)
//                }
//
//                componentList.add(component)
//            }.onFailure {
//                logger.error("Failed to create component $name", it)
//            }
//        }

        return mutableListOf()
    }

}
