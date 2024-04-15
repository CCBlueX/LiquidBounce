package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.autoOff
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.fakePlayer
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.maxFallDist
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.minFallDist
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.simulateDebug
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.state
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBacktrackBox
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB
import java.awt.Color

object HypixelBlink : NoFallMode("HypixelBlink") {
    private var blinked = false

    override fun onDisable() {
        BlinkUtils.unblink()
        blinked = false
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.isDead)
            return

        val simPlayer = SimulatedPlayer.fromClientPlayer(thePlayer.movementInput)

        repeat(6) {
            simPlayer.tick()
        }

        if (thePlayer.onGround && simPlayer.onGround && blinked) {
            BlinkUtils.unblink()
            blinked = false

            if (autoOff) {
                state = false
            }
            Chat.print("Unblink")
        }

        if (event.packet is C03PacketPlayer) {
            if (blinked && thePlayer.fallDistance > minFallDist.get()) {
                if (thePlayer.fallDistance < maxFallDist.get()) {
                    if (blinked) {
                        event.packet.onGround = thePlayer.ticksExisted % 2 == 0
                    }
                } else {
                    Chat.print("rewriting ground")
                    BlinkUtils.unblink()
                    blinked = false
                    event.packet.onGround = false
                }
            }
        }

        if (simPlayer.isOnLadder() || simPlayer.inWater || simPlayer.isInLava() || simPlayer.isInWeb || simPlayer.isCollided)
            return

        if (thePlayer.motionY > 0 && blinked)
            return

        // Re-check
        repeat(4) {
            simPlayer.tick()
        }

        if (!simPlayer.onGround && simPlayer.fallDistance > minFallDist.get()) {
            if (thePlayer.onGround && !blinked) {
                blinked = true

                if (fakePlayer)
                    BlinkUtils.addFakePlayer()

                Chat.print("Blinked")
                BlinkUtils.blink(packet, event)
            }
        }
    }

    @EventTarget
    override fun onRender3D(event: Render3DEvent) {
        if (!simulateDebug) return

        val thePlayer = mc.thePlayer ?: return

        val simPlayer = SimulatedPlayer.fromClientPlayer(thePlayer.movementInput)

        repeat(4) {
            simPlayer.tick()
        }

        thePlayer.run {
            val targetEntity = thePlayer as IMixinEntity

            if (targetEntity.truePos) {

                val x =
                    simPlayer.posX - mc.renderManager.renderPosX
                val y =
                    simPlayer.posY - mc.renderManager.renderPosY
                val z =
                    simPlayer.posZ - mc.renderManager.renderPosZ

                val axisAlignedBB = entityBoundingBox.offset(-posX, -posY, -posZ).offset(x, y, z)

                drawBacktrackBox(
                    AxisAlignedBB.fromBounds(
                        axisAlignedBB.minX,
                        axisAlignedBB.minY,
                        axisAlignedBB.minZ,
                        axisAlignedBB.maxX,
                        axisAlignedBB.maxY,
                        axisAlignedBB.maxZ
                    ), Color.BLUE
                )
            }
        }
    }
}