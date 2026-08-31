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
package net.ccbluex.liquidbounce.features.addon

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.loader.api.ModContainer
import java.nio.file.Path

/**
 * An add-on's identity, read from the providing mod's `fabric.mod.json`.
 *
 * Nothing here is declared in add-on code. Fabric already requires this metadata, so duplicating
 * it would only let the two drift apart.
 */
class AddonMetadata(private val container: ModContainer) {

    private val meta get() = container.metadata

    val id: String get() = meta.id
    val name: String get() = meta.name
    val version: String get() = meta.version.friendlyString
    val description: String get() = meta.description
    val authors: List<String> get() = meta.authors.map { it.name }

    val homepage: String? get() = contact("homepage")
    val sources: String? get() = contact("sources")
    val issues: String? get() = contact("issues")

    /**
     * Accent colour from `custom.liquidbounce.color`, used to tint the add-on in listings.
     */
    val color: Color4b? by lazy {
        val raw = meta.getCustomValue(CUSTOM_NAMESPACE)
            ?.takeIf { it.type == net.fabricmc.loader.api.metadata.CustomValue.CvType.OBJECT }
            ?.asObject
            ?.get(CUSTOM_COLOR)
            ?.takeIf { it.type == net.fabricmc.loader.api.metadata.CustomValue.CvType.STRING }
            ?.asString
            ?: return@lazy null

        runCatching { Color4b.fromHex(raw) }
            .onFailure { logger.warn("Add-on $id declares an unreadable color '$raw'", it) }
            .getOrNull()
    }

    /**
     * The jar (or directory, in a dev environment) the add-on was loaded from.
     */
    val origin: List<Path> get() = container.origin.paths

    fun findPath(path: String): Path? = container.findPath(path).orElse(null)

    private fun contact(key: String): String? = meta.contact.get(key).orElse(null)

    private companion object {
        const val CUSTOM_NAMESPACE = "liquidbounce"
        const val CUSTOM_COLOR = "color"
    }

}
