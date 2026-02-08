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

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.Blocks

object ModuleHitFX : ClientModule("HitFX", ModuleCategories.RENDER) {

    init {
        HitFXRegistry
    }

    enum class Particle(override val tag: String) : Tagged {
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
        override val tag: String,
        val sounds: Array<SoundEvent>
    ) : Tagged {
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

    private val particles by multiEnumChoice("Particle", Particle.FIRE)
    private val particleAmount by intRange("ParticleAmount", 1..1, 1..20)

    private val otherSoundSet by multiEnumChoice("OtherSound", Sound.POP)

    private val otherSound
        get() = otherSoundSet.randomOrNull()?.sounds?.randomOrNull()

    val selfSound
        get() = selfSoundSet.randomOrNull()?.sounds?.randomOrNull()

    private val selfSoundSet by multiEnumChoice("SelfSound",
        Sound.BOYKISSER
    )

    private var lastTargetId: Int? = null

    private val vanillaHitSounds = setOf(
        SoundEvents.ARROW_HIT_PLAYER,
        SoundEvents.PLAYER_ATTACK_NODAMAGE,
        SoundEvents.PLAYER_ATTACK_KNOCKBACK,
        SoundEvents.PLAYER_ATTACK_CRIT,
        SoundEvents.PLAYER_ATTACK_STRONG,
        SoundEvents.PLAYER_ATTACK_SWEEP,
        SoundEvents.PLAYER_ATTACK_WEAK,
        SoundEvents.SPEAR_HIT,
        SoundEvents.PLAYER_HURT
    )

    @Suppress("unused")
    private val effectHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet !is ClientboundSoundPacket) {
            return@handler
        }

        val source = packet.source
        val sound = packet.sound.value()

        // Cannot be from any living entity.
        if (source != SoundSource.PLAYERS && source != SoundSource.NEUTRAL && source != SoundSource.HOSTILE) {
            return@handler
        }

        if (sound !in vanillaHitSounds) {
            return@handler
        }

        val lastTarget = lastTargetId?.let { world.getEntity(it) as? LivingEntity } ?: return@handler
        if (!lastTarget.isAlive) {
            return@handler
        }

        val distanceToSq = lastTarget.distanceToSqr(packet.x, packet.y, packet.z)
        if (distanceToSq > 8.0) {
            return@handler
        }

        playEffect(lastTarget)
        otherSound?.let { sound ->
            mc.execute {
                world.playSeededSound(
                    player,
                    packet.x,
                    packet.y,
                    packet.z,
                    sound,
                    packet.source,
                    packet.volume,
                    packet.pitch,
                    packet.seed
                )
            }
            event.cancelEvent()
        }
        this.lastTargetId = null
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        val target = event.entity

        if (target is LivingEntity) {
            if (!target.isAlive) {
                return@handler
            }

            this.lastTargetId = target.id
        }
    }

    private fun playEffect(target: LivingEntity) = mc.execute {
        val particles = particles.ifEmpty { return@execute }
        repeat(particleAmount.random()) {
            when (particles.random()) {
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
            }
        }
    }

}
