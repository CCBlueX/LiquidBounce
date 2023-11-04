/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.block.Blocks
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.entity.LivingEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundEvents

object ModuleAttackEffects : Module("AttackEffects", Category.RENDER) {

    enum class Particle(override val choiceName: String) : NamedChoice {
        NONE("None"),
        BLOOD("Blood"),
        FIRE("Fire"),
        HEART("Heart"),
        WATER("Water"),
        SMOKE("Smoke"),
        MAGIC("Magic"),
        CRITS("Crits")
    }

    private val particle by enumChoice("Particle", Particle.FIRE, Particle.values())
    private val amount by int("ParticleAmount", 1, 1..20)
    enum class Sound(override val choiceName: String) : NamedChoice {
        NONE("None"),
        HIT("Hit"),
        ORB("Orb")
    }

    private val sound by enumChoice("Sound", Sound.ORB, Sound.values())

    val onAttack = handler<AttackEvent> { event ->
        val target = event.enemy

        if (target is LivingEntity) {
            repeat(amount) {
                doEffect(target)
            }

            doSound()
        }
    }

    private fun doSound() {
        mc.soundManager.play(PositionedSoundInstance.master(when (sound) {
            Sound.HIT -> SoundEvents.ENTITY_ARROW_HIT
            Sound.ORB -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP
            Sound.NONE -> return
        }, 1F))
    }

    private fun doEffect(target: LivingEntity) {
        when (particle) {
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
