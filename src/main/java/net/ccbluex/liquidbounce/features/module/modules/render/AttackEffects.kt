package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Block
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.init.Blocks
import net.minecraft.network.packet.s2c.play.EntitySpawnGlobalS2CPacket
import net.minecraft.util.EnumParticleTypes

object AttackEffects : Module("AttackEffects", Category.RENDER, hideModule = false) {

    private val particle by ListValue("Particle",
        arrayOf("None", "Blood", "Lighting", "Fire", "Heart", "Water", "Smoke", "Magic", "Crits"), "Blood")

        private val amount by IntegerValue("ParticleAmount", 5, 1..20) { particle != "None" }

    private val sound by ListValue("Sound", arrayOf("None", "Hit", "Orb", "Pop", "Splash", "Lightning"), "BowHit")

        private val volume by FloatValue("Volume", 1f, 0.1f.. 5f) { sound != "None" }
        private val pitch by FloatValue("Pitch", 1f, 0.1f..5f) { sound != "None" }


    @EventTarget
    fun onAttack(event: AttackEvent) {
        val target = event.targetEntity as? LivingEntity ?: return

        repeat(amount) {
            doEffect(target)
        }

        doSound()
    }

    private fun doSound() {
        val player = mc.player

        when (sound) {
            "Hit" -> player.playSound("random.bowhit", volume, pitch)
            "Orb" -> player.playSound("random.orb", volume, pitch)
            "Pop" -> player.playSound("random.pop", volume, pitch)
            "Splash" -> player.playSound("random.splash", volume, pitch)
            "Lightning" -> player.playSound("ambient.weather.thunder", volume, pitch)
        }
    }

    private fun doEffect(target: LivingEntity) {
        when (particle) {
            "Blood" -> spawnBloodParticle(EnumParticleTypes.BLOCK_CRACK, target)
            "Crits" -> spawnEffectParticle(EnumParticleTypes.CRIT, target)
            "Magic" -> spawnEffectParticle(EnumParticleTypes.CRIT_MAGIC, target)
            "Lighting" -> spawnLightning(target)
            "Smoke" -> spawnEffectParticle(EnumParticleTypes.SMOKE_NORMAL, target)
            "Water" -> spawnEffectParticle(EnumParticleTypes.WATER_DROP, target)
            "Heart" -> spawnEffectParticle(EnumParticleTypes.HEART, target)
            "Fire" -> spawnEffectParticle(EnumParticleTypes.LAVA, target)
        }
    }

    private fun spawnBloodParticle(particleType: EnumParticleTypes, target: LivingEntity) {
        mc.world.spawnParticle(particleType,
            target.posX, target.posY + target.height - 0.75, target.posZ,
            0.0, 0.0, 0.0,
            Block.getStateId(Blocks.redstone_block.defaultState)
        )
    }

    private fun spawnEffectParticle(particleType: EnumParticleTypes, target: LivingEntity) {
        mc.effectRenderer.spawnEffectParticle(particleType.particleID,
            target.posX, target.posY, target.posZ,
            target.posX, target.posY, target.posZ
        )
    }

    private fun spawnLightning(target: LivingEntity) {
        mc.netHandler.handleSpawnGlobalEntity(EntitySpawnGlobalS2CPacket(
            EntityLightningBolt(mc.world, target.posX, target.posY, target.posZ)
        ))
    }

}