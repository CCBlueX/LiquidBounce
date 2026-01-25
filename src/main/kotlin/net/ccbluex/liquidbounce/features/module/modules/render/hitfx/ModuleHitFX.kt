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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.Blocks

@Suppress("MagicNumber")
object ModuleHitFX : ClientModule("HitFX", ModuleCategories.RENDER) {

    init {
        HitFXRegistry
    }

    enum class Particle(override val choiceName: String) : NamedChoice {
        BLOOD("Blood"),
        FIRE("Fire"),
        HEART("Heart"),
        WATER("Water"),
        SMOKE("Smoke"),
        MAGIC("Magic"),
        CRITS("Crits")
    }

    @Suppress("unused")
    enum class Sound(
        override val choiceName: String,
        val sounds: Array<SoundEvent>
    ) : NamedChoice {
        HIT("Hit", arrayOf(SoundEvents.ARROW_HIT)),
        ORB("Orb", arrayOf(SoundEvents.EXPERIENCE_ORB_PICKUP)),
        BONK("Bonk", HitFXRegistry.BONK),
        BOYKISSER("Boykisser", HitFXRegistry.BOYKISSER),
        BRING("Bring", HitFXRegistry.BRING),
        GLASS("Glass", HitFXRegistry.GLASS),
        CLICK("Click", HitFXRegistry.CLICK),
        MEOW("Meow", HitFXRegistry.MEOW),
        MOAN("Moan", HitFXRegistry.MOAN),
        MAGICSQUASH("MagicSquash", HitFXRegistry.MAGICSQUASH),
        NYA("NYA", HitFXRegistry.NYA),
        POP("Pop", HitFXRegistry.POP),
        SOFT("Soft", HitFXRegistry.SOFT),
        SQUASH("Squash", HitFXRegistry.SQUASH),
        TUNG("Tung", HitFXRegistry.TUNG),
        UWU("UWU", HitFXRegistry.UWU),
    }

    private val particle by multiEnumChoice(
        "Particle",
        Particle.FIRE
    )

    private val sound by multiEnumChoice("Sound",
        Sound.POP
    )

    private val amount by int("ParticleAmount", 1, 1..20)

    @Suppress("unused")
    val onAttack = handler<AttackEntityEvent> { event ->
        val target = event.entity

        if (target is LivingEntity) {
            if (!target.isAlive) {
                return@handler
            }

            repeat(amount) {
                doEffect(target)
            }

            doSound()
        }
    }

    private fun doSound() {
        val sounds = (sound.randomOrNull() ?: return).sounds
        val sound = sounds.randomOrNull() ?: return

        mc.soundManager.play(SimpleSoundInstance.forUI(sound, 1f))
    }

    private fun doEffect(target: LivingEntity) {
        when (particle.randomOrNull()) {
            Particle.BLOOD -> world.addDestroyBlockEffect(
                target.blockPosition().above(1),
                Blocks.REDSTONE_BLOCK.defaultBlockState()
            )

            Particle.FIRE -> mc.particleEngine.createTrackingEmitter(target, ParticleTypes.LAVA)
            Particle.HEART -> mc.particleEngine.createTrackingEmitter(target, ParticleTypes.HEART)
            Particle.WATER -> mc.particleEngine.createTrackingEmitter(target, ParticleTypes.FALLING_WATER)
            Particle.SMOKE -> mc.particleEngine.createTrackingEmitter(target, ParticleTypes.SMOKE)
            Particle.MAGIC -> mc.particleEngine.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT)
            Particle.CRITS -> mc.particleEngine.createTrackingEmitter(target, ParticleTypes.CRIT)
            else -> return
        }
    }

}
