
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.ParticleTypes
import kotlin.math.sqrt

object ModuleParticleCosmetics : ClientModule("ParticleCosmetics", Category.RENDER) {

    enum class Effect(override val choiceName: String) : NamedChoice {
        END_ROD("Trail")
    }

    private val effects by multiEnumChoice(
        "Effects",
        Effect.END_ROD,
        canBeNone = false
    )
    private val particleCount by int("ParticleCount", 2, 1..5)
    private val speed by float("Speed", 0.25f, 0.05f..0.5f)
    private val trailLength by float("TrailLength", 0.5f, 0.2f..1.0f)
    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            if (!this.enabled) return@register
            val player = mc.player ?: return@register
            if (!player.isAlive) return@register
            if (Effect.END_ROD in effects) {
                if ( player.isSprinting ||player.fallDistance > 1.25) {
                    spawnEndRodTrail(player)
                }
            }
        }
    }


    private fun spawnEndRodTrail(player: PlayerEntity) {
        val world = mc.world ?: return
        val pos = player.pos
        val x = pos.x
        val y = pos.y
        val z = pos.z
        val velocity = player.velocity


        val magnitude = sqrt(velocity.x * velocity.x + velocity.z * velocity.z).coerceAtLeast(0.001)
        val dirX = -velocity.x / magnitude
        val dirZ = -velocity.z / magnitude

        repeat(particleCount) {
            val offset = Math.random() * trailLength
            world.addParticle(
                ParticleTypes.END_ROD,
                x + dirX * offset,
                y + Math.random() * 0.1,
                z + dirZ * offset,
                dirX * speed * 0.5,
                Math.random() * speed * 0.2,
                dirZ * speed * 0.5
            )
        }
    }
}
