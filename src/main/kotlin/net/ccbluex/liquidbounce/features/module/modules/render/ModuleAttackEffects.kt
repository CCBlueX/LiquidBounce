/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.block.Blocks
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.entity.LivingEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents

@Suppress("MagicNumber")
object ModuleAttackEffects : ClientModule("AttackEffects", Category.RENDER) {

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
        HIT("Hit", SoundEvents.ENTITY_ARROW_HIT),
        ORB("Orb", SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP)
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
            PositionedSoundInstance.master(
                (sound.randomOrNull() ?: return).soundEvent, 1f
            )
        )
    }

    private fun doEffect(target: LivingEntity) {
        when (particle.randomOrNull()) {
            Particle.BLOOD -> world.addBlockBreakParticles(
                target.blockPos.up(1),
                Blocks.REDSTONE_BLOCK.defaultState
            )

            Particle.FIRE -> mc.particleManager.addEmitter(target, ParticleTypes.LAVA)
            Particle.HEART -> mc.particleManager.addEmitter(target, ParticleTypes.HEART)
            Particle.WATER -> mc.particleManager.addEmitter(target, ParticleTypes.FALLING_WATER)
            Particle.SMOKE -> mc.particleManager.addEmitter(target, ParticleTypes.SMOKE)
            Particle.MAGIC -> mc.particleManager.addEmitter(target, ParticleTypes.ENCHANTED_HIT)
            Particle.CRITS -> mc.particleManager.addEmitter(target, ParticleTypes.CRIT)
            else -> return
        }
    }

}
