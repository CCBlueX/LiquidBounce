/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerInteractedItemEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawLine
import net.ccbluex.liquidbounce.render.drawPlane
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.block.immutable
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.center
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.math.toFixed
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.math.toVec3f
import net.ccbluex.liquidbounce.utils.world.forEachSectionBlock
import net.ccbluex.liquidbounce.utils.world.stronghold.EyeMeasurement
import net.ccbluex.liquidbounce.utils.world.stronghold.PosteriorSnapshot
import net.ccbluex.liquidbounce.utils.world.stronghold.StrongholdBayesianEstimator
import net.ccbluex.liquidbounce.utils.world.stronghold.StrongholdHypothesis
import net.ccbluex.liquidbounce.utils.world.stronghold.StrongholdHypothesisGenerator
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.resources.ResourceKey
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.projectile.EyeOfEnder
import net.minecraft.world.item.Items
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import kotlin.math.hypot

private const val RAY_RENDER_LENGTH = 2048.0

/**
 * Stronghold finder module.
 *
 * Automatically tracks Eye of Ender throws and estimates the strongest stronghold chunk candidate
 * using a Bayesian posterior.
 *
 * [Article](https://github.com/Ninjabrain1/Ninjabrain-Bot/blob/main/triangulation.pdf)
 */
@Suppress("TooManyFunctions")
object ModuleStrongholdFinder : ClientModule(
    "StrongholdFinder",
    ModuleCategories.WORLD,
    aliases = listOf("Triangulation")
) {

    private val sigma by float("Sigma", 0.03f, 0.005f..0.20f, "°").onChanged {
        onEstimatorSettingsChanged()
    }

    private val hypothesisCount by int("HypothesisCount", 20000, 2000..100000).onChanged {
        cachedHypothesisCount = -1
        onEstimatorSettingsChanged()
    }

    private val requireSameStrongholdAcrossThrows by boolean("RequireSameStrongholdAcrossThrows", true).onChanged {
        onEstimatorSettingsChanged()
    }

    private val sampleDelayTicks by int("SampleDelayTicks", 2, 0..10)
    private val minEyeHorizontalSpeed by float("MinEyeHorizontalSpeed", 0.02f, 0.001f..0.2f)
    private val maxSampleAgeTicks by int("MaxSampleAgeTicks", 20, 5..100)

    private val showTopCandidates by int("ShowTopCandidates", 3, 1..10).onChanged {
        onEstimatorSettingsChanged()
    }

    private val renderRays by boolean("RenderRays", true)
    private val renderBestChunk by boolean("RenderBestChunk", true)
    private val renderTopChunks by boolean("RenderTopChunks", true)
    private val announcePrediction by boolean("AnnouncePrediction", true)
    private val resetOnWorldChange by boolean("ResetOnWorldChange", true)

    private val pendingThrows = ArrayDeque<PendingThrow>()
    private val trackedEyes = Int2ObjectLinkedOpenHashMap<TrackedEye>()
    private val measurements = mutableListOf<EyeMeasurement>()
    private var posterior: PosteriorSnapshot? = null
    private var lastAnnouncedCandidate: ChunkPos? = null
    private val detectedPortalBlocks = linkedMapOf<BlockPos, PortalBlockType>()

    private var hypothesisCache: List<StrongholdHypothesis> = emptyList()
    private var cachedHypothesisCount = -1

    override fun onDisabled() {
        resetState()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        if (resetOnWorldChange) {
            resetState()
        }
    }

    @Suppress("unused")
    private val interactedItemHandler = handler<PlayerInteractedItemEvent> { event ->
        if (!isOverworld()) {
            return@handler
        }

        if (!event.actionResult.consumesAction()) {
            return@handler
        }

        if (event.player.getItemInHand(event.hand).item != Items.ENDER_EYE) {
            return@handler
        }

        val nowTick = player.tickCount
        trimPendingThrows(nowTick)
        pendingThrows.addLast(
            PendingThrow(
                throwPosition = player.position(),
                tick = nowTick,
                dimension = world.dimension()
            )
        )
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (!isOverworld()) {
            return@handler
        }

        when (val packet = event.packet) {
            is ClientboundAddEntityPacket -> mc.execute {
                handleEyeSpawnPacket(packet)
            }

            is ClientboundBlockUpdatePacket -> mc.execute {
                trackPortalBlock(packet.pos, packet.blockState.block)
            }

            is ClientboundSectionBlocksUpdatePacket -> mc.execute {
                packet.runUpdates { pos, state ->
                    trackPortalBlock(pos, state.block)
                }
            }

            is ClientboundLevelChunkWithLightPacket -> mc.execute {
                scanChunkForPortalBlocks(packet.x, packet.z)
            }

            is ClientboundForgetLevelChunkPacket -> mc.execute {
                removePortalBlocksInChunk(packet.pos)
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (!isOverworld()) {
            return@handler
        }

        val nowTick = player.tickCount
        trimPendingThrows(nowTick)

        val trackedIterator = trackedEyes.int2ObjectEntrySet().iterator()
        while (trackedIterator.hasNext()) {
            val entry = trackedIterator.next()
            val entityId = entry.intKey
            val trackedEye = entry.value

            if (nowTick - trackedEye.spawnTick < sampleDelayTicks) {
                continue
            }

            val eye = world.getEntity(entityId) as? EyeOfEnder ?: run {
                trackedIterator.remove()
                continue
            }

            if (eye.deltaMovement.horizontalDistance().toFloat() < minEyeHorizontalSpeed) {
                continue
            }

            val throwPos = trackedEye.throwPosition
            val eyePos = eye.position()
            val yaw = eyePos.subtract(throwPos).yaw

            measurements += EyeMeasurement(
                throwPos,
                angleDeg = yaw,
                tick = nowTick
            )

            trackedIterator.remove()

            notification(
                name,
                message("sampleCaptured", measurements.size),
                NotificationEvent.Severity.INFO
            )

            recomputePosterior(announce = true)
        }
    }

    @Suppress("unused")
    private val render3DHandler = handler<WorldRenderEvent> { event ->
        if (!isOverworld()) {
            return@handler
        }

        renderEnvironmentForWorld(event.matrixStack) {
            if (detectedPortalBlocks.isNotEmpty()) {
                renderDetectedPortalBlocks(event)
                return@renderEnvironmentForWorld
            }

            if (renderRays) {
                val color = Color4b.WHITE.alpha(170).argb
                for (measurement in measurements) {
                    val start = measurement.throwPos
                    val direction = Vec3.directionFromRotation(0f, measurement.angleDeg)
                    val end = measurement.throwPos.add(direction.scale(RAY_RENDER_LENGTH))

                    drawLine(
                        relativeToCamera(start).toVec3f(),
                        relativeToCamera(end).toVec3f(),
                        color,
                    )
                }
            }

            val snapshot = posterior ?: return@renderEnvironmentForWorld
            val drawY = player.interpolateCurrentPosition(event.partialTicks).y
            val candidates = snapshot.candidates.take(showTopCandidates)
            candidates.forEachIndexed { index, candidate ->
                val chunkPos = candidate.chunkPos
                val minX = chunkPos.minBlockX
                val minZ = chunkPos.minBlockZ
                val alpha = (45 + candidate.probability * 170).toInt().coerceIn(30, 200)

                val color = if (index == 0) {
                    Color4b(0, 170, 255, alpha)
                } else {
                    Color4b(255, 170, 0, alpha)
                }

                if ((index == 0 && renderBestChunk) || (index > 0 && renderTopChunks)) {
                    withPositionRelativeToCamera(minX.toDouble(), drawY, minZ.toDouble()) {
                        drawPlane(16f, 16f, color, color.darker())
                    }
                }
            }
        }
    }

    @Suppress("unused")
    private val renderOverlayHandler = handler<OverlayRenderEvent> { event ->
        if (!isOverworld()) {
            return@handler
        }

        if (detectedPortalBlocks.isNotEmpty()) {
            return@handler
        }

        val snapshot = posterior ?: return@handler
        val best = snapshot.candidates.firstOrNull() ?: return@handler
        val bestChunk = best.chunkPos

        val lines = arrayOf(
            this.name,
            "Samples: ${snapshot.sampleCount} | Sigma: ${sigma.toFixed(3)}°",
            "Best chunk: ${bestChunk.x}, ${bestChunk.z} (${(best.probability * 100.0).toFixed(1)}%)",
            "/tp ${bestChunk.middleBlockX} ~ ${bestChunk.middleBlockZ}",
        )

        val centerX = mc.window.guiScaledWidth / 2
        val startY = mc.window.guiScaledHeight / 2 + 10

        lines.forEachIndexed { index, line ->
            val lineX = centerX - mc.font.width(line) / 2
            event.context.text(
                mc.font,
                line,
                lineX,
                startY + index * (mc.font.lineHeight + 1),
                Color4b.WHITE.argb,
            )
        }
    }

    private fun getOrCreateHypotheses(): List<StrongholdHypothesis> {
        if (cachedHypothesisCount != hypothesisCount || hypothesisCache.isEmpty()) {
            hypothesisCache = StrongholdHypothesisGenerator.generate(hypothesisCount)
            cachedHypothesisCount = hypothesisCount
        }
        return hypothesisCache
    }

    private fun recomputePosterior(announce: Boolean) {
        posterior = StrongholdBayesianEstimator.estimate(
            measurements = measurements,
            hypotheses = getOrCreateHypotheses(),
            sigmaDeg = sigma.toDouble(),
            requireSameStrongholdAcrossThrows = requireSameStrongholdAcrossThrows,
            topCandidates = showTopCandidates,
        )

        val best = posterior?.candidates?.firstOrNull() ?: return
        val bestChunk = best.chunkPos

        if (announcePrediction && announce && bestChunk != lastAnnouncedCandidate) {
            notification(
                name,
                message("bestChunk", bestChunk.x, bestChunk.z, (best.probability * 100.0).toFixed(1)),
                NotificationEvent.Severity.INFO
            )
            lastAnnouncedCandidate = bestChunk
        }
    }

    private fun onEstimatorSettingsChanged() {
        if (measurements.isNotEmpty()) {
            recomputePosterior(announce = false)
        }
    }

    private fun trimPendingThrows(nowTick: Int) {
        while (pendingThrows.firstOrNull()?.let { nowTick - it.tick > maxSampleAgeTicks } == true) {
            pendingThrows.removeFirst()
        }
    }

    private fun handleEyeSpawnPacket(packet: ClientboundAddEntityPacket) {
        if (packet.type != EntityTypes.EYE_OF_ENDER) {
            return
        }

        val nowTick = player.tickCount
        trimPendingThrows(nowTick)

        val pending = pendingThrows
            .filter { it.dimension == world.dimension() && nowTick - it.tick in 0..maxSampleAgeTicks }
            .minWithOrNull(
                compareBy<PendingThrow> { nowTick - it.tick }
                    .thenComparingDouble {
                        it.throwPosition.horizontalDistanceToSqr(packet.x, packet.z)
                    }
            ) ?: return

        pendingThrows.remove(pending)
        val trackedEye = TrackedEye(
            entityId = packet.id,
            throwPosition = pending.throwPosition,
            spawnTick = nowTick
        )
        trackedEyes.put(packet.id, trackedEye)
    }

    private fun trackPortalBlock(pos: BlockPos, block: Block) {
        when (block) {
            Blocks.END_PORTAL -> detectedPortalBlocks[pos.immutable] = PortalBlockType.Portal
            Blocks.END_PORTAL_FRAME -> detectedPortalBlocks[pos.immutable] = PortalBlockType.Frame
            else -> detectedPortalBlocks.remove(pos)
        }
    }

    private fun scanChunkForPortalBlocks(chunkX: Int, chunkZ: Int) {
        val chunk = world.getChunk(chunkX, chunkZ)
        removePortalBlocksInChunk(chunk.pos)

        for (sectionIndex in 0..chunk.highestFilledSectionIndex) {
            chunk.forEachSectionBlock(sectionIndex) { pos, state ->
                when (state.block) {
                    Blocks.END_PORTAL -> detectedPortalBlocks[pos.immutable] = PortalBlockType.Portal
                    Blocks.END_PORTAL_FRAME -> detectedPortalBlocks[pos.immutable] = PortalBlockType.Frame
                }
            }
        }
    }

    private fun removePortalBlocksInChunk(chunkPos: ChunkPos) {
        detectedPortalBlocks.keys.removeIf(chunkPos::contains)
    }

    private fun WorldRenderEnvironment.renderDetectedPortalBlocks(event: WorldRenderEvent) {
        detectedPortalBlocks.forEach { (pos, type) ->
            withPositionRelativeToCamera(pos.toVec3d(yOffset = 0.01)) {
                drawPlane(1f, 1f, type.color, type.color.darker())
            }
        }

        val playerPos = player.interpolateCurrentPosition(event.partialTicks)
        val closestPortalPos = detectedPortalBlocks.keys.minByOrNull { pos ->
            pos.distToCenterSqr(playerPos)
        } ?: return

        val start = playerPos.add(0.0, 0.05, 0.0)
        val target = closestPortalPos.center

        val lineColor = Color4b(255, 80, 80, 220).argb
        val startRelative = relativeToCamera(start).toVec3f()

        drawLine(startRelative, relativeToCamera(target).toVec3f(), lineColor)

        val deltaX = target.x - start.x
        val deltaZ = target.z - start.z
        val horizontalLength = hypot(deltaX, deltaZ)
        if (horizontalLength > 1e-6) {
            val markerEnd = Vec3(
                start.x + deltaX / horizontalLength * 2.0,
                start.y,
                start.z + deltaZ / horizontalLength * 2.0
            )
            drawLine(startRelative, relativeToCamera(markerEnd).toVec3f(), lineColor)
        }
    }

    private fun resetState() {
        pendingThrows.clear()
        trackedEyes.clear()
        measurements.clear()
        posterior = null
        lastAnnouncedCandidate = null
        detectedPortalBlocks.clear()
    }

    private fun isOverworld(): Boolean {
        return world.dimension() == Level.OVERWORLD
    }

    private data class PendingThrow(
        val throwPosition: Vec3,
        val tick: Int,
        val dimension: ResourceKey<Level>,
    )

    private data class TrackedEye(
        val entityId: Int,
        val throwPosition: Vec3,
        val spawnTick: Int,
    )

    private enum class PortalBlockType(val color: Color4b) {
        Portal(Color4b(0, 220, 255, 170)),
        Frame(Color4b(255, 215, 0, 170)),
    }
}
