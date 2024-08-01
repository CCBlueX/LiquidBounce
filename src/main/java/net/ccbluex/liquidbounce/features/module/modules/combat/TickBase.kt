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
import net.ccbluex.liquidbounce.features.module.modules.render.Breadcrumbs
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object TickBase : Module("TickBase", Category.COMBAT) {

    private val balanceMaxValue by IntegerValue("BalanceMaxValue", 100, 1..1000)
    private val balanceRecoveryIncrement by FloatValue("BalanceRecoveryIncrement", 0.1f, 0.01f..10f)
    private val maxTicksAtATime by IntegerValue("MaxTicksAtATime", 20, 1..100)
    private val rangeToAttack by FloatValue("RangeToAttack", 3.0f, 0.1f..10f)
    private val forceGround by BoolValue("ForceGround", false)
    private val pauseAfterTick by IntegerValue("PauseAfterTick", 0, 0..100)
    private val pauseOnFlag by BoolValue("PauseOnFlag", true)

    private var ticksToSkip = 0
    private var tickBalance = 0f
    private var reachedTheLimit = false
    private val tickBuffer = mutableListOf<TickData>()
    private var duringTickModification = false

    override fun onToggle(state: Boolean) {
        duringTickModification = false
    }

    @EventTarget
    fun onTickPre(event: PlayerTickEvent) {
        val player = mc.thePlayer ?: return

        if (player.ridingEntity != null || Blink.handleEvents()) {
            return
        }

        if (event.state == EventState.PRE && ticksToSkip-- > 0) {
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onTickPost(event: PlayerTickEvent) {
        val player = mc.thePlayer ?: return

        if (player.ridingEntity != null || Blink.handleEvents()) {
            return
        }

        if (event.state == EventState.POST && !duringTickModification && tickBuffer.isNotEmpty()) {
            val nearbyEnemy = getNearestEntityInRange() ?: return
            val currentDistance = player.positionVector.squareDistanceTo(nearbyEnemy.positionVector)

            val possibleTicks = tickBuffer
                .mapIndexed { index, tick -> index to tick }
                .filter { (_, tick) ->
                    tick.position.squareDistanceTo(nearbyEnemy.positionVector) < currentDistance &&
                        tick.position.squareDistanceTo(nearbyEnemy.positionVector) in 0f..rangeToAttack
                }
                .filter { (_, tick) -> !forceGround || tick.onGround }

            val criticalTick = possibleTicks
                .filter { (_, tick) -> tick.fallDistance > 0.0f }
                .minByOrNull { (index, _) -> index }

            val (bestTick, _) = criticalTick ?: possibleTicks.minByOrNull { (index, _) -> index } ?: return

            if (bestTick == 0) return

            duringTickModification = true

            ticksToSkip = bestTick + pauseAfterTick

            WaitTickUtils.scheduleTicks(ticksToSkip) {
                repeat(bestTick) {
                    player.onUpdate()
                    tickBalance -= 1
                }

                duringTickModification = false
            }
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (mc.thePlayer?.ridingEntity != null || Blink.handleEvents()) {
            return
        }

        tickBuffer.clear()

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput)

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

        repeat(minOf(tickBalance.toInt(), maxTicksAtATime)) {
            simulatedPlayer.tick()
            tickBuffer += TickData(
                simulatedPlayer.pos,
                simulatedPlayer.fallDistance,
                simulatedPlayer.motionX,
                simulatedPlayer.motionY,
                simulatedPlayer.motionZ,
                simulatedPlayer.onGround
            )
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val color = if (Breadcrumbs.colorRainbow) rainbow() else Color(Breadcrumbs.colorRed,
            Breadcrumbs.colorGreen,
            Breadcrumbs.colorBlue
        )

        synchronized(tickBuffer) {
            glPushMatrix()
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)
            mc.entityRenderer.disableLightmap()
            glBegin(GL_LINE_STRIP)
            glColor(color)

            val renderPosX = mc.renderManager.viewerPosX
            val renderPosY = mc.renderManager.viewerPosY
            val renderPosZ = mc.renderManager.viewerPosZ

            for (tick in tickBuffer) {
                glVertex3d(tick.position.xCoord - renderPosX,
                    tick.position.yCoord - renderPosY,
                    tick.position.zCoord - renderPosZ
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
        if (event.packet is S08PacketPlayerPosLook && pauseOnFlag) {
            tickBalance = 0f
        }
    }

    private data class TickData(
        val position: Vec3,
        val fallDistance: Float,
        val motionX: Double,
        val motionY: Double,
        val motionZ: Double,
        val onGround: Boolean,
    )

    private fun getNearestEntityInRange(): EntityLivingBase? {
        val player = mc.thePlayer ?: return null

        return mc.theWorld?.loadedEntityList
            ?.filterIsInstance<EntityLivingBase>()
            ?.filter { EntityUtils.isSelected(it, true) }
            ?.minByOrNull { player.getDistanceToEntity(it) }
    }
}