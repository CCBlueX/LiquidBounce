package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.sound.SoundPlayer
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Block
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity
import net.minecraft.util.EnumParticleTypes
import java.util.*

@ModuleInfo(name = "AttackEffects", description = "Show effect when you attack", category = ModuleCategory.RENDER)
class AttackEffects : Module() {
    val amount = IntegerValue("Amount", 5, 1, 20)

    private val volume = FloatValue("Volume", 20f, 0f, 100f)
    var target: EntityLivingBase? = null


    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is EntityLivingBase) target = event.targetEntity
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.PRE) {
            if (target != null && target!!.hurtTime >= 3 && mc.thePlayer!!.getDistance(
                    target!!.posX,
                    target!!.posY,
                    target!!.posZ
                ) < 10
            ) {
                if(mc.thePlayer!!.ticksExisted > 4){
                    when(atksound.get().lowercase(Locale.getDefault())) {
                        "knock" ->{

                            SoundPlayer().playSound(SoundPlayer.SoundType.Crack, volume.get())
                        }
                        "skeet" -> {
                            SoundPlayer().playSound(SoundPlayer.SoundType.SKEET, volume.get())
                        }

                        "neko" -> {

                            SoundPlayer().playSound(SoundPlayer.SoundType.NEKO, volume.get())
                        }
                    }
                }
                if (mc.thePlayer!!.ticksExisted > 3) {


                    when (mode.get().lowercase(Locale.getDefault())) {
                        "blood" -> {
                            var i = 0
                            while (i < amount.get()) {
                                mc.theWorld.spawnParticle(
                                    EnumParticleTypes.BLOCK_CRACK,
                                    target!!.posX,
                                    target!!.posY + target!!.height - 0.75,
                                    target!!.posZ,
                                    0.0,
                                    0.0,
                                    0.0,
                                    Block.getStateId(Blocks.redstone_block.defaultState)
                                )
                                i++
                            }
                        }

                        "criticals" -> {
                            var i = 0
                            while (i < amount.get()) {
                                mc.effectRenderer.spawnEffectParticle( EnumParticleTypes.CRIT.particleID,target!!.posX, target!!.posY, target!!.posZ, target!!.posX, target!!.posY, target!!.posZ)
                                i++
                            }
                        }
                        "magic" -> {
                            var i = 0
                            while (i < amount.get()) {
                                mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.CRIT_MAGIC.particleID, target!!.posX, target!!.posY, target!!.posZ, target!!.posX, target!!.posY, target!!.posZ)
                                i++
                            }
                        }
                        "lighting" -> {
                            mc.netHandler.handleSpawnGlobalEntity(
                                S2CPacketSpawnGlobalEntity(
                                EntityLightningBolt(mc.theWorld,
                                target!!.posX, target!!.posY, target!!.posZ)
                            )
                            )
                        }
                        "smoke"-> mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.SMOKE_NORMAL.particleID, target!!.posX, target!!.posY, target!!.posZ, target!!.posX, target!!.posY, target!!.posZ)
                        "water" -> mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.WATER_DROP.particleID, target!!.posX, target!!.posY, target!!.posZ, target!!.posX, target!!.posY, target!!.posZ)
                        "heart" -> mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.HEART.particleID, target!!.posX, target!!.posY, target!!.posZ, target!!.posX, target!!.posY, target!!.posZ)
                        "fire" -> mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.LAVA.particleID,target!!.posX, target!!.posY, target!!.posZ, target!!.posX, target!!.posY, target!!.posZ)

                    }
                }
                target = null
            }
        }
    }





    companion object {
        val  atksound = ListValue("AttackSound", arrayOf(
            "None",
            "Skeet",
            "Neko",
            "Knock"

        ), "None")
        val mode = ListValue(
            "Mode", arrayOf(
                "Blood",
                "Lighting",
                "Fire",
                "Heart",
                "Water"

            ), "Blood"
        )
    }
}