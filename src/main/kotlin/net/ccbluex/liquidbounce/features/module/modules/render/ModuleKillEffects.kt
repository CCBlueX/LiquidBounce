package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.session.KilledTarget
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.block.Blocks
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LightningEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.particle.DustColorTransitionParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.particle.ShriekParticleEffect
import net.minecraft.sound.SoundEvents
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ModuleKillEffects : ClientModule("KillEffects", Category.RENDER) {

    enum class Effect(override val choiceName: String) : NamedChoice {
        BLOOD("Blood"),
        LIGHTNING("Lightning"),
        EXPLOSION("Explosion"),
        FIREWORK("Firework"),
        SOUL("Soul"),
        SMOKE("SmokeCloud"),
        FREEZE("Freeze"),
        PORTAL("Portal"),
        WITCH("WitchMagic"),
        TOTEM("Totem"),
        SCULK("Sculk"),
        FLASH("Flash"),
        SHRIEK("Shriek"),
        COLORBURST("Colorburst"),
        LAVA_SPARK("Bonfire"),
        GLASS_SHATTER("GlassShatter"),
        SNOWBALL_BURST("SnowballBurst")
    }

    private val effects by multiEnumChoice(
        "Effects",
        Effect.BLOOD,
        canBeNone = false
    )
    private val onlyPlayer by boolean("Only Player", true)
    private val volume by float("Volume", 1f, 0f..1f)
    private val particleCount by int("Particle Count", 20, 5..100)
    private val renderEntities = mutableMapOf<Entity, Long>()

    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            val now = System.currentTimeMillis()
            mc.world ?: return@register

            KilledTarget.tick()

            KilledTarget.getKilledEntities().forEach { entity ->
                if (entity == mc.player) return@forEach
                if (!onlyPlayer || entity.type == EntityType.PLAYER) {
                    if (!renderEntities.containsKey(entity)) {
                        renderEntities[entity] = now
                        onKillEffect(entity)
                    }
                }
            }

            // Clear old effects
            val effectIterator = renderEntities.entries.iterator()
            while (effectIterator.hasNext()) {
                val (entity, time) = effectIterator.next()
                if (now - time > 3000L) effectIterator.remove()
            }
        }
    }

    @Suppress("CognitiveComplexMethod","LongMethod")
    private fun onKillEffect(entity: Entity) {
        val world = mc.world ?: return
        val pos = entity.pos
        val x = pos.x
        val y = pos.y
        val z = pos.z

        if (Effect.LIGHTNING in effects) {
            val bolt = LightningEntity(EntityType.LIGHTNING_BOLT, world)
            bolt.refreshPositionAfterTeleport(pos)
            bolt.setCosmetic(true)
            world.addEntity(bolt)

            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                    volume,
                    1f
                )
            )
        }

        if (Effect.EXPLOSION in effects) {
            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                    volume,
                    1f
                )
            )

            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.EXPLOSION,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.0, 0.0
                )
            }
        }
        if (Effect.LAVA_SPARK in effects) {
            world.addBlockBreakParticles(
                entity.blockPos.up(1),
                Blocks.REDSTONE_BLOCK.defaultState
            )
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.LAVA,
                    x + (Math.random() - 0.5) * 1.5,
                    y + Math.random() * 1.5,
                    z + (Math.random() - 0.5) * 1.5,
                    0.0, 0.01, 0.0
                )
            }
        }

        if (Effect.GLASS_SHATTER in effects) {
            world.addBlockBreakParticles(
                entity.blockPos.up(1),
                Blocks.GLASS.defaultState
            )
            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvents.BLOCK_GLASS_BREAK,
                    volume,
                    1f
                )
            )
        }


        if (Effect.SNOWBALL_BURST in effects) {
            repeat(particleCount) {
                val theta = Math.random() * 2 * Math.PI
                val phi = Math.random() * Math.PI

                val vx = sin(phi) * cos(theta) * 0.5
                val vy = cos(phi) * 0.5
                val vz = sin(phi) * sin(theta) * 0.5

                world.addParticle(
                    ParticleTypes.SNOWFLAKE,
                    x, y + 1.0, z,
                    vx, vy, vz
                )
            }
        }
        if (Effect.BLOOD in effects && entity is LivingEntity) {
            world.addBlockBreakParticles(
                entity.blockPos.up(1),
                Blocks.REDSTONE_BLOCK.defaultState
            )
        }

        if (Effect.FIREWORK in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.FIREWORK,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 0.1,
                    Math.random() * 0.1,
                    (Math.random() - 0.5) * 0.1
                )
            }
            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH,
                    volume,
                    1f
                )
            )
        }
        if (Effect.TOTEM in effects && entity is LivingEntity) {
            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvents.ITEM_TOTEM_USE,
                    volume,
                    1f
                )
            )
            val px = x
            val py = y + entity.height * 0.5 + 0.5
            val pz = z

            repeat(particleCount * 4) {

                val theta = Math.random() * 2.0 * Math.PI
                val phi = acos(2.0 * Math.random() - 1.0)

                val vx = sin(phi) * cos(theta)
                val vy = sin(phi) * sin(theta)
                val vz = cos(phi)

                val speed = 0.5 + Math.random() * 0.5
                world.addParticle(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    px, py, pz,
                    vx * speed,
                    vy * speed + 0.2,
                    vz * speed
                )
            }
        }
        if (Effect.SOUL in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.SOUL,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 0.05,
                    Math.random() * 0.05,
                    (Math.random() - 0.5) * 0.05
                )
            }
        }

        if (Effect.SMOKE in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.05, 0.0
                )
            }
        }

        if (Effect.FREEZE in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.SNOWFLAKE,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.0, 0.0
                )
            }
            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvents.BLOCK_GLASS_BREAK,
                    volume,
                    1f
                )
            )
        }

        if (Effect.PORTAL in effects) {
            repeat(particleCount * 2) {
                world.addParticle(
                    ParticleTypes.REVERSE_PORTAL,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 0.1,
                    Math.random() * 0.1,
                    (Math.random() - 0.5) * 0.1
                )
            }
            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvents.BLOCK_PORTAL_AMBIENT,
                    volume,
                    1f
                )
            )
        }
        if (Effect.SCULK in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.SCULK_SOUL,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.05, 0.0
                )
            }
        }
        if (Effect.FLASH in effects) {
            world.addParticle(
                ParticleTypes.FLASH,
                x, y + 1.0, z,
                0.0, 0.0, 0.0
            )
        }
        if (Effect.SHRIEK in effects) {
            val effect = ShriekParticleEffect(0)
            val player = mc.player ?: return

            val dirX = player.x - x
            val dirY = player.eyeY - y
            val dirZ = player.z - z
            val magnitude = sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ).coerceAtLeast(0.001)
            val unitX = dirX / magnitude
            val unitY = dirY / magnitude
            val unitZ = dirZ / magnitude

            repeat(particleCount / 2) {
                val px = x + (Math.random() - 0.5) * 1.0
                val py = y + 1.0 + Math.random() * 0.5
                val pz = z + (Math.random() - 0.5) * 1.0

                world.addParticle(
                    effect,
                    px, py, pz,
                    unitX * 0.2,
                    unitY * 0.2,
                    unitZ * 0.2
                )
            }

            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                    volume,
                    1f
                )
            )
        }




        if (Effect.COLORBURST in effects) {
            val from = Color4b.Companion.WHITE
            val to = Color4b.Companion.TRANSPARENT

            val effect = DustColorTransitionParticleEffect(
                from.toARGB(),
                to.toARGB(),
                1.0f
            )

            repeat(particleCount) {
                world.addParticle(
                    effect,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 1.5,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.02, 0.0
                )
            }
        }



        if (Effect.WITCH in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.WITCH,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.0, 0.0
                )
            }
            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvents.ENTITY_WITCH_AMBIENT,
                    volume,
                    1f
                )
            )
        }
    }
}
