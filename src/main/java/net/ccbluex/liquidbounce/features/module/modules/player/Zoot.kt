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
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect

// TODO: Max packets per tick limit
@ModuleInfo(name = "Zoot", description = "Removes all bad potion effects/fire.", category = ModuleCategory.PLAYER)
class Zoot : Module()
{
    private val badEffectsValue = BoolValue("BadEffects", true)
    private val fireValue = BoolValue("Fire", true)
    private val noAirValue = BoolValue("NoAir", false)
    private val noMoveValue = BoolValue("NoMove", false)

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (noMoveValue.get() && thePlayer.isMoving) return

        val onGround = thePlayer.onGround

        if (noAirValue.get() && !onGround) return

        val netHandler = mc.netHandler

        if (badEffectsValue.get()) thePlayer.activePotionEffects.filter { isBadEffect(it.potionID) }.maxByOrNull(PotionEffect::getDuration)?.let { effect -> runAsync { repeat(effect.duration / 20) { netHandler.addToSendQueue(C03PacketPlayer(onGround)) } } }

        if (fireValue.get() && !thePlayer.capabilities.isCreativeMode && thePlayer.isBurning) runAsync { repeat(9) { netHandler.addToSendQueue(C03PacketPlayer(onGround)) } }
    }

    companion object
    {
        val badEffectsArray = arrayListOf<Int>()

        init
        {
            badEffectsArray.add(Potion.hunger.id)
            badEffectsArray.add(Potion.moveSlowdown.id)
            badEffectsArray.add(Potion.digSlowdown.id)
            badEffectsArray.add(Potion.harm.id)
            badEffectsArray.add(Potion.confusion.id)
            badEffectsArray.add(Potion.blindness.id)
            badEffectsArray.add(Potion.weakness.id)
            badEffectsArray.add(Potion.wither.id)
            badEffectsArray.add(Potion.poison.id)
        }

        fun isBadEffect(potionID: Int): Boolean = badEffectsArray.any { potionID == it }
    }
}
