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
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
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
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.Packet
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket
import net.minecraft.network.packet.s2c.query.QueryPongS2CPacket
import net.minecraft.util.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.WorldSettings
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object Backtrack : Module("Backtrack", Category.COMBAT, hideModule = false) {

    private val nextBacktrackDelay by IntegerValue("NextBacktrackDelay", 0, 0..2000) { mode == "Modern" }
    private val delay by object : IntegerValue("Delay", 80, 0..700) {
        override fun onChange(oldValue: Int, newValue: Int): Int {
            if (mode == "Modern")
            {
                clearPackets()
                reset()
            }

            return newValue
        }
    }

    val mode by object : ListValue("Mode", arrayOf("Legacy", "Modern"), "Modern") {
        override fun onChanged(oldValue: String, newValue: String) {
            clearPackets()
            backtrackedPlayer.clear()
        }
    }

    // Legacy
    private val legacyPos by ListValue("Caching mode",
        arrayOf("ClientPos", "ServerPos"),
        "ClientPos"
    ) { mode == "Legacy" }

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
    val espMode by ListValue("ESP-Mode",
        arrayOf("None", "Box", "Player"),
        "Box",
        subjective = true
    ) { mode == "Modern" }
    private val rainbow by BoolValue("Rainbow", true, subjective = true) { mode == "Modern" && espMode == "Box" }
    private val red by IntegerValue("R",
        0,
        0..255,
        subjective = true
    ) { !rainbow && mode == "Modern" && espMode == "Box" }
    private val green by IntegerValue("G",
        255,
        0..255,
        subjective = true
    ) { !rainbow && mode == "Modern" && espMode == "Box" }
    private val blue by IntegerValue("B",
        0,
        0..255,
        subjective = true
    ) { !rainbow && mode == "Modern" && espMode == "Box" }

    private val packetQueue = LinkedHashMap<Packet<*>, Long>()
    private val positions = mutableListOf<Pair<Vec3d, Long>>()

    var target: LivingEntity? = null

    private var globalTimer = MSTimer()

    var shouldRender = true

    private var ignoreWholeTick = false

    private var delayForNextBacktrack = 0L

    // Legacy
    private val maximumCachedPositions by IntegerValue("MaxCachedPositions", 10, 1..20) { mode == "Legacy" }

    private val backtrackedPlayer = ConcurrentHashMap<UUID, MutableList<BacktrackData>>()

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
                    is PlayerSpawnS2CPacket -> {
                        // Insert first backtrack data
                        addBacktrackData(
                            packet.player,
                            packet.realX,
                            packet.realY,
                            packet.realZ,
                            System.currentTimeMillis()
                        )
                    }

                    is EntityS2CPacket -> {
                        if (legacyPos == "ServerPos") {
                            val entity = mc.world?.getEntityById(packet.id)
                            val entityMixin = entity as? IMixinEntity
                            if (entityMixin != null) {
                                addBacktrackData(
                                    entity.uuid,
                                    entityMixin.trueX,
                                    entityMixin.trueY,
                                    entityMixin.trueZ,
                                    System.currentTimeMillis()
                                )
                            }
                        }
                    }

                    is EntityPositionS2CPacket -> {
                        if (legacyPos == "ServerPos") {
                            val entity = mc.world?.getEntityById(packet.id)
                            val entityMixin = entity as? IMixinEntity
                            if (entityMixin != null) {
                                addBacktrackData(
                                    entity.uuid,
                                    entityMixin.trueX,
                                    entityMixin.trueY,
                                    entityMixin.trueZ,
                                    System.currentTimeMillis()
                                )
                            }
                        }
                    }
                }
            }

            "modern" -> {
                // Prevent cancelling packets when not needed
                if (packetQueue.isEmpty() && PacketUtils.queuedPackets.isEmpty() && !shouldBacktrack())
                    return

                when (packet) {
                    // Ignore server related packets
                    is HandshakeC2SPacket, is QueryRequestC2SPacket, is ChatMessageS2CPacket, is QueryPongS2CPacket ->
                        return

                    // Flush on teleport or disconnect
                    is PlayerPositionLookS2CPacket, is DisconnectS2CPacket -> {
                        clearPackets()
                        return
                    }

                    is PlaySoundIdS2CPacket ->
                        if (nonDelayedSoundSubstrings in packet.soundName)
                            return

                    // Flush on own death
                    is HealthUpdateS2CPacket ->
                        if (packet.health <= 0) {
                            clearPackets()
                            return
                        }

                    is EntitiesDestroyS2CPacket ->
                        if (target != null && target!!.entityId in packet.ids) {
                            clearPackets()
                            reset()
                            return
                        }

                    is EntityTrackerUpdateS2CPacket ->
                        if (target?.entityId == packet.id) {
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

                    is EntityStatusS2CPacket ->
                        if (packet.id == target?.entityId)
                            return
                }

                // Cancel every received packet to avoid possible server synchronization issues from random causes.
                if (event.eventType == EventState.RECEIVE) {
                    when (packet) {
                        is EntityS2CPacket ->
                            if (packet.id == target?.entityId)
                                (target as? IMixinEntity)?.run {
                                    synchronized(positions) {
                                        positions += Pair(Vec3d(trueX, trueY, trueZ), System.currentTimeMillis())
                                    }
                                }

                        is EntityPositionS2CPacket ->
                            if (packet.id == target?.entityId)
                                (target as? IMixinEntity)?.run {
                                    synchronized(positions) {
                                        positions += Pair(Vec3d(trueX, trueY, trueZ), System.currentTimeMillis())
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
        if (mode == "Legacy") {
            backtrackedPlayer.forEach { (key, backtrackData) ->
                // Remove old data
                backtrackData.removeAll { it.time + delay < System.currentTimeMillis() }

                // Remove player if there is no data left. This prevents memory leaks.
                if (backtrackData.isEmpty())
                    removeBacktrackData(key)
            }
        }

        val target = target as? LivingEntity
        val targetMixin = target as? IMixinEntity
        if (mode == "Modern")
        {
            if (targetMixin != null)
            {
                if (!Blink.blinkingReceive() && shouldBacktrack() && targetMixin.truePos) {
                    val trueDist = mc.player.getDistance(targetMixin.trueX, targetMixin.trueY, targetMixin.trueZ)
                    val dist = mc.player.getDistance(target.posX, target.posY, target.posZ)
        
                    if (trueDist <= 6f && (!smart || trueDist >= dist) && (style == "Smooth" || !globalTimer.hasTimePassed(delay))) {
                        shouldRender = true
        
                        if (mc.player.getDistanceToEntityBox(target) in minDistance..maxDistance)
                            handlePackets()
                        else
                            handlePacketsRange()
                    } else {
                        clearPackets()
                        globalTimer.reset()
                    }
                }
            }
            else
            {
                clearPackets()
                globalTimer.reset()
            }
        }

        ignoreWholeTick = false
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (!isSelected(event.targetEntity, true))
            return

        // Clear all packets, start again on enemy change
        if (target != event.targetEntity) {
            clearPackets()
            reset()
        }

        if (event.targetEntity is LivingEntity) {
            target = event.targetEntity
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        when (mode.lowercase()) {
            "legacy" -> {
                val color = Color.RED

                for (entity in mc.world.entities) {
                    if (entity is PlayerEntity) {
                        glPushMatrix()
                        glDisable(GL_TEXTURE_2D)
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                        glEnable(GL_LINE_SMOOTH)
                        glEnable(GL_BLEND)
                        glDisable(GL_DEPTH_TEST)

                        mc.entityRenderer.disableLightmap()

                        glBegin(GL_LINE_STRIP)
                        glColor(color)

                        val renderPosX = mc.entityRenderManager.viewerPosX
                        val renderPosY = mc.entityRenderManager.viewerPosY
                        val renderPosZ = mc.entityRenderManager.viewerPosZ

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
                if (!shouldBacktrack() || packetQueue.isEmpty() || !shouldRender)
                    return

                if (espMode != "Box") return

                val renderManager = mc.entityRenderManager

                target?.run {
                    val targetEntity = target as IMixinEntity

                    if (targetEntity.truePos) {
                        val x =
                            targetEntity.trueX - renderManager.renderPosX
                        val y =
                            targetEntity.trueY - renderManager.renderPosY
                        val z =
                            targetEntity.trueZ - renderManager.renderPosZ

                        val Box = boundingBox.offset(-posX, -posY, -posZ).offset(x, y, z)

                        drawBacktrackBox(
                            Box.fromBounds(
                                Box.minX,
                                Box.minY,
                                Box.minZ,
                                Box.maxX,
                                Box.maxY,
                                Box.maxZ
                            ), color
                        )
                    }
                }
            }
        }
    }

    @EventTarget
    fun onEntityMove(event: EntityMovementEvent) {
        if (mode == "Legacy" && legacyPos == "ClientPos") {
            val entity = event.movedEntity

            // Check if entity is a player
            if (entity is PlayerEntity) {
                // Add new data
                addBacktrackData(entity.uuid, entity.posX, entity.posY, entity.posZ, System.currentTimeMillis())
            }
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        // Clear packets on disconnect only
        // Set target to null on world change
        if (mode == "Modern") {
            if (event.worldClient == null)
                clearPackets(false)
            target = null
        }
    }

    override fun onEnable() =
        reset()

    override fun onDisable() {
        clearPackets()
        backtrackedPlayer.clear()
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
                val targetPos = Vec3d(target!!.posX, target!!.posY, target!!.posZ)
                val (dx, dy, dz) = data.first - targetPos
                val targetBox = target!!.hitBox.offset(dx, dy, dz)
                if (mc.player.getDistanceToBox(targetBox) in minDistance..maxDistance) {
                    found = true
                    break
                }
            }
        }
        return if (found) time else -1L
    }

    private fun clearPackets(handlePackets: Boolean = true) {
        if (packetQueue.isNotEmpty()) {
            delayForNextBacktrack = System.currentTimeMillis() + nextBacktrackDelay
        }

        synchronized(packetQueue) {
            if (handlePackets)
                PacketUtils.queuedPackets.addAll(packetQueue.keys)

            packetQueue.clear()
        }

        positions.clear()
        shouldRender = false
        ignoreWholeTick = true
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

    /**
     * This function will return the nearest tracked range of an entity.
     */
    fun getNearestTrackedDistance(entity: Entity): Double {
        var nearestRange = 0.0

        loopThroughBacktrackData(entity) {
            val range = entity.getDistanceToEntityBox(mc.player)

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
        if (!Backtrack.state || entity !is PlayerEntity || mode == "Modern")
            return

        val backtrackDataArray = getBacktrackData(entity.uuid) ?: return

        val currPos = entity.currPos
        val prevPos = entity.prevPos

        // This will loop through the backtrack data. We are using reversed() to loop through the data from the newest to the oldest.
        for ((x, y, z, _) in backtrackDataArray.reversed()) {
            entity.setPosAndPrevPos(Vec3d(x, y, z))

            if (action())
                break
        }

        // Reset position
        entity.setPosAndPrevPos(currPos, prevPos)
    }

    fun runWithNearestTrackedDistance(entity: Entity, f: () -> Unit) {
        if (entity !is PlayerEntity || !handleEvents() || mode == "Modern") {
            f()

            return
        }

        var backtrackDataArray = getBacktrackData(entity.uuid)?.toMutableList()

        if (backtrackDataArray == null) {
            f()

            return
        }

        backtrackDataArray = backtrackDataArray.sortedBy { (x, y, z, _) ->
            runWithSimulatedPastPosition(entity, Vec3d(x, y, z)) {
                mc.player.getDistanceToBox(entity.hitBox)
            }
        }.toMutableList()

        val (x, y, z, _) = backtrackDataArray.first()

        runWithSimulatedPastPosition(entity, Vec3d(x, y, z)) {
            f()

            null
        }
    }

    private fun runWithSimulatedPastPosition(entity: Entity, Vec3d: Vec3d, f: () -> Double?): Double? {
        val currPos = entity.currPos
        val prevPos = entity.prevPos

        entity.setPosAndPrevPos(Vec3d)

        val result = f()

        // Reset position
        entity.setPosAndPrevPos(currPos, prevPos)

        return result
    }

    val color
        get() = if (rainbow) rainbow() else Color(red, green, blue)

    fun shouldBacktrack() =
         mc.player != null && target != null && mc.player.health > 0 && (target!!.health > 0 || target!!.health.isNaN()) && mc.interactionManager.isSpectator && System.currentTimeMillis() >= delayForNextBacktrack && target?.let {
            isSelected(it, true) && (mc.player?.ticksAlive ?: 0) > 20 && !ignoreWholeTick
        } ?: false

    private fun reset() {
        target = null
        globalTimer.reset()
    }
}

data class BacktrackData(val x: Double, val y: Double, val z: Double, val time: Long)
