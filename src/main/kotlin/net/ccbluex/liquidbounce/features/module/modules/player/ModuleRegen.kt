package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.timer
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object ModuleRegen : Module("Regen", Category.PLAYER) {
    private val health by int("Health", 18, 0..20)
    private val speed by int("Speed", 100, 1..100)
    private val timer by float("Timer", 0.5f, 0.1f..10f)
    private val noAir by boolean("NoAir", false)
    private val potionEffect by boolean("PotionEffect", false)

    override fun disable() {
        mc.timer.timerSpeed = 1f
    }

    val repeatable = repeatable {

        if ((!noAir && player.isOnGround) && !player.abilities.creativeMode && player.health >= 0 && player.health < health) {
            if (potionEffect && !player.hasStatusEffect(StatusEffects.REGENERATION)) {
                return@repeatable
            }

            if(player.hungerManager.foodLevel < 20) {
                return@repeatable
            }

            mc.timer.timerSpeed = timer

            repeat(speed) {
                network.sendPacket(PlayerMoveC2SPacket(player.isOnGround))
            }
        }
    }
}
