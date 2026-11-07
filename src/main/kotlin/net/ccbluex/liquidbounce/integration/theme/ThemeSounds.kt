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
package net.ccbluex.liquidbounce.integration.theme

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.features.misc.SelfDestruct
import net.ccbluex.liquidbounce.utils.client.FileSounds
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.sounds.WeighedSoundEvents
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.Resource
import java.io.ByteArrayInputStream
import java.util.Locale

/**
 * Registers the sounds themes ship ([ThemeMetadata.sounds]) with the game's sound engine as
 * `liquidbounce:theme/<theme id>/<name>`, so pages can play them like any game sound.
 */
object ThemeSounds {

    private val files = FileSounds()

    /** The id of [name] in [themeId]'s sounds; null when either cannot form an id. */
    fun soundId(themeId: String, name: String): Identifier? =
        if (Identifier.isValidPath(name) && '/' !in name) {
            Identifier.tryBuild(CLIENT_NAME.lowercase(Locale.ROOT), "theme/${themeId.lowercase(Locale.ROOT)}/$name")
        } else {
            null
        }

    /**
     * Replaces the registered theme sounds with those of the loaded themes. Runs on the render thread.
     */
    fun sync() {
        unregister()
        register(mc.soundManager.registry, mc.soundManager.soundCache)
    }

    /**
     * Adds the sounds of the loaded themes to [registry] and their files to [cache]; the active theme wins a shared
     * id. Also called after the sound manager rebuilt both on a resource reload.
     */
    @JvmStatic
    fun register(registry: MutableMap<Identifier, WeighedSoundEvents>, cache: MutableMap<Identifier, Resource>) {
        files.forget()
        if (SelfDestruct.isDestructed) {
            return
        }

        for (theme in (listOfNotNull(ThemeManager.theme) + ThemeManager.themes).distinct()) {
            for ((name, data) in theme.sounds) {
                val id = soundId(theme.metadata.id, name) ?: continue
                // The engine caches decoded files by path; a changed file gets a new one, so it is not served stale
                val file = id.withSuffix("/${Integer.toHexString(data.contentHashCode())}")
                files.add(registry, cache, id, file) { ByteArrayInputStream(data) }
            }
        }
    }

    fun unregister() = files.remove()

}
