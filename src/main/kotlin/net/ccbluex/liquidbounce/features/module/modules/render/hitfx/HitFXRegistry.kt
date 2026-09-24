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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.utils.client.clientIdentifier
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.resources.sounds.Sound
import net.minecraft.client.sounds.WeighedSoundEvents
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.Resource
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.valueproviders.ConstantFloat

@Suppress("unused")
enum class HitFXRegistry(
    override val tag: String,
    vanillaSounds: List<SoundEvent> = [],
    private val customSoundIds: List<String> = []
) : Tagged {
    HIT("Hit", vanillaSounds = [SoundEvents.ARROW_HIT]),
    ORB("Orb", vanillaSounds = [SoundEvents.EXPERIENCE_ORB_PICKUP]),
    BONK("Bonk", customSoundIds = ["bonk"]),
    BOYKISSER("Boykisser", customSoundIds = [
        "boykisser-1",
        "boykisser-2",
        "boykisser-3",
        "boykisser-4",
        "boykisser-5",
        "boykisser-6",
    ]),
    APPLEPAY("ApplePay", customSoundIds = ["applepay"]),
    AIMBOOSTER("Aimbooster", customSoundIds = ["aimbooster"]),
    BRING("Bring", customSoundIds = ["bring"]),
    BRICK("Brick", customSoundIds = ["brick"]),
    BUMP("Bump", customSoundIds = ["bump"]),
    GLASS("Glass", customSoundIds = ["glass-1", "glass-2", "glass-3"]),
    CLICK("Click", customSoundIds = ["click-1", "click-2", "click-3"]),
    COIN("Coin", customSoundIds = ["coin"]),
    MEOW("Meow", customSoundIds = ["meow"]),
    MOAN("Moan", customSoundIds = ["moan-1", "moan-2", "moan-3", "moan-4"]),
    MAGIC_SQUASH("MagicSquash", customSoundIds = ["magic_squash"]),
    NYA("NYA", customSoundIds = ["nya"]),
    OSU("OSU", customSoundIds = ["osu"]),
    POP("Pop", customSoundIds = ["pop"]),
    SOFT("Soft", customSoundIds = ["soft"]),
    SCHOOLBOY("Schoolboy", customSoundIds = ["schoolboy"]),
    SKEET("Skeet", customSoundIds = ["skeet"]),
    SLAP("Slap", customSoundIds = ["slap"]),
    SQUASH("Squash", customSoundIds = ["squash"]),
    TUNG("Tung", customSoundIds = ["tung"]),
    TF2CRIT("TF2 Crit", customSoundIds = ["tf2-crit"]),
    UWU("UWU", customSoundIds = ["uwu"]);

    val sounds = vanillaSounds + customSoundIds.map {
        SoundEvent.createVariableRangeEvent(clientIdentifier(it))
    }

    companion {
        fun registerSounds(
            registry: MutableMap<Identifier, WeighedSoundEvents>,
            cache: MutableMap<Identifier, Resource>
        ) {
            for (id in entries.flatMap { it.customSoundIds }) {
                val location = clientIdentifier(id)
                val sound = Sound(
                    location, ConstantFloat.of(1F), ConstantFloat.of(1F), 1, Sound.Type.FILE, false, false, 16
                )

                registry.putIfAbsent(location, WeighedSoundEvents(location, null).apply { addSound(sound) })
                cache.putIfAbsent(sound.path, Resource(mc.vanillaPackResources) {
                    LiquidBounce.resource("sounds/$id.ogg")
                })
            }
        }
    }
}
