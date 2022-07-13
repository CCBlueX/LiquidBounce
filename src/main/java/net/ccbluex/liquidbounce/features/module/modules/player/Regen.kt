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
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.runAsync
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.potion.Potion

@ModuleInfo(name = "Regen", description = "Regenerates your health much faster.", category = ModuleCategory.PLAYER)
class Regen : Module()
{

    private val modeValue = ListValue("Mode", arrayOf("Vanilla", "Spartan"), "Vanilla")
    private val healthValue = IntegerValue("Health", 18, 0, 20)
    private val foodValue = IntegerValue("Food", 18, 0, 20)
    private val speedValue = IntegerValue("Speed", 100, 1, 100)
    private val noAirValue = BoolValue("NoAir", false)
    private val potionEffectValue = BoolValue("PotionEffect", false)

    private var resetTimer = false

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val timer = mc.timer

        if (resetTimer) timer.timerSpeed = 1F
        resetTimer = false

        val thePlayer = mc.thePlayer ?: return
        val netHandler = mc.netHandler
        val onGround = thePlayer.onGround

        if ((!noAirValue.get() || onGround) && !thePlayer.capabilities.isCreativeMode && thePlayer.foodStats.foodLevel > foodValue.get() && thePlayer.isEntityAlive && thePlayer.health < healthValue.get())
        {
            if (potionEffectValue.get() && !thePlayer.isPotionActive(Potion.regeneration)) return

            when (modeValue.get().lowercase())
            {
                "vanilla" -> runAsync { repeat(speedValue.get()) { netHandler.addToSendQueue(C03PacketPlayer(onGround)) } }

                "spartan" ->
                {
                    if (thePlayer.isMoving || !onGround) return

                    runAsync { repeat(9) { netHandler.addToSendQueue(C03PacketPlayer(true)) } }

                    timer.timerSpeed = 0.45F
                    resetTimer = true
                }
            }
        }
    }

    override val tag: String
        get() = modeValue.get()
}
