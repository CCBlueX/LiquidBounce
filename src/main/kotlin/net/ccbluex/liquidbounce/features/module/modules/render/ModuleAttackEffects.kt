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
package net.ccbluex.liquidbounce.features.module.modules.render

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
object ModuleAttackEffects : ClientModule("AttackEffects", ModuleCategories.RENDER) {

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
        val soundEvent: SoundEvent
    ) : NamedChoice {
        HIT("Hit", SoundEvents.ARROW_HIT),
        ORB("Orb", SoundEvents.EXPERIENCE_ORB_PICKUP)
    }

    private val particle by multiEnumChoice(
        "Particle",
        Particle.FIRE
    )

    private val sound by multiEnumChoice("Sound",
        Sound.ORB
    )

    private val amount by int("ParticleAmount", 1, 1..20)

    @Suppress("unused")
    val onAttack = handler<AttackEntityEvent> { event ->
        val target = event.entity

        if (target is LivingEntity) {
            repeat(amount) {
                doEffect(target)
            }

            doSound()
        }
    }

    private fun doSound() {
        mc.soundManager.play(
            SimpleSoundInstance.forUI(
                (sound.randomOrNull() ?: return).soundEvent, 1f
            )
        )
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
