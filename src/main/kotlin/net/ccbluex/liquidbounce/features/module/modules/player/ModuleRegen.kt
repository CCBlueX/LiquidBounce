package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object ModuleRegen : Module("Regen", Category.PLAYER) {
    private val modes = choices("Mode", Vanilla, arrayOf(Vanilla, Spartan))
    private val health by int("Health", 18, 0..20)
    private val food by int("Food", 18, 0..20)
    private val speed by int("Speed", 100, 1..100)
    private val noAir by boolean("NoAir", false)
    private val potionEffect by boolean("PotionEffect", false)

    private object Vanilla : Choice("Vanilla") {
        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {

            if ((!noAir && player.isOnGround) && !player.abilities.creativeMode && player.hungerManager.foodLevel < food || (player.health >= 0 && player.health < health * 2)) {
                if (potionEffect && !player.hasStatusEffect(StatusEffects.REGENERATION)) {
                    return@repeatable
                }

                repeat(speed) {
                    network.sendPacket(PlayerMoveC2SPacket(player.isOnGround))
                }
            }
        }
    }

    private object Spartan : Choice("Spartan") {
        override val parent: ChoiceConfigurable
            get() = modes

        var resetTimer = false

        override fun disable() {
            mc.timer.timerSpeed = 1F
            resetTimer = false
        }

        val repeatable = repeatable {
            if (resetTimer) {
                mc.timer.timerSpeed = 1F
                resetTimer = false
            }

            if ((!noAir && player.isOnGround) && !player.abilities.creativeMode && (player.health >= 0 && player.health < health * 2)) {
                if (potionEffect && !player.hasStatusEffect(StatusEffects.REGENERATION)) {
                    return@repeatable
                }

                if (player.moving || player.hungerManager.foodLevel == 0) {
                    return@repeatable
                }

                repeat(9) {
                    network.sendPacket(PlayerMoveC2SPacket(player.isOnGround))
                }

                mc.timer.timerSpeed = 0.45F
                resetTimer = true
            }
        }
    }

}
