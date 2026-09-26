/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.command.commands.ingame

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.clickablePath
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.handItems
import net.ccbluex.liquidbounce.utils.io.clipboardSet
import net.ccbluex.liquidbounce.utils.item.toNativeImage
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.saveddata.maps.MapId
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import java.io.File

/**
 * MapImage Command
 *
 * Renders the map currently held (main or off hand) as a 128x128 PNG image, then either
 * copies it to the system clipboard (as an actual image) or saves it under the liquidbounce folder.
 * An optional filename may be given to `save`, otherwise a unique timestamped name is generated.
 */
object CommandMapImage : CommandRegistrar {

    private val dir = File(ConfigSystem.rootFolder, "map_image")

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("map_image") {
            requires { it.isIngame }

            literal("copy") {
                exec {
                    val (_, data) = heldMapData() ?: run {
                        chat(markAsError(t("notHoldingMap")))
                        return@exec 1
                    }
                    data.toNativeImage().use { image ->
                        if (clipboardSet(image)) {
                            chat(regular(t("copy.success")))
                        } else {
                            chat(markAsError(t("copy.failed")))
                        }
                    }
                    1
                }
            }
            literal("save") {
                optional("filename", ClientStringArgumentType.string()) { filename ->
                    exec { ctx ->
                        val (mapId, data) = heldMapData() ?: run {
                            chat(markAsError(t("notHoldingMap")))
                            return@exec 1
                        }

                        data.toNativeImage().use { image ->
                            dir.mkdirs()
                            val file = dir.resolve(
                                ctx.get(filename)?.let {
                                    if (!it.endsWith(".png", ignoreCase = true)) "$it.png" else it
                                } ?: "map_${mapId.id}_${System.currentTimeMillis().toString(16)}.png"
                            )

                            image.writeToFile(file)
                            chat(regular(t("save.success")).append(clickablePath(file)))
                        }
                        1
                    }
                }
            }
        }
    }

    private fun heldMapData(): Pair<MapId, MapItemSavedData>? =
        player.handItems.firstNotNullOfOrNull { stack ->
            val component = stack[DataComponents.MAP_ID] ?: return@firstNotNullOfOrNull null
            MapItem.getSavedData(component, world)?.let { Pair(component, it) }
        }

}
