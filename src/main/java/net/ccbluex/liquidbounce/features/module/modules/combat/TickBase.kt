/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.utils.ClientUtils.runTimeTicks
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.LivingEntity
import net.minecraft.network.Packet
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object TickBase : Module("TickBase", Category.COMBAT) {

    private val mode by ListValue("Mode", arrayOf("Past", "Future"), "Past")
    private val onlyOnKillAura by BoolValue("OnlyOnKillAura", true)

    private val change by IntegerValue("Changes", 100, 0..100)

    private val balanceMaxValue by IntegerValue("BalanceMaxValue", 100, 1..1000)
    private val balanceRecoveryIncrement by FloatValue("BalanceRecoveryIncrement", 0.1f, 0.01f..10f)
    private val maxTicksAtATime by IntegerValue("MaxTicksAtATime", 20, 1..100)

    private val maxRangeToAttack: FloatValue = object : FloatValue("MaxRangeToAttack", 5.0f, 0f..10f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minRangeToAttack.get())
    }
    private val minRangeToAttack: FloatValue = object : FloatValue("MinRangeToAttack", 3.0f, 0f..10f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxRangeToAttack.get())
    }

    private val forceGround by BoolValue("ForceGround", false)
    private val pauseAfterTick by IntegerValue("PauseAfterTick", 0, 0..100)
    private val pauseOnFlag by BoolValue("PauseOnFlag", true)
    private val experimentalPacketCancel by BoolValue("ExperimentalPacketCancel", false)

    private val line by BoolValue("Line", true, subjective = true)
    private val rainbow by BoolValue("Rainbow", false, subjective = true) { line }
    private val red by IntegerValue(
        "R",
        0,
        0..255,
        subjective = true
    ) { !rainbow && line }
    private val green by IntegerValue(
        "G",
        255,
        0..255,
        subjective = true
    ) { !rainbow && line }
    private val blue by IntegerValue(
        "B",
        0,
        0..255,
        subjective = true
    ) { !rainbow && line }

    private var ticksToSkip = 0
    private var tickBalance = 0f
    private var reachedTheLimit = false
    private val tickBuffer = mutableListOf<TickData>()
    private var duringTickModification = false
    private val packetMap: MutableMap<Int, MutableList<Packet<*>>> = mutableMapOf()

    override val tag
        get() = mode

    override fun onToggle(state: Boolean) {
        duringTickModification = false
    }

    @EventTarget
    fun onTickPre(event: PlayerTickEvent) {
        val player = mc.player ?: return

        if (player.vehicle != null || Blink.handleEvents()) {
            return
        }

        if (event.state == EventState.PRE && ticksToSkip-- > 0) {
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onTickPost(event: PlayerTickEvent) {
        val player = mc.player ?: return

        if (player.vehicle != null || Blink.handleEvents()) {
            return
        }

        if (event.state == EventState.POST && !duringTickModification && tickBuffer.isNotEmpty()) {
            val nearbyEnemy = getNearestEntityInRange() ?: return
            val currentDistance = player.positionVector.squareDistanceTo(nearbyEnemy.positionVector)

            val possibleTicks = tickBuffer
                .mapIndexed { index, tick -> index to tick }
                .filter { (_, tick) ->
                    tick.position.distanceTo(nearbyEnemy.positionVector) < currentDistance &&
                            tick.position.distanceTo(nearbyEnemy.positionVector) in minRangeToAttack.get()..maxRangeToAttack.get()
                }
                .filter { (_, tick) -> !forceGround || tick.onGround }

            val criticalTick = possibleTicks
                .filter { (_, tick) -> tick.fallDistance > 0.0f }
                .minByOrNull { (index, _) -> index }

            val (bestTick, _) = criticalTick ?: possibleTicks.minByOrNull { (index, _) -> index } ?: return

            if (bestTick == 0) return

            if (RandomUtils.nextInt(endExclusive = 100) > change || (onlyOnKillAura && (!KillAura.state || KillAura.target == null))) {
                return
            }

            packetMap.clear()
            duringTickModification = true

            if (mode == "Past") {
                ticksToSkip = (bestTick + pauseAfterTick).coerceAtMost(maxTicksAtATime + pauseAfterTick)

                WaitTickUtils.scheduleTicks(ticksToSkip) {
                    repeat(bestTick) {
                        if (experimentalPacketCancel) {
                            val zeroIndex = runTimeTicks - bestTick
                            val index = runTimeTicks - (bestTick - it)

                            if (it == 1) {
                                packetMap[zeroIndex]?.forEach { packet ->
                                    sendPacket(packet, false)
                                }
                            }
                            packetMap[index]?.forEach { packet ->
                                sendPacket(packet, false)
                            }
                        }
                        player.onUpdate()
                        tickBalance -= 1
                    }

                    packetMap.clear()
                    duringTickModification = false
                }
            } else {
                val skipTicks = (bestTick + pauseAfterTick).coerceAtMost(maxTicksAtATime + pauseAfterTick)

                repeat(skipTicks) {
                    player.onUpdate()
                    tickBalance -= 1
                }

                ticksToSkip = skipTicks

                WaitTickUtils.scheduleTicks(ticksToSkip) {
                    if (experimentalPacketCancel) {
                        repeat(skipTicks) {
                            val zeroIndex = runTimeTicks - skipTicks
                            val index = runTimeTicks - (skipTicks - it)
                            if (it == 1) {
                                packetMap[zeroIndex]?.forEach { packet ->
                                    sendPacket(packet, false)
                                }
                            }
                            packetMap[index]?.forEach { packet ->
                                sendPacket(packet, false)
                            }
                        }
                    }

                    packetMap.clear()
                    duringTickModification = false
                }
            }
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (mc.player?.vehicle != null || Blink.handleEvents()) {
            return
        }

        tickBuffer.clear()

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(mc.player.input)

        if (tickBalance <= 0) {
            reachedTheLimit = true
        }
        if (tickBalance > balanceMaxValue / 2) {
            reachedTheLimit = false
        }
        if (tickBalance <= balanceMaxValue) {
            tickBalance += balanceRecoveryIncrement
        }

        if (reachedTheLimit) return

        repeat(minOf(tickBalance.toInt(), maxTicksAtATime * if (mode == "Past") 2 else 1)) {
            simulatedPlayer.tick()
            tickBuffer += TickData(
                simulatedPlayer.pos,
                simulatedPlayer.fallDistance,
                simulatedPlayer.velocityX,
                simulatedPlayer.velocityY,
                simulatedPlayer.velocityZ,
                simulatedPlayer.onGround
            )
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!line) return

        val color = if (rainbow) rainbow() else Color(
            red,
            green,
            blue
        )

        synchronized(tickBuffer) {
            glPushMatrix()
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)
            mc.entityRenderDispatcher.disableLightmap()
            glBegin(GL_LINE_STRIP)
            glColor(color)

            val cameraX = mc.entityRenderManager.viewerPosX
            val cameraY = mc.entityRenderManager.viewerPosY
            val cameraZ = mc.entityRenderManager.viewerPosZ

            for (tick in tickBuffer) {
                glVertex3d(
                    tick.position.x - cameraX,
                    tick.position.y - cameraY,
                    tick.position.z - cameraZ
                )
            }

            glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
            glEnd()
            glEnable(GL_DEPTH_TEST)
            glDisable(GL_LINE_SMOOTH)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            glPopMatrix()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is PlayerPositionLookS2CPacket && pauseOnFlag) {
            tickBalance = 0f
        }

        if (event.eventType == EventState.SEND && ticksToSkip > 0 && experimentalPacketCancel) {
            event.cancelEvent()
            packetMap.getOrPut(runTimeTicks) { mutableListOf() }.add(event.packet)
        }

    }

    private data class TickData(
        val position: Vec3d,
        val fallDistance: Float,
        val velocityX: Double,
        val velocityY: Double,
        val velocityZ: Double,
        val onGround: Boolean,
    )

    private fun getNearestEntityInRange(): LivingEntity? {
        val player = mc.player ?: return null

        return mc.world?.entities
            ?.filterIsInstance<LivingEntity>()
            ?.filter { EntityUtils.isSelected(it, true) }
            ?.minByOrNull { player.distanceTo(it) }
    }
}
