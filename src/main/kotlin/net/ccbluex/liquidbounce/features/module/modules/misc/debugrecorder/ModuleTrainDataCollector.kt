package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.traindata.DataSample
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.traindata.ItemCategoryProvider
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.traindata.ParquetWriterUtil
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.traindata.SurroundingsMapper
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.underline
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

object ModuleTrainDataCollector : ClientModule("TrainDataCollector", ModuleCategories.MISC, disableOnQuit = true) {

    init {
        doNotIncludeAlways()
    }

    private val folder = ConfigSystem.rootFolder.resolve("train-data/v1").apply {
        mkdirs()
    }

    // Save every 5 minutes
    private const val AUTOSAVE_INTERVAL = 5 * 60 * 20

    private val collectedData = LinkedBlockingQueue<DataSample>()
    private val swingingPlayers = ConcurrentHashMap<UUID, Boolean>()
    private val damagedPlayers = ConcurrentHashMap<UUID, Boolean>()

    private var sessionStartTime = 0L
    private var sessionTicks = 0L

    override fun onEnabled() {
        collectedData.clear()
        swingingPlayers.clear()
        damagedPlayers.clear()
        sessionStartTime = (System.currentTimeMillis() / 1000L) * 1_000_000_000
        sessionTicks = 0L
        chat(regular("Started collecting training data..."))
    }

    override fun onDisabled() {
        saveData()
    }

    private fun saveData() {
        if (collectedData.isEmpty()) {
            chat(regular("No data to save."))
            return
        }

        val dataToSave = ArrayList<DataSample>()

        collectedData.drainTo(dataToSave)
        swingingPlayers.clear()
        damagedPlayers.clear()

        val formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")
        val fileName = "train_data_${LocalDateTime.now().format(formatter)}.parquet"
        val file = folder.resolve(fileName)

        chat(regular("Saving ${dataToSave.size} samples in background..."))

        thread(name = "TrainDataCollector-Save") {
            try {
                ParquetWriterUtil.saveToParquet(dataToSave, file)

                mc.submit {
                    val pathText = file.absolutePath.asText()
                        .underline(true)
                        .onHover(HoverEvent.ShowText(regular("Open file")))
                        .onClick(ClickEvent.OpenFile(file.absolutePath))

                    chat(regular("Training data saved to "), pathText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mc.submit {
                    chat(markAsError("Failed to save training data: ${e.message}".asText()))
                }
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val level = world ?: return@handler
        val localPlayer = player ?: return@handler

        // Timestamp calculation: seconds on activation + ticks since activation
        val timestamp = sessionStartTime + sessionTicks

        for (p in level.players()) {
            if (p == localPlayer || p.isSpectator || p.isInvisible || !p.shouldBeAttacked()) continue
            collectedData.add(createSample(p, timestamp))
        }

        if (sessionTicks > 0 && sessionTicks % AUTOSAVE_INTERVAL == 0L && collectedData.isNotEmpty()) {
            chat(regular("Autosaving..."))
            saveData()
        }

        sessionTicks++
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is ClientboundAnimatePacket && (packet.action == 0 || packet.action == 3)) {
            val entity = world?.getEntity(packet.id)
            if (entity is Player && entity != player) {
                swingingPlayers[entity.uuid] = true
            }
        } else if (packet is ClientboundDamageEventPacket) {
            val entity = world?.getEntity(packet.entityId())
            if (entity is Player && entity != player) {
                damagedPlayers[entity.uuid] = true
            }
        }
    }

    private fun createSample(p: Player, timestamp: Long): DataSample {
        val isSwinging = swingingPlayers.getOrDefault(p.uuid, false)
        swingingPlayers[p.uuid] = false

        // Use either the damage packet flag or the hurtTime indicator
        val wasHit = damagedPlayers.getOrDefault(p.uuid, false) || p.hurtTime == p.hurtDuration && p.hurtDuration > 0
        damagedPlayers[p.uuid] = false

        val mapper = SurroundingsMapper(p, world!!).apply { compute() }

        // Find closest flying arrow
        var closestArrowDistSq = Double.MAX_VALUE
        var closestArrowX = 0.0
        var closestArrowY = 0.0
        var closestArrowZ = 0.0

        for (entity in world!!.entitiesForRendering()) {
            if (entity is AbstractArrow && !entity.isInGround) {
                val distSq = entity.distanceToSqr(p)
                if (distSq < closestArrowDistSq) {
                    closestArrowDistSq = distSq
                    // Relative coordinates to player
                    closestArrowX = entity.x
                    closestArrowY = entity.y
                    closestArrowZ = entity.z
                }
            }
        }

        return DataSample(
            timestamp = timestamp,
            playerId = p.uuid.toString(),
            posX = p.x,
            posY = p.y,
            posZ = p.z,
            yaw = Math.toRadians(p.yRot.toDouble()).toFloat(),
            pitch = Math.toRadians(p.xRot.toDouble()).toFloat(),
            isSneaking = p.isShiftKeyDown,
            isOnGround = p.onGround(),
            isUsingItem = p.isUsingItem,
            isSwinging = isSwinging,
            wasHit = wasHit,
            closestArrowX = closestArrowX,
            closestArrowY = closestArrowY,
            closestArrowZ = closestArrowZ,
            mainHandCategory = ItemCategoryProvider.getCategory(p.mainHandItem),
            offHandCategory = ItemCategoryProvider.getCategory(p.offhandItem),
            floorMap = mapper.floorMap,
            ceilMap = mapper.ceilMap,
            poiMap = mapper.poiMap
        )
    }
}
