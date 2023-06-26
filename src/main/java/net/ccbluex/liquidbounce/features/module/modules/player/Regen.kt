/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.potion.Potion

object Regen : Module("Regen", ModuleCategory.PLAYER) {

    private val mode by ListValue("Mode", arrayOf("Vanilla", "Spartan"), "Vanilla")
    private val delay by IntegerValue("Delay", 0, 0..10000)
    private val speed by IntegerValue("Speed", 100, 1..100) { mode == "Vanilla" }
    private val health by IntegerValue("Health", 18, 0..20)
    private val food by IntegerValue("Food", 18, 0..20)
    private val noAir by BoolValue("NoAir", false)
    private val potionEffect by BoolValue("PotionEffect", false)

    private val timer = MSTimer()

    private var resetTimer = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (resetTimer) {
            mc.timer.timerSpeed = 1F
        } else {
            resetTimer = false
        }

        if ((!noAir || mc.thePlayer.onGround) && !mc.thePlayer.capabilities.isCreativeMode && mc.thePlayer.foodStats.foodLevel > food && mc.thePlayer.isEntityAlive && mc.thePlayer.health < health) {
            if (potionEffect && !mc.thePlayer.isPotionActive(Potion.regeneration)) {
                return
            }

            if (timer.hasTimePassed(delay)) {
                when (mode.lowercase()) {
                    "vanilla" -> {
                        repeat(speed) {
                            sendPacket(C03PacketPlayer(mc.thePlayer.onGround))
                        }
                    }

                    "spartan" -> {
                        if (!isMoving && mc.thePlayer.onGround) {
                            repeat(9) {
                                sendPacket(C03PacketPlayer(mc.thePlayer.onGround))
                            }

                            mc.timer.timerSpeed = 0.45F
                            resetTimer = true
                        }
                    }
                }

                timer.reset()
            }
        }
    }
}
