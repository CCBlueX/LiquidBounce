package net.ccbluex.liquidbounce.integration.theme.type.web

import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.config.util.decode

import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.component.FriendlyAlignmentDeserializer
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.wallpaper.Wallpaper
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.Alignment
import java.io.File

class WebTheme(val folder: File) : Theme {

    override val name: String = folder.name
    private val metadata: ThemeMetadata = run {
        val metadataFile = File(folder, "metadata.json")
        if (!metadataFile.exists()) {
            error("Theme $name does not contain a metadata file")
        }

        metadataGson.fromJson(metadataFile.readText(), ThemeMetadata::class.java)
    }

    override val components: List<ComponentFactory>
        get() = metadata.components

    private val url: String
        get() = "${ClientInteropServer.url}/$name/#/"

    override val wallpapers: List<Wallpaper> = folder.resolve("wallpapers").listFiles()
        ?.mapNotNull { file ->
            runCatching {
                Wallpaper.fromFile(file)
            }.onFailure { error ->
                logger.error("Failed to load wallpaper from file ${file.name} ${error.message}")
            }.getOrNull()
        } ?: emptyList()

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

}
