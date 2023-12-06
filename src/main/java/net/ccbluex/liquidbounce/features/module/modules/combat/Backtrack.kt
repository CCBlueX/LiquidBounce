/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.misc.StringUtils.contains
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBacktrackBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.server.*
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*


object Backtrack : Module("Backtrack", ModuleCategory.COMBAT) {

    private val delay by object : IntegerValue("Delay", 80, 0..700) {
        override fun onChange(oldValue: Int, newValue: Int): Int {
            if (mode == "Modern")
                clearPackets()

            return newValue
        }
    }

    val mode by ListValue("Mode", arrayOf("Legacy", "Modern"), "Modern")

    // Modern
    private val style by ListValue("Style", arrayOf("Pulse", "Smooth"), "Smooth") { mode == "Modern" }

    private val maxDistanceValue: FloatValue = object : FloatValue("MaxDistance", 3.0f, 0.0f..3.5f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minDistance)
        override fun isSupported() = mode == "Modern"
    }
    private val maxDistance by maxDistanceValue
    private val minDistance by object : FloatValue("MinDistance", 2.0f, 0.0f..3.0f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceIn(minimum, maxDistance)
        override fun isSupported() = mode == "Modern"
    }
    private val smart by BoolValue("Smart", true) { mode == "Modern" }

    // ESP
    private val esp by BoolValue("ESP", true, subjective = true) { mode == "Modern" }
        private val rainbow by BoolValue("Rainbow", true, subjective = true) { mode == "Modern" && esp }
        private val red by IntegerValue("R", 0, 0..255, subjective = true) { !rainbow && mode == "Modern" && esp }
        private val green by IntegerValue("G", 255, 0..255, subjective = true) { !rainbow && mode == "Modern" && esp }
        private val blue by IntegerValue("B", 0, 0..255, subjective = true) { !rainbow && mode == "Modern" && esp }

    private val packetQueue = LinkedHashMap<Packet<*>, Long>()
    private val positions = mutableListOf<Pair<Vec3, Long>>()

    private var target: Entity? = null

    private var globalTimer = MSTimer()

    private var shouldDraw = true

    // Legacy
    private val maximumCachedPositions by IntegerValue("MaxCachedPositions", 10, 1..20) { mode == "Legacy" }

    private val backtrackedPlayer = mutableMapOf<UUID, MutableList<BacktrackData>>()

    private val nonDelayedSoundSubstrings = arrayOf("game.player.hurt", "game.player.die")

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (Blink.blinkingReceive())
            return

        if (event.isCancelled)
            return

        when (mode.lowercase()) {
            "legacy" -> {
                when (packet) {
                    // Check if packet is a spawn player packet
                    is S0CPacketSpawnPlayer -> {
                        // Insert first backtrack data
                        addBacktrackData(
                            packet.player,
                            packet.realX,
                            packet.realY,
                            packet.realZ,
                            System.currentTimeMillis()
                        )
                    }
                }

                backtrackedPlayer.forEach { (key, backtrackData) ->
                    // Remove old data
                    backtrackData.removeAll { it.time + delay < System.currentTimeMillis() }

                    // Remove player if there is no data left. This prevents memory leaks.
                    if (backtrackData.isEmpty())
                        removeBacktrackData(key)
                }
            }

            "modern" -> {
                // Prevent cancelling packets when not needed
                if (packetQueue.isEmpty() && !shouldBacktrack())
                    return

                when (packet) {
                    // Ignore chat packets
                    is S02PacketChat -> return

                    // Flush on teleport or disconnect
                    is S08PacketPlayerPosLook, is S40PacketDisconnect -> {
                        clearPackets()
                        return
                    }

                    is S29PacketSoundEffect ->
                        if (nonDelayedSoundSubstrings in packet.soundName)
                            return

                    // Flush on own death
                    is S06PacketUpdateHealth ->
                        if (packet.health <= 0) {
                            clearPackets()
                            return
                        }

                    is S13PacketDestroyEntities ->
                        if (target != null && target!!.entityId in packet.entityIDs) {
                            clearPackets()
                            reset()
                            return
                        }

                    is S1CPacketEntityMetadata ->
                        if (target?.entityId == packet.entityId) {
                            val metadata = packet.func_149376_c() ?: return

                            metadata.forEach {
                                if (it.dataValueId == 6) {
                                    val objectValue = it.getObject().toString().toDoubleOrNull()
                                    if (objectValue != null && !objectValue.isNaN() && objectValue <= 0.0) {
                                        clearPackets()
                                        reset()
                                        return
                                    }
                                }
                            }

                            return
                        }

                    is S19PacketEntityStatus ->
                        if (packet.entityId == target?.entityId)
                            return
                }

                // Cancel every received packet to avoid possible server synchronization issues from random causes.
                if (event.eventType == EventState.RECEIVE) {
                    when (packet) {
                        is S14PacketEntity ->
                            if (packet.entityId == target?.entityId)
                                (target as? IMixinEntity)?.run {
                                    synchronized(positions) {
                                        positions += Pair(Vec3(trueX, trueY, trueZ), System.currentTimeMillis())
                                    }
                                }

                        is S18PacketEntityTeleport ->
                            if (packet.entityId == target?.entityId)
                                (target as? IMixinEntity)?.run {
                                    synchronized(positions) {
                                        positions += Pair(Vec3(trueX, trueY, trueZ), System.currentTimeMillis())
                                    }
                                }
                    }

                    event.cancelEvent()
                    synchronized(packetQueue) {
                        packetQueue[packet] = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    @EventTarget
    fun onGameLoop(event: GameLoopEvent) {
        val target = target as? EntityLivingBase
        val targetMixin = target as? IMixinEntity

        if (targetMixin != null && !Blink.blinkingReceive() && shouldBacktrack()
            && targetMixin.truePos && (style == "Smooth" || !globalTimer.hasTimePassed(delay))
        ) {
            val trueDistSq = targetMixin.run { mc.thePlayer.getDistanceSq(trueX, trueY, trueZ) }
            val distSq = target.run { mc.thePlayer.getDistanceSq(posX, posY, posZ) }

            if (trueDistSq <= 36f && (!smart || trueDistSq >= distSq)) {
                shouldDraw = true
                if (mc.thePlayer.getDistanceToEntityBox(target) in minDistance..maxDistance)
                    handlePackets()
                else
                    handlePacketsRange()
            }
        } else {
            clearPackets()
            globalTimer.reset()
        }
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (!isEnemy(event.targetEntity))
            return

        // Clear all packets, start again on enemy change
        if (target != event.targetEntity) {
            clearPackets()
            reset()
        }

        target = event.targetEntity
    }
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        when (mode.lowercase()) {
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
                if (!shouldBacktrack() || packetQueue.isEmpty() || !shouldDraw || !esp )
                    return

                val renderManager = mc.renderManager

                target?.run {
                    val targetEntity = target as IMixinEntity

                    if (targetEntity.truePos) {

                        val x =
                            targetEntity.trueX - renderManager.renderPosX
                        val y =
                            targetEntity.trueY - renderManager.renderPosY
                        val z =
                            targetEntity.trueZ - renderManager.renderPosZ

                        val axisAlignedBB = entityBoundingBox.offset(-posX, -posY, -posZ).offset(x, y, z)

                        drawBacktrackBox(
                            AxisAlignedBB.fromBounds(
                                axisAlignedBB.minX,
                                axisAlignedBB.minY,
                                axisAlignedBB.minZ,
                                axisAlignedBB.maxX,
                                axisAlignedBB.maxY,
                                axisAlignedBB.maxZ
                            ), color
                        )
                    }
                }
            }
        }
    }

    @EventTarget
    fun onEntityMove(event: EntityMovementEvent) {
        if (mode == "Legacy") {
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
        // Clear packets on disconnect only
        if (mode == "Modern" && event.worldClient == null)
            clearPackets(false)
    }

    override fun onEnable() = reset()

    override fun onDisable() {
        if (mode == "Modern") {
            clearPackets()
            reset()
        }
    }

    private fun handlePackets() {
        synchronized(packetQueue) {
            packetQueue.entries.removeAll { (packet, timestamp) ->
                if (timestamp <= System.currentTimeMillis() - delay) {
                    PacketUtils.queuedPackets.add(packet)
                    true
                } else false
            }
        }
        synchronized(positions) {
            positions.removeAll { (_, timestamp) -> timestamp < System.currentTimeMillis() - delay }
        }
    }

    private fun handlePacketsRange() {
        val time = getRangeTime()
        if (time == -1L) {
            clearPackets()
            return
        }
        synchronized(packetQueue) {
            packetQueue.entries.removeAll { (packet, timestamp) ->
                if (timestamp <= time) {
                    PacketUtils.queuedPackets.add(packet)
                    true
                } else false
            }
        }
        synchronized(positions) {
            positions.removeAll { (_, timestamp) -> timestamp < time }
        }
    }

    private fun getRangeTime(): Long {
        if (target == null) return 0L
        var time = 0L
        var found = false
        synchronized(positions) {
            for (data in positions) {
                time = data.second
                val targetPos = Vec3(target!!.posX, target!!.posY, target!!.posZ)
                val (dx, dy, dz) = data.first - targetPos
                val targetBox = target!!.hitBox.offset(dx, dy, dz)
                if (mc.thePlayer.getDistanceToBox(targetBox) in minDistance..maxDistance) {
                    found = true
                    break
                }
            }
        }
        return if (found) time else -1L
    }

    private fun clearPackets(handlePackets: Boolean = true) {
        synchronized(packetQueue) {
            if (handlePackets)
                PacketUtils.queuedPackets.addAll(packetQueue.keys)

            packetQueue.clear()
        }
        positions.clear()
        shouldDraw = false
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

    private fun getBacktrackData(id: UUID) = backtrackedPlayer[id]

    private fun removeBacktrackData(id: UUID) = backtrackedPlayer.remove(id)

    private fun isEnemy(entity: Entity?): Boolean {
        if (entity is EntityLivingBase && entity != mc.thePlayer) {
            if (entity is EntityPlayer) {
                if (entity.isSpectator || isBot(entity)) return false

                if (entity.isClientFriend() && !NoFriends.handleEvents()) return false

                return !Teams.handleEvents() || !Teams.isInYourTeam(entity)
            }

            return true
        }

        return false
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
        if (!Backtrack.state || entity !is EntityPlayer || mode == "Modern")
            return

        val backtrackDataArray = getBacktrackData(entity.uniqueID) ?: return
        val entityPosition = entity.positionVector
        val (prevX, prevY, prevZ) = Triple(entity.prevPosX, entity.prevPosY, entity.prevPosZ)

        // This will loop through the backtrack data. We are using reversed() to loop through the data from the newest to the oldest.
        for ((x, y, z, _) in backtrackDataArray.reversed()) {
            entity.setPosition(x, y, z)
            entity.prevPosX = x
            entity.prevPosY = y
            entity.prevPosZ = z

            if (action())
                break
        }

        // Reset position
        entity.prevPosX = prevX
        entity.prevPosY = prevY
        entity.prevPosZ = prevZ

        entity.setPosition(entityPosition.xCoord, entityPosition.yCoord, entityPosition.zCoord)
    }

    val color
        get() = if (rainbow) rainbow() else Color(red, green, blue)

    private fun shouldBacktrack() =
        target?.let {
            !it.isDead && isEnemy(it) && (mc.thePlayer?.ticksExisted ?: 0) > 20
        } ?: false

    private fun reset() {
        target = null
        globalTimer.reset()
    }
}

data class BacktrackData(val x: Double, val y: Double, val z: Double, val time: Long)
