/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Criticals : Module("Criticals", Category.COMBAT, hideModule = false) {

    val mode by ListValue(
        "Mode",
        arrayOf("Packet", "NCPPacket", "BlocksMC", "BlocksMC2", "NoGround", "Hop", "TPHop", "Jump", "LowJump", "CustomMotion", "Visual"),
        "Packet"
    )

    val delay by IntegerValue("Delay", 0, 0..500)
    private val hurtTime by IntegerValue("HurtTime", 10, 0..10)
    private val customMotionY by FloatValue("Custom-Y", 0.2f, 0.01f..0.42f) { mode == "CustomMotion" }

    val msTimer = MSTimer()

    override fun onEnable() {
        if (mode == "NoGround")
            player.tryJump()
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is EntityLivingBase) {
            player ?: return
            val entity = event.targetEntity

            if (!player.onGround || player.isOnLadder || player.isInWeb || player.isInWater ||
                player.isInLava || player.ridingEntity != null || entity.hurtTime > hurtTime ||
                Fly.handleEvents() || !msTimer.hasTimePassed(delay)
            )
                return

            val (x, y, z) = player

            when (mode.lowercase()) {
                "packet" -> {
                    sendPackets(
                        C04PacketPlayerPosition(x, y + 0.0625, z, true),
                        C04PacketPlayerPosition(x, y, z, false)
                    )
                    player.onCriticalHit(entity)
                }

                "ncppacket" -> {
                    sendPackets(
                        C04PacketPlayerPosition(x, y + 0.11, z, false),
                        C04PacketPlayerPosition(x, y + 0.1100013579, z, false),
                        C04PacketPlayerPosition(x, y + 0.0000013579, z, false)
                    )
                    player.onCriticalHit(entity)
                }

                "blocksmc" -> {
                    sendPackets(
                        C04PacketPlayerPosition(x, y + 0.001091981, z, true),
                        C04PacketPlayerPosition(x, y, z, false)
                    )
                }

                "blocksmc2" -> {
                    if (player.ticksExisted % 4 == 0) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.0011, z, true),
                            C04PacketPlayerPosition(x, y, z, false)
                        )
                    }
                }

                "hop" -> {
                    player.motionY = 0.1
                    player.fallDistance = 0.1f
                    player.onGround = false
                }

                "tphop" -> {
                    sendPackets(
                        C04PacketPlayerPosition(x, y + 0.02, z, false),
                        C04PacketPlayerPosition(x, y + 0.01, z, false)
                    )
                    player.setPosition(x, y + 0.01, z)
                }

                "jump" -> player.motionY = 0.42
                "lowjump" -> player.motionY = 0.3425
                "custommotion" -> player.motionY = customMotionY.toDouble()
                "visual" -> player.onCriticalHit(entity)
            }

            msTimer.reset()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer && mode == "NoGround")
            packet.onGround = false
    }

    override val tag
        get() = mode
}
