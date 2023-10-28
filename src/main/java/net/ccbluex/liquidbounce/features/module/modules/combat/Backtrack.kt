/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.PacketUtils.handlePacket
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToBox
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBacktrackBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.server.*
import net.minecraft.util.AxisAlignedBB
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import org.lwjgl.opengl.GL11.*
import net.minecraft.entity.player.EntityPlayer

object Backtrack : Module("Backtrack", ModuleCategory.COMBAT) {

    public val backtrackMode by ListValue("Mode", arrayOf("Legacy", "Modern"), "Modern")

    private val delay by object : IntegerValue("Delay", 80, 0..700) {
        override fun onChange(oldValue: Int, newValue: Int): Int {
            if (backtrackMode == "Modern") {
                clearPackets()
                packetQueue.clear()
            }       

            return newValue
        }
    }

    // newgen
    private val maxDistanceValue: FloatValue = object : FloatValue("MaxDistance", 3.0f, 0.0f..3.5f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minDistance)
        override fun isSupported() = backtrackMode == "Modern"
    }
    private val maxDistance by maxDistanceValue
    private val minDistance by object : FloatValue("MinDistance", 2.0f, 0.0f..3.0f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceIn(minimum, maxDistance)
        override fun isSupported() = backtrackMode == "Modern"
    }

    private val pingSpoof by BoolValue("PingSpoof", false) { backtrackMode == "Modern" }
    private val delayVelocity by BoolValue("DelayVelocity", false) { backtrackMode == "Modern" }
    private val delayExplosion by BoolValue("DelayExplosion", false) { backtrackMode == "Modern" }
    private val allPackets by BoolValue("AllPackets", false) { backtrackMode == "Modern" }
    private val excludeSpecial by BoolValue("ExcludeSpecial", true) { backtrackMode == "Modern" && allPackets}

    // ESP
    private val rainbow by BoolValue("Rainbow", true) { backtrackMode == "Modern" }
    private val red by IntegerValue("R", 0, 0..255) { !rainbow && backtrackMode == "Modern" }
    private val green by IntegerValue("G", 255, 0..255) { !rainbow && backtrackMode == "Modern"}
    private val blue by IntegerValue("B", 0, 0..255) { !rainbow && backtrackMode == "Modern"}

    private val packetQueue = ConcurrentHashMap<Packet<*>, Pair<Long, Long>>()
    private var target: Entity? = null
    private var realX = 0.0
    private var realY = 0.0
    private var realZ = 0.0

    // Legacy
    private val maximumCachedPositions by IntegerValue("MaxCachedPositions", 10, 1..20) { backtrackMode == "Legacy" }

    private val backtrackedPlayer = mutableMapOf<UUID, MutableList<BacktrackData>>()

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        
        if (mc.thePlayer == null) {
            return
        }
        when (backtrackMode.lowercase()) {
            "legacy" -> {
                when (packet) {
                    // Check if packet is a spawn player packet
                    is S0CPacketSpawnPlayer -> {
                        // Insert first backtrack data
                        addBacktrackData(packet.player, packet.x / 32.0, packet.y / 32.0, packet.z / 32.0, System.currentTimeMillis())
                    }
                }

                backtrackedPlayer.forEach { (key, backtrackData) ->
                    // Remove old data
                    backtrackData.removeIf { it.time + delay < System.currentTimeMillis() }

                    // Remove player if there is no data left. This prevents memory leaks.
                    if (backtrackData.isEmpty()) {
                        removeBacktrackData(key)
                    }
                }
            }

            "modern" -> {

                when (packet) {
                    is S32PacketConfirmTransaction -> {
                        if (pingSpoof) {
                            event.cancelEvent()
                            packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                        }
                        return
                    }

                    is S12PacketEntityVelocity -> {
                        if (delayVelocity) {
                            event.cancelEvent()
                            packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                        }
                        return
                    }

                    is S27PacketExplosion -> {
                        if (delayExplosion) {
                            event.cancelEvent()
                            packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                        }
                        return
                    }

                    is S14PacketEntity -> {
                        if (packet.getEntity(mc.theWorld) == target) {
                            realX += packet.func_149062_c().toDouble()
                            realY += packet.func_149061_d().toDouble()
                            realZ += packet.func_149064_e().toDouble()
                        }
                        event.cancelEvent()
                        packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                        return
                    }

                    is S19PacketEntityStatus -> {
                        event.cancelEvent()
                        packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                        return
                    }
                }

                if (event.eventType == EventState.RECEIVE && allPackets) {
                    val isSpecialPacket = packet is S29PacketSoundEffect || packet is S0CPacketSpawnPlayer || packet is S02PacketChat || packet is S0BPacketAnimation || packet is S2APacketParticles || packet is S06PacketUpdateHealth

                    if (excludeSpecial) {
                        if (!isSpecialPacket){
                            event.cancelEvent()
                            packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                        }
                    }
                    else {
                        event.cancelEvent()
                        packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                    }   
                }
            }
        }     
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (backtrackMode == "Modern") {
            if (shouldBacktrack()) handlePackets()
            else clearPackets()
        }
        
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        target = event.targetEntity
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        when (backtrackMode.lowercase()) {
            "legacy" -> {
                val color = Color.RED

                for (entity in mc.theWorld.loadedEntityList) {
                    if (entity is EntityPlayer) {
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

                        loopThroughBacktrackData(entity) {
                            glVertex3d(entity.posX - renderPosX, entity.posY - renderPosY, entity.posZ - renderPosZ)
                            false
                        }

                        glColor4d(1.0, 1.0, 1.0, 1.0)
                        glEnd()
                        glEnable(GL_DEPTH_TEST)
                        glDisable(GL_LINE_SMOOTH)
                        glDisable(GL_BLEND)
                        glEnable(GL_TEXTURE_2D)
                        glPopMatrix()
                    }
                }
            }

            "modern" -> {
                if (!shouldBacktrack()) {
                    return
                }

                val renderManager = mc.renderManager
                val timer = mc.timer

                target?.let {
                    val x = it.lastTickPosX + (it.posX - it.lastTickPosX) * timer.renderPartialTicks - renderManager.renderPosX
                    val y = it.lastTickPosY + (it.posY - it.lastTickPosY) * timer.renderPartialTicks - renderManager.renderPosY
                    val z = it.lastTickPosZ + (it.posZ - it.lastTickPosZ) * timer.renderPartialTicks - renderManager.renderPosZ
                    val axisAlignedBB = it.entityBoundingBox.offset(-it.posX, -it.posY, -it.posZ).offset(x, y, z)

                    drawBacktrackBox(
                        AxisAlignedBB.fromBounds(
                            axisAlignedBB.minX,
                            axisAlignedBB.minY,
                            axisAlignedBB.minZ,
                            axisAlignedBB.maxX,
                            axisAlignedBB.maxY,
                            axisAlignedBB.maxZ
                        ).offset(realX / 32.0, realY / 32.0, realZ / 32.0), color
                    )
                }
            }
        }


        
    }

    @EventTarget
    fun onEntityMove(event: EntityMovementEvent) {
        if (backtrackMode == "Legacy") {
            val entity = event.movedEntity

            // Check if entity is a player
            if (entity is EntityPlayer) {
                // Add new data
                addBacktrackData(entity.uniqueID, entity.posX, entity.posY, entity.posZ, System.currentTimeMillis())
            }
        }

    }
    
    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (backtrackMode == "Modern") clearPackets()
    }

    override fun onEnable() {
        target = null
        realX = 0.0
        realY = 0.0
        realZ = 0.0
    }

    override fun onDisable() {
        if (backtrackMode == "Modern") clearPackets()
    }

    private fun handlePackets() {
        val filtered = packetQueue.filter { 
            it.value.first <= System.currentTimeMillis()
        }.entries.sortedBy { it.value.second }.map { it.key }

        for (packet in filtered) {
            handlePacket(packet)
            if (packet is S14PacketEntity && packet.getEntity(mc.theWorld) == target) {
                realX -= packet.func_149062_c().toDouble()
                realY -= packet.func_149061_d().toDouble()
                realZ -= packet.func_149064_e().toDouble()
            }
            packetQueue.remove(packet)
        }
    }

    private fun clearPackets(handlePackets: Boolean = true) {
        target = null
        if (handlePackets) {
            val filtered = packetQueue.entries.sortedBy { it.value.second }.map { it.key }
    
            for (packet in filtered) {
                handlePacket(packet)
                packetQueue.remove(packet)
            }
        }
        else packetQueue.clear()
        realX = 0.0
        realY = 0.0
        realZ = 0.0
    }

    private fun addBacktrackData(id: UUID, x: Double, y: Double, z: Double, time: Long) {
        // Get backtrack data of player
        val backtrackData = getBacktrackData(id)

        // Check if there is already data of the player
        if (backtrackData != null) {
            // Check if there is already enough data of the player
            if (backtrackData.size >= maximumCachedPositions) {
                // Remove first data
                backtrackData.removeFirst()
            }

            // Insert new data
            backtrackData += BacktrackData(x, y, z, time)
        } else {
            // Create new list
            backtrackedPlayer[id] = mutableListOf(BacktrackData(x, y, z, time))
        }
    }

    fun getBacktrackData(id: UUID) = backtrackedPlayer[id]

    fun removeBacktrackData(id: UUID) {
        backtrackedPlayer.remove(id)
    }

    /**
     * This function will return the nearest tracked range of an entity.
     */
    fun getNearestTrackedDistance(entity: Entity): Double {
        var nearestRange = 0.0

        loopThroughBacktrackData(entity) {
            val range = entity.getDistanceToEntityBox(mc.thePlayer)

            if (range < nearestRange || nearestRange == 0.0) {
                nearestRange = range
            }

            false
        }

        return nearestRange
    }

    /**
     * This function will loop through the backtrack data of an entity.
     */
    fun loopThroughBacktrackData(entity: Entity, action: () -> Boolean) {
        if (!Backtrack.state || entity !is EntityPlayer || Backtrack.backtrackMode == "Modern") {
            return
        }

        val backtrackDataArray = getBacktrackData(entity.uniqueID) ?: return
        val entityPosition = entity.positionVector
        val prevPosition = Triple(entity.prevPosX, entity.prevPosY, entity.prevPosZ)

        // This will loop through the backtrack data. We are using reversed() to loop through the data from the newest to the oldest.
        for (backtrackData in backtrackDataArray.reversed()) {
            entity.setPosition(backtrackData.x, backtrackData.y, backtrackData.z)
            entity.prevPosX = backtrackData.x
            entity.prevPosY = backtrackData.y
            entity.prevPosZ = backtrackData.z
            if (action()) {
                break
            }
        }

        // Reset position
        val (prevX, prevY, prevZ) = prevPosition
        entity.prevPosX = prevX
        entity.prevPosY = prevY
        entity.prevPosZ = prevZ

        entity.setPosition(entityPosition.xCoord, entityPosition.yCoord, entityPosition.zCoord)
    }


    val color
        get() = if (rainbow) rainbow() else Color(red, green, blue)

    private fun shouldBacktrack(): Boolean {
        return (target != null) && (!target!!.isDead) && (mc.thePlayer.getDistanceToBox(target!!.hitBox) in minDistance..maxDistance) && (mc.thePlayer.ticksExisted > 20)
    }
}

data class BacktrackData(val x: Double, val y: Double, val z: Double, val time: Long)

