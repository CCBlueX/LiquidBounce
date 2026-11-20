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
package net.ccbluex.liquidbounce.utils.client

import net.minecraft.client.resources.sounds.Sound
import net.minecraft.client.sounds.WeighedSoundEvents
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.IoSupplier
import net.minecraft.server.packs.resources.Resource
import net.minecraft.util.valueproviders.ConstantFloat
import java.io.InputStream

/**
 * Sound events of one file each that the client adds to the game's sound registry, outside resource packs.
 * A resource reload rebuilds the registry without them, so they are added again after each one.
 */
internal class FileSounds {

    /** Added event ids and where their files sit in the sound cache. */
    private val added = mutableMapOf<Identifier, Identifier>()

    /**
     * Adds [id] as a sound event that plays one file, read from [open] and cached as [file]. An id the registry
     * already has keeps its sound.
     */
    fun add(
        registry: MutableMap<Identifier, WeighedSoundEvents>,
        cache: MutableMap<Identifier, Resource>,
        id: Identifier,
        file: Identifier = id,
        open: IoSupplier<InputStream>,
    ) {
        if (id in registry) {
            return
        }

        val sound = Sound(
            file, ConstantFloat.of(1F), ConstantFloat.of(1F), 1, Sound.Type.FILE, false, false, ATTENUATION_DISTANCE
        )
        registry[id] = WeighedSoundEvents(id, null).apply { addSound(sound) }
        cache[sound.path] = Resource(mc.vanillaPackResources.fullResources(), open)
        added[id] = sound.path
    }

    /** Forgets the added sounds once a resource reload dropped them. */
    fun forget() = added.clear()

    /** Removes the added sounds from the game. */
    fun remove() {
        val manager = mc.soundManager
        for ((id, file) in added) {
            manager.registry.remove(id)
            manager.soundCache.remove(file)
        }
        added.clear()
    }

    private companion object {
        const val ATTENUATION_DISTANCE = 16
    }

}
