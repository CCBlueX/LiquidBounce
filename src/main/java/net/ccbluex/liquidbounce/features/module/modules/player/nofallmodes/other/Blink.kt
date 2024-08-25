/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.autoOff
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.checkFallDist
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
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBacktrackBox
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Box
import java.awt.Color

object Blink : NoFallMode("Blink") {
    private var blinked = false

    private val tick = TickTimer()

    override fun onDisable() {
        BlinkUtils.unblink()
        blinked = false
        tick.reset()
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        val thePlayer = mc.player ?: return

        if (thePlayer.isDead)
            return

        val simPlayer = SimulatedPlayer.fromClientPlayer(thePlayer.movementInput)

        simPlayer.tick()

        if (simPlayer.onGround && blinked) {
            if (thePlayer.onGround) {
                tick.update()

                if (tick.hasTimePassed(100)) {
                    BlinkUtils.unblink()
                    blinked = false
                    Chat.print("Unblink")

                    if (autoOff) {
                        state = false
                    }
                    tick.reset()
                }
            }
        }

        if (event.packet is PlayerMoveC2SPacket) {
            if (blinked && thePlayer.fallDistance > minFallDist.get()) {
                if (thePlayer.fallDistance < maxFallDist.get()) {
                    if (blinked) {
                        event.packet.onGround = thePlayer.ticksAlive % 2 == 0
                    }
                } else {
                    Chat.print("rewriting ground")
                    BlinkUtils.unblink()
                    blinked = false
                    event.packet.onGround = false
                }
            }
        }

        // Re-check #1
        repeat(2) {
            simPlayer.tick()
        }

        if (simPlayer.isClimbing() || simPlayer.inWater || simPlayer.isTouchingLava() || simPlayer.isInWeb() || simPlayer.isCollided)
            return

        if (thePlayer.velocityY > 0 && blinked)
            return

        if (simPlayer.onGround)
            return

        // Re-check #2
        if (checkFallDist) {
            repeat(6) {
                simPlayer.tick()
            }
        }

        val fallingPlayer = FallingPlayer(thePlayer)

        if ((checkFallDist && simPlayer.fallDistance > minFallDist.get()) ||
            !checkFallDist && fallingPlayer.findCollision(60) != null && simPlayer.velocityY < 0) {
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

        val thePlayer = mc.player ?: return

        val simPlayer = SimulatedPlayer.fromClientPlayer(thePlayer.movementInput)

        repeat(4) {
            simPlayer.tick()
        }

        thePlayer.run {
            val targetEntity = thePlayer as IMixinEntity

            if (targetEntity.truePos) {

                val x =
                    simplayer.x - mc.renderManager.renderPosX
                val y =
                    simplayer.y - mc.renderManager.renderPosY
                val z =
                    simplayer.z - mc.renderManager.renderPosZ

                val Box = entityBoundingBox.offset(-posX, -posY, -posZ).offset(x, y, z)

                drawBacktrackBox(
                    Box.fromBounds(
                        Box.minX,
                        Box.minY,
                        Box.minZ,
                        Box.maxX,
                        Box.maxY,
                        Box.maxZ
                    ), Color.BLUE
                )
            }
        }
    }
}