package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoWeapon
import net.ccbluex.liquidbounce.utils.extensions.isSelected
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.extensions.raycastEntity
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.pathfinding.PathFinder
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Blocks
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.world.World
import org.lwjgl.opengl.GL11.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@ModuleInfo(name = "ExtendedReach", description = "Upgraded combat and block reach over 100+ blocks.", category = ModuleCategory.PLAYER)
class ExtendedReach : Module()
{
    /**
     * Options
     */
    private val combatReach = FloatValue("CombatReach", 100F, 6F, 128F)

    @JvmField
    val buildReach = FloatValue("BuildReach", 100F, 6F, 128F)
    private val maxDashDistanceValue = IntegerValue("DashDistance", 5, 1, 10)

    private val pathEspGroup = ValueGroup("PathESP")
    private val pathEspEnabledValue = BoolValue("Enabled", true, "PathESP")
    private val pathEspTagValue = BoolValue("Tag", false)
    private val pathEspLineWidthValue = FloatValue("LineWidth", 1f, 0.5f, 2f)
    private val pathEspTimeValue = IntegerValue("KeepLength", 1000, 100, 3000, "PathESPTime")
    private val pathEspColorValue = RGBAColorValue("Color", 255, 179, 72, 255, listOf("PathESP-Red", "PathESP-Green", "PathESP-Blue", "PathESP-Alpha"))

