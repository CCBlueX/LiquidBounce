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
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

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
            mc.player.tryJump()
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is LivingEntity) {
            val thePlayer = mc.player ?: return
            val entity = event.targetEntity

            if (!thePlayer.onGround || thePlayer.isClimbing || thePlayer.isInWeb() || thePlayer.isTouchingWater ||
                thePlayer.isTouchingLava || thePlayer.rider != null || entity.hurtTime > hurtTime ||
                Fly.handleEvents() || !msTimer.hasTimePassed(delay)
            )
                return

            val (x, y, z) = thePlayer

            when (mode.lowercase()) {
                "packet" -> {
                    sendPackets(
                        PositionOnly(x, y + 0.0625, z, true),
                        PositionOnly(x, y, z, false)
                    )
                    thePlayer.addCritParticles(entity)
                }

                "ncppacket" -> {
                    sendPackets(
                        PositionOnly(x, y + 0.11, z, false),
                        PositionOnly(x, y + 0.1100013579, z, false),
                        PositionOnly(x, y + 0.0000013579, z, false)
                    )
                    mc.player.addCritParticles(entity)
                }

                "blocksmc" -> {
                    sendPackets(
                        PositionOnly(x, y + 0.001091981, z, true),
                        PositionOnly(x, y, z, false)
                    )
                }

                "blocksmc2" -> {
                    if (thePlayer.ticksAlive % 4 == 0) {
                        sendPackets(
                            PositionOnly(x, y + 0.0011, z, true),
                            PositionOnly(x, y, z, false)
                        )
                    }
                }

                "hop" -> {
                    thePlayer.velocityY = 0.1
                    thePlayer.fallDistance = 0.1f
                    thePlayer.onGround = false
                }

                "tphop" -> {
                    sendPackets(
                        PositionOnly(x, y + 0.02, z, false),
                        PositionOnly(x, y + 0.01, z, false)
                    )
                    thePlayer.updatePosition(x, y + 0.01, z)
                }

                "jump" -> thePlayer.velocityY = 0.42
                "lowjump" -> thePlayer.velocityY = 0.3425
                "custommotion" -> thePlayer.velocityY = customMotionY.toDouble()
                "visual" -> thePlayer.addCritParticles(entity)
            }

            msTimer.reset()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is PlayerMoveC2SPacket && mode == "NoGround")
            packet.onGround = false
    }

    override val tag
        get() = mode
}
