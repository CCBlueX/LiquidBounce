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
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.addVertex
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.drawPlane
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.longLines
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.toDegrees
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.math.toFixed
import net.ccbluex.liquidbounce.utils.world.stronghold.EyeMeasurement
import net.ccbluex.liquidbounce.utils.world.stronghold.PosteriorCandidate
import net.ccbluex.liquidbounce.utils.world.stronghold.PosteriorSnapshot
import net.ccbluex.liquidbounce.utils.world.stronghold.StrongholdBayesianEstimator
import net.ccbluex.liquidbounce.utils.world.stronghold.StrongholdHypothesis
import net.ccbluex.liquidbounce.utils.world.stronghold.StrongholdHypothesisGenerator
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.EyeOfEnder
import net.minecraft.world.item.Items
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val RAY_RENDER_LENGTH = 2048.0

/**
 * Stronghold finder module.
 *
 * Automatically tracks Eye of Ender throws and estimates the strongest stronghold chunk candidate
 * using a Bayesian posterior.
 *
 * [Article](https://github.com/Ninjabrain1/Ninjabrain-Bot/blob/main/triangulation.pdf)
 */
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
    private val trackedEyes = linkedMapOf<Int, TrackedEye>()
    private val measurements = mutableListOf<EyeMeasurement>()
    private var posterior: PosteriorSnapshot? = null
    private var lastAnnouncedCandidate: ChunkPos? = null

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

        val packet = event.packet as? ClientboundAddEntityPacket ?: return@handler
        if (packet.type != EntityType.EYE_OF_ENDER) {
            return@handler
        }

        val nowTick = player.tickCount
        trimPendingThrows(nowTick)

        val pending = pendingThrows
            .filter { it.dimension == world.dimension() && nowTick - it.tick in 0..maxSampleAgeTicks }
            .minWithOrNull(
                compareBy<PendingThrow> { nowTick - it.tick }
                    .thenBy {
                        val dx = it.throwPosition.x - packet.x
                        val dz = it.throwPosition.z - packet.z
                        dx * dx + dz * dz
                    }
            ) ?: return@handler

        pendingThrows.remove(pending)
        trackedEyes[packet.id] = TrackedEye(
            entityId = packet.id,
            throwPosition = pending.throwPosition,
            spawnTick = nowTick
        )
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (!isOverworld()) {
            return@handler
        }

        val nowTick = player.tickCount
        trimPendingThrows(nowTick)

        val trackedIterator = trackedEyes.iterator()
        while (trackedIterator.hasNext()) {
            val (entityId, trackedEye) = trackedIterator.next()

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
            val yaw = vectorToYaw(eyePos.x - throwPos.x, eyePos.z - throwPos.z)

            measurements += EyeMeasurement(
                throwX = throwPos.x,
                throwY = throwPos.y,
                throwZ = throwPos.z,
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
            if (renderRays) {
                withPositionRelativeToCamera {
                    longLines {
                        val color = Color4b.WHITE.alpha(170).argb
                        drawCustomMesh(ClientRenderPipelines.Lines) { pose ->
                            for (measurement in measurements) {
                                val start = Vec3(measurement.throwX, measurement.throwY, measurement.throwZ)
                                val yawRad = measurement.angleDeg.toDouble().toRadians()
                                val direction = Vec3(-sin(yawRad), 0.0, cos(yawRad))
                                val end = Vec3(
                                    measurement.throwX,
                                    measurement.throwY,
                                    measurement.throwZ
                                ).add(direction.scale(RAY_RENDER_LENGTH))
                                addVertex(pose, start).setColor(color)
                                addVertex(pose, end).setColor(color)
                            }
                        }
                    }
                }
            }

            val snapshot = posterior ?: return@renderEnvironmentForWorld
            val drawY = player.y
            val candidates = snapshot.candidates.take(showTopCandidates)
            candidates.forEachIndexed { index, candidate ->
                val chunkPos = candidate.toChunkPos()
                val minX = chunkPos.minBlockX
                val minZ = chunkPos.minBlockZ
                val alpha = (45 + candidate.probability * 170).toInt().coerceIn(30, 200)

                val color = if (index == 0) {
                    Color4b(0, 170, 255, alpha)
                } else {
                    Color4b(255, 170, 0, alpha)
                }

                if ((index == 0 && renderBestChunk) || (index > 0 && renderTopChunks)) {
                    withPositionRelativeToCamera(Vec3(minX.toDouble(), drawY, minZ.toDouble())) {
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

        val snapshot = posterior ?: return@handler
        val best = snapshot.candidates.firstOrNull() ?: return@handler
        val bestChunk = best.toChunkPos()

        val lines = arrayOf(
            "StrongholdFinder",
            "Samples: ${snapshot.sampleCount} | Sigma: ${sigma.toFixed(3)}°",
            "Best chunk: ${bestChunk.x}, ${bestChunk.z} (${(best.probability * 100.0).toFixed(1)}%)"
        )

        val centerX = mc.window.guiScaledWidth / 2
        val startY = mc.window.guiScaledHeight / 2 + 10

        lines.forEachIndexed { index, line ->
            val lineX = centerX - mc.font.width(line) / 2
            event.context.drawString(
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
        val bestChunk = best.toChunkPos()

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

    private fun resetState() {
        pendingThrows.clear()
        trackedEyes.clear()
        measurements.clear()
        posterior = null
        lastAnnouncedCandidate = null
    }

    private fun isOverworld(): Boolean {
        return world.dimension() == Level.OVERWORLD
    }

    private fun vectorToYaw(dx: Double, dz: Double): Float {
        return Mth.wrapDegrees(atan2(dz, dx).toDegrees().toFloat() - 90f)
    }

    private fun PosteriorCandidate.toChunkPos(): ChunkPos {
        return ChunkPos(chunkX, chunkZ)
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
}