    private val pathEspColorRainbowGroup = ValueGroup("Rainbow")
    private val pathEspColorRainbowEnabledValue = BoolValue("Enabled", false, "PathESP-Rainbow")
    private val pathEspColorRainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "PathESP-RainbowSpeed")
    private val pathEspColorRainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f, "PathESP-RainbowHSB-Saturation")
    private val pathEspColorRainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f, "PathESP-RainbowHSB-Brightness")

    /**
     * Variables
     */
    private val pathESPTimer = MSTimer()
    private var path = mutableListOf<Vec3>()

    init
    {
        pathEspColorRainbowGroup.addAll(pathEspColorRainbowEnabledValue, pathEspColorRainbowSpeedValue, pathEspColorRainbowSaturationValue, pathEspColorRainbowBrightnessValue)
        pathEspGroup.addAll(pathEspEnabledValue, pathEspTagValue, pathEspLineWidthValue, pathEspTimeValue, pathEspColorValue, pathEspColorRainbowGroup)
    }

    override fun onEnable()
    {
        path.clear()
    }

    override fun onDisable()
    {
        path.clear()
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val renderManager = mc.renderManager
        val viewerPosX = renderManager.viewerPosX
        val viewerPosY = renderManager.viewerPosY
        val viewerPosZ = renderManager.viewerPosZ

        if (pathEspEnabledValue.get() && path.isNotEmpty() && !pathESPTimer.hasTimePassed(pathEspTimeValue.get().toLong()))
        {
            val customColor = pathEspColorValue.getColor()
            val color = if (pathEspColorRainbowEnabledValue.get()) ColorUtils.rainbow(pathEspColorValue.getAlpha(), speed = pathEspColorRainbowSpeedValue.get(), saturation = pathEspColorRainbowSaturationValue.get(), brightness = pathEspColorRainbowBrightnessValue.get()) else customColor

            glPushMatrix()
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)
            mc.entityRenderer.disableLightmap()

            RenderUtils.glColor(color)
            glLineWidth(pathEspLineWidthValue.get())
            glBegin(GL_LINE_STRIP)

            for (path in path) glVertex3d(path.xCoord - viewerPosX, path.yCoord - viewerPosY, path.zCoord - viewerPosZ)

            RenderUtils.resetColor()
            glEnd()

            glEnable(GL_DEPTH_TEST)
            glDisable(GL_LINE_SMOOTH)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            glPopMatrix()

            if (pathEspTagValue.get()) path.firstOrNull()?.let { path ->
                this.path.lastOrNull()?.let { lastPath ->
                    RenderUtils.renderNameTag("${StringUtils.DECIMALFORMAT_2.format(Vec3(lastPath.xCoord, lastPath.yCoord, lastPath.zCoord).distanceTo(Vec3(path.xCoord, path.yCoord, path.zCoord)))}m in ${this.path.size} moves", path.xCoord + 0.5, path.yCoord + 0.2, path.zCoord + 0.5)
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return
        val playerPosVec = Vec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)

        val packet = event.packet
        val networkManager = mc.netHandler.networkManager

        if (packet is C08PacketPlayerBlockPlacement)
        {
            val pos = packet.position
            val stack = packet.stack
            val distance = sqrt(thePlayer.getDistanceSq(pos))

            if (distance > 6.0 && pos.y != -1 && (stack != null || theWorld.getBlockState(pos).block is BlockContainer))
            {
                val to = Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                path = computePath(theWorld, playerPosVec, to)

                // Travel to the target block.
                for (pathElm in path) networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
                pathESPTimer.reset()
                networkManager.sendPacketWithoutEvent(packet)

                // Go back to the home.
                path.reverse()
                for (pathElm in path) networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
                event.cancelEvent()
            }
        }

        if (packet is C07PacketPlayerDigging)
        {
            val action = packet.status
            val pos = packet.position
            val face = packet.facing
            val distance = sqrt(thePlayer.getDistanceSq(pos))

            if (distance > 6 && action == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK)
            {
                val to = Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                path = computePath(theWorld, playerPosVec, to)

                // Travel to the target.
                for (pathElm in path) networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
                pathESPTimer.reset()
                val end = C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, face)
                networkManager.sendPacketWithoutEvent(packet)
                networkManager.sendPacketWithoutEvent(end)

                // Go back to the home.
                path.reverse()
                for (pathElm in path) networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
                event.cancelEvent()
            }
            else if (action == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) event.cancelEvent()
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val netHandler = mc.netHandler
        val networkManager = netHandler.networkManager

        if (event.eventState == EventState.PRE)
        {
            val facedEntity = theWorld.raycastEntity(thePlayer, combatReach.get().toDouble(), entityFilter = { entity -> entity is EntityLivingBase })

            var targetEntity: EntityLivingBase? = null
            val from = Vec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)

            if (mc.gameSettings.keyBindAttack.isKeyDown && facedEntity != null && facedEntity.isSelected(true) && thePlayer.getDistanceSqToEntity(facedEntity) >= 1) targetEntity = facedEntity as EntityLivingBase

            if (targetEntity != null)
            {
                val to = Vec3(targetEntity.posX, targetEntity.posY, targetEntity.posZ)

                // Compute the path
                path = computePath(theWorld, from, to)

                // Travel to the target entity.
                for (pathElm in path) networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
                path.reverse()

                LiquidBounce.eventManager.callEvent(AttackEvent(targetEntity, path.firstOrNull() ?: from))

                pathESPTimer.reset()
                thePlayer.swingItem()

                // Make AutoWeapon compatible
                var sendAttack = true
                val attackPacket: Packet<*> = C02PacketUseEntity(targetEntity, C02PacketUseEntity.Action.ATTACK)

                val autoWeapon = LiquidBounce.moduleManager[AutoWeapon::class.java] as AutoWeapon
                if (autoWeapon.state)
                {
                    val packetEvent = PacketEvent(attackPacket)
                    autoWeapon.onPacket(packetEvent)

                    if (packetEvent.isCancelled) sendAttack = false
                }

                if (sendAttack) netHandler.addToSendQueue(attackPacket)

                thePlayer.onCriticalHit(targetEntity)

                // Go back to the home.
                for (pathElm in path) networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
            }
        }
    }

    private fun computePath(theWorld: World, topFrom: Vec3, to: Vec3): MutableList<Vec3>
    {
        var topFromPos = topFrom

        if (!canPassThrough(theWorld, BlockPos(topFromPos.xCoord, topFromPos.yCoord, topFromPos.zCoord))) topFromPos = topFromPos.plus(0.0, 1.0, 0.0)

        val pathfinder = PathFinder(topFromPos, to)
        pathfinder.compute(theWorld)

        var lastPos: Vec3? = null
        var lastDashPos: Vec3? = null
        val path = mutableListOf<Vec3>()
        val pathFinderPath = pathfinder.path

        pathFinderPath.forEachIndexed { i, pathElm ->
            if (i == 0 || i == pathFinderPath.size - 1)
            {
                if (lastPos != null) path.add((lastPos ?: return@forEachIndexed).plus(0.5, 0.0, 0.5))

                path.add(pathElm.plus(0.5, 0.0, 0.5))
                lastDashPos = pathElm
            }
            else
            {
                var stop = false
                val maxDashDistance = maxDashDistanceValue.get().toFloat()
                val lastDashPosChecked = lastDashPos ?: return@forEachIndexed

                if (pathElm.squareDistanceTo(lastDashPosChecked) > maxDashDistance * maxDashDistance) stop = true
                else
                {
                    val minX = min(lastDashPosChecked.xCoord, pathElm.xCoord)
                    val minY = min(lastDashPosChecked.yCoord, pathElm.yCoord)
                    val minZ = min(lastDashPosChecked.zCoord, pathElm.zCoord)
                    val maxX = max(lastDashPosChecked.xCoord, pathElm.xCoord)
                    val maxY = max(lastDashPosChecked.yCoord, pathElm.yCoord)
                    val maxZ = max(lastDashPosChecked.zCoord, pathElm.zCoord)

                    var x = minX.toInt()
                    coordsLoop@ while (x <= maxX)
                    {
                        var y = minY.toInt()
                        while (y <= maxY)
                        {
                            var z = minZ.toInt()
                            while (z <= maxZ)
                            {
                                if (!PathFinder.checkPositionValidity(theWorld, x, y, z, false))
                                {
                                    stop = true
                                    break@coordsLoop
                                }
                                z++
                            }
                            y++
                        }
                        x++
                    }
                }

                if (stop)
                {
                    path.add((lastPos ?: return@forEachIndexed).plus(0.5, 0.0, 0.5))
                    lastDashPos = lastPos
                }
            }

            lastPos = pathElm
        }
        return path
    }

    override val tag: String
        get() = "${maxDashDistanceValue.get()}"

    companion object
    {
        private fun canPassThrough(theWorld: World, pos: BlockPos): Boolean
        {
            val state = theWorld.getBlockState(BlockPos(pos.x, pos.y, pos.z))
            val block = state.block

            return Material.air == block.getMaterial() || Material.plants == block.getMaterial() || Material.vine == block.getMaterial() || Blocks.ladder == block || Blocks.water == block || Blocks.flowing_water == block || Blocks.wall_sign == block || Blocks.standing_sign == block
        }
    }
}
