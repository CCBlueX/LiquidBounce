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

package net.ccbluex.liquidbounce.features.module.modules.render.hitfx

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.utils.client.clientIdentifier
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents

@Suppress("unused")
enum class HitFXRegistry(
    override val tag: String,
    private val vanillaSounds: Array<SoundEvent> = emptyArray(),
    private val customSoundIds: Array<String> = emptyArray()
) : Tagged {
    HIT("Hit", vanillaSounds = arrayOf(SoundEvents.ARROW_HIT)),
    ORB("Orb", vanillaSounds = arrayOf(SoundEvents.EXPERIENCE_ORB_PICKUP)),
    BONK("Bonk", customSoundIds = arrayOf("bonk")),
    BOYKISSER("Boykisser", customSoundIds = arrayOf(
        "boykisser-1",
        "boykisser-2",
        "boykisser-3",
        "boykisser-4",
        "boykisser-5",
        "boykisser-6"
    )),
    BRING("Bring", customSoundIds = arrayOf("bring")),
    GLASS("Glass", customSoundIds = arrayOf("glass-1", "glass-2", "glass-3")),
    CLICK("Click", customSoundIds = arrayOf("click-1", "click-2", "click-3")),
    MEOW("Meow", customSoundIds = arrayOf("meow")),
    MOAN("Moan", customSoundIds = arrayOf("moan-1", "moan-2", "moan-3", "moan-4")),
    MAGIC_SQUASH("MagicSquash", customSoundIds = arrayOf("magic_squash")),
    NYA("NYA", customSoundIds = arrayOf("nya")),
    POP("Pop", customSoundIds = arrayOf("pop")),
    SOFT("Soft", customSoundIds = arrayOf("soft")),
    SQUASH("Squash", customSoundIds = arrayOf("squash")),
    TUNG("Tung", customSoundIds = arrayOf("tung")),
    UWU("UWU", customSoundIds = arrayOf("uwu"));

    var sounds: Array<SoundEvent> = vanillaSounds
        private set

    companion object {
        private val customSounds = mutableListOf<SoundEvent>()

        private var registered = false

        @JvmStatic
        fun registerAll() {
            if (registered) {
                return
            }

            for (type in entries) {
                type.sounds = registerCustom(type.customSoundIds.ifEmpty { continue })
            }

            registered = true
            logger.info("HitFXRegistry initialized ${customSounds.size} custom sounds.")
        }

        private fun registerCustom(ids: Array<out String>): Array<SoundEvent> = ids.mapToArray { id ->
            val soundId = clientIdentifier(id)

            Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                soundId,
                SoundEvent.createVariableRangeEvent(soundId)
            ).also(customSounds::add)
        }
    }
}
