package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Block
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity
import net.minecraft.util.EnumParticleTypes

object AttackEffects : Module("AttackEffects", ModuleCategory.RENDER) {

    private val particle by ListValue(
        "Particle", arrayOf(
            "None",
            "Blood",
            "Lighting",
            "Fire",
            "Heart",
            "Water",
            "Smoke",
            "Magic",
            "Crits"
        ), "Blood"
    )

    private val amount by IntegerValue("ParticleAmount", 5, 1..20) { particle != "None" }

    private val sound by ListValue("Sound", arrayOf("None", "Hit", "Orb"), "Hit")


    @EventTarget
    fun onAttack(event: AttackEvent) {
        val target = event.targetEntity

        if (target is EntityLivingBase) {
            repeat(amount) {
                doEffect(target)
            }

            doSound()
        }
    }

    private fun doSound() {
        when (sound) {
            "Hit" -> mc.thePlayer.playSound("random.bowhit", 1f, 1f)
            "Orb" -> mc.thePlayer.playSound("random.orb", 1f, 1f)
        }
    }

    private fun doEffect(target: EntityLivingBase) {
        when (particle) {
            "Blood" -> mc.theWorld.spawnParticle(
                EnumParticleTypes.BLOCK_CRACK,
                target.posX,
                target.posY + target.height - 0.75,
                target.posZ,
                0.0,
                0.0,
                0.0,
                Block.getStateId(Blocks.redstone_block.defaultState)
            )
            "Crits" -> mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.CRIT.particleID, target.posX, target.posY, target.posZ, target.posX, target.posY, target.posZ)
            "Magic" -> mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.CRIT_MAGIC.particleID, target.posX, target.posY, target.posZ, target.posX, target.posY, target.posZ)
            "Lighting" -> mc.netHandler.handleSpawnGlobalEntity(S2CPacketSpawnGlobalEntity(EntityLightningBolt(mc.theWorld, target.posX, target.posY, target.posZ)))
            "Smoke"-> mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.SMOKE_NORMAL.particleID, target.posX, target.posY, target.posZ, target.posX, target.posY, target.posZ)
            "Water" -> mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.WATER_DROP.particleID, target.posX, target.posY, target.posZ, target.posX, target.posY, target.posZ)
            "Heart" -> mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.HEART.particleID, target.posX, target.posY, target.posZ, target.posX, target.posY, target.posZ)
            "Fire" -> mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.LAVA.particleID,
                target.posX, target.posY, target.posZ, target.posX, target.posY, target.posZ)
        }
    }

}