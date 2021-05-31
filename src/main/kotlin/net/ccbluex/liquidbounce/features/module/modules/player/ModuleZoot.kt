package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket

object ModuleZoot : Module("Zoot", Category.PLAYER) {

    val badEffects by boolean("BadEffects", true)
    val fire by boolean("Fire", true)
    val noAir by boolean("NoAir", false)

    val repeatable = repeatable {
        if (!noAir && player.isOnGround) {
            if (badEffects) {
                val effect = player.activeStatusEffects.maxByOrNull { it.value.duration }

                if (effect != null) {
                    if (!effect.key.isBeneficial) {
                        repeat(effect.value.duration / 20) {
                            network.sendPacket(PlayerMoveC2SPacket(player.isOnGround))
                        }
                    }
                    if (effect.value.isPermanent) {
                        player.statusEffects.remove(effect.value)
                        network.sendPacket(RemoveEntityStatusEffectS2CPacket(player.entityId, effect.key))
                    }
                }
            }
            if (fire && !player.abilities.creativeMode && player.isOnFire) {
                repeat(9) {
                    network.sendPacket(PlayerMoveC2SPacket(player.isOnGround))
                }
            }
        }
    }
}
