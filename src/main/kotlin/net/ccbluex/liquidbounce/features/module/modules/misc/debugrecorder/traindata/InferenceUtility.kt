package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.traindata

import ai.djl.Model
import ai.djl.inference.Predictor
import ai.djl.ndarray.NDList
import ai.djl.ndarray.types.Shape
import ai.djl.translate.NoBatchifyTranslator
import ai.djl.translate.TranslateException
import ai.djl.translate.TranslatorContext
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandRemoteView.player
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandRemoteView.world
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.phys.Vec3
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

object InferenceUtility: EventListener {

    private const val downsampleFactor = 4
    private const val minContextFrames = 10
    private const val defaultMaxSeqLen = 64
    private const val mapSize = 15 * 15
    private const val maxStoredFramesPerPlayer = 128

    private const val toleranceX = 0.3
    private const val toleranceY = 0.8
    private const val toleranceZ = 3.0

    private val swingingPlayers = ConcurrentHashMap<UUID, Boolean>()
    private val damagedPlayers = ConcurrentHashMap<UUID, Boolean>()
    private val playerStates = ConcurrentHashMap<UUID, PlayerStreamState>()

    private var timestamp = 0L

    private val cache = LinkedHashMap<UUID, List<List<PossibleHitPosition>>>()

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        this.cache.clear()

        collectForAllPlayers(world, player, timestamp)

        timestamp++
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is ClientboundAnimatePacket && (packet.action == 0 || packet.action == 3)) {
            val entity = world.getEntity(packet.id)
            if (entity is Player && entity != player) {
                recordSwing(entity.uuid)
            }
        } else if (packet is ClientboundDamageEventPacket) {
            val entity = world?.getEntity(packet.entityId())
            if (entity is Player && entity != player) {
                recordDamage(entity.uuid)
            }
        }
    }

    fun getPredictionCached(uuid: UUID, Y: Int): List<PossibleHitPosition> {
        val cachedPredictions = cache.computeIfAbsent(uuid) {
            inferPossibleHitPositions(uuid, gamemode = 0)
        }

        return cachedPredictions[Y.coerceIn(1, 15) - 1]
    }

    private val modelLock = Any()
    @Volatile
    private var loadedModel: LoadedModel? = null

    data class PreparedModelInput(
        val playerId: UUID,
        val sequenceLength: Int,
        val contextLength: Int,
        val endpointPosX: Double,
        val endpointPosY: Double,
        val endpointPosZ: Double,
        val endpointYaw: Float,
        val floorMaps: FloatArray,
        val ceilMaps: FloatArray,
        val poiMaps: LongArray,
        val scalarFeatures: FloatArray,
        val mainHand: LongArray,
        val offHand: LongArray,
        val yHorizons: LongArray,
        val gamemode: Long,
        val ctxLens: LongArray
    )

    data class PossibleHitPosition(
        val worldPos: Vec3,
        val probability: Double
    )

    private data class RawModelOutput(
        val piLogits: FloatArray,
        val mu: FloatArray,
        val sigma: FloatArray,
        val horizons: Int,
        val components: Int
    )

    private data class LoadedModel(
        val model: Model,
        val predictor: Predictor<PreparedModelInput, RawModelOutput>,
        val maxSeqLen: Int
    )

    private class PlayerStreamState {
        private val pendingWindow = PendingDownsampleWindow()
        private val currentSegment = ArrayDeque<DataSample>()
        private var lastDownsampled: DataSample? = null

        fun addRawSample(sample: DataSample) {
            val downsampled = pendingWindow.add(sample) ?: return
            val previous = lastDownsampled

            if (previous != null) {
                val dt = downsampled.timestamp - previous.timestamp
                val dx = downsampled.posX - previous.posX
                val dy = downsampled.posY - previous.posY
                val dz = downsampled.posZ - previous.posZ
                val step = sqrt(dx * dx + dy * dy + dz * dz)

                if (dt != downsampleFactor.toLong() || step > 7.0) {
                    currentSegment.clear()
                }
            }

            currentSegment.addLast(downsampled)
            if (currentSegment.size > maxStoredFramesPerPlayer) {
                currentSegment.removeFirst()
            }
            lastDownsampled = downsampled
        }

        fun latestContext(maxSeqLen: Int): List<DataSample>? {
            if (currentSegment.size < minContextFrames) {
                return null
            }

            val list = currentSegment.toList()
            return if (list.size <= maxSeqLen) {
                list
            } else {
                list.subList(list.size - maxSeqLen, list.size)
            }
        }

        fun clear() {
            pendingWindow.clear()
            currentSegment.clear()
            lastDownsampled = null
        }
    }

    private class PendingDownsampleWindow {
        private var count = 0
        private var last: DataSample? = null
        private var swingingOr = false
        private var hitOr = false

        fun add(sample: DataSample): DataSample? {
            count++
            last = sample
            swingingOr = swingingOr || sample.isSwinging
            hitOr = hitOr || sample.wasHit

            if (count < downsampleFactor) {
                return null
            }

            val base = last ?: return null
            val aggregated = base.copy(
                isSwinging = swingingOr,
                wasHit = hitOr
            )

            clear()
            return aggregated
        }

        fun clear() {
            count = 0
            last = null
            swingingOr = false
            hitOr = false
        }
    }

    fun recordSwing(playerId: UUID) {
        swingingPlayers[playerId] = true
    }

    fun recordDamage(playerId: UUID) {
        damagedPlayers[playerId] = true
    }

    fun reset() {
        swingingPlayers.clear()
        damagedPlayers.clear()
        playerStates.values.forEach(PlayerStreamState::clear)
        playerStates.clear()
    }

    fun collectForAllPlayers(level: ClientLevel, localPlayer: Player, timestamp: Long) {
        for (target in level.players()) {
            if (target == localPlayer || target.isSpectator || target.isInvisible || !target.shouldBeAttacked()) {
                continue
            }

            val sample = createSample(level, target, timestamp)
            val state = playerStates.computeIfAbsent(target.uuid) { PlayerStreamState() }
            state.addRawSample(sample)
        }
    }

    fun prepareModelInput(
        playerId: UUID,
        horizons: IntArray = (1..15).toList().toIntArray(),
        gamemode: Int = 0
    ): PreparedModelInput? {
        val state = playerStates[playerId] ?: return null
        val maxSeqLen = loadedModel?.maxSeqLen ?: readMaxSeqLenFromMetadata()
        val context = state.latestContext(maxSeqLen) ?: return null

        val endpoint = context.last()
        val seqLen = context.size
        val horizonValues = clampHorizons(horizons)

        val floorMaps = FloatArray(seqLen * mapSize)
        val ceilMaps = FloatArray(seqLen * mapSize)
        val poiMaps = LongArray(seqLen * mapSize)
        val scalarFeatures = FloatArray(seqLen * 25)
        val mainHand = LongArray(seqLen)
        val offHand = LongArray(seqLen)

        var previousViewDx = 0.0
        var previousViewDy = 0.0
        var previousViewDz = 0.0

        for (index in context.indices) {
            val sample = context[index]
            val prev = if (index > 0) context[index - 1] else null

            val rawDx = if (prev == null) 0.0 else sample.posX - prev.posX
            val rawDy = if (prev == null) 0.0 else sample.posY - prev.posY
            val rawDz = if (prev == null) 0.0 else sample.posZ - prev.posZ

            val viewDelta = rotateToView(rawDx, rawDz, sample.yaw.toDouble())
            val viewDx = viewDelta.first
            val viewDy = rawDy
            val viewDz = viewDelta.second

            val dYaw = if (prev == null) 0.0 else wrapToPi(sample.yaw.toDouble() - prev.yaw.toDouble())
            val dPitch = if (prev == null) 0.0 else sample.pitch.toDouble() - prev.pitch.toDouble()
            val velocity = sqrt(rawDx * rawDx + rawDy * rawDy + rawDz * rawDz)

            val arrowDx = sample.closestArrowX - sample.posX
            val arrowDy = sample.closestArrowY - sample.posY
            val arrowDz = sample.closestArrowZ - sample.posZ
            val arrowDist = sqrt(arrowDx * arrowDx + arrowDy * arrowDy + arrowDz * arrowDz)

            val arrDirX: Double
            val arrDirY: Double
            val arrDirZ: Double
            val arrowDistanceFeature: Double

            if (arrowDist < 0.01) {
                arrDirX = 0.0
                arrDirY = 0.0
                arrDirZ = 0.0
                arrowDistanceFeature = 0.0
            } else {
                val nx = arrowDx / arrowDist
                val nz = arrowDz / arrowDist
                val rotatedArrowDir = rotateToView(nx, nz, sample.yaw.toDouble())
                arrDirX = rotatedArrowDir.first
                arrDirY = arrowDy / arrowDist
                arrDirZ = rotatedArrowDir.second
                arrowDistanceFeature = arrowDist
            }

            val lastViewDx = if (index == 0) 0.0 else previousViewDx
            val lastDy = if (index == 0) 0.0 else previousViewDy
            val lastViewDz = if (index == 0) 0.0 else previousViewDz

            val accX = if (index == 0) 0.0 else viewDx - previousViewDx
            val accY = if (index == 0) 0.0 else viewDy - previousViewDy
            val accZ = if (index == 0) 0.0 else viewDz - previousViewDz

            previousViewDx = viewDx
            previousViewDy = viewDy
            previousViewDz = viewDz

            val scalarOffset = index * 25
            scalarFeatures[scalarOffset] = viewDx.toFloat()
            scalarFeatures[scalarOffset + 1] = viewDy.toFloat()
            scalarFeatures[scalarOffset + 2] = viewDz.toFloat()
            scalarFeatures[scalarOffset + 3] = lastViewDx.toFloat()
            scalarFeatures[scalarOffset + 4] = lastDy.toFloat()
            scalarFeatures[scalarOffset + 5] = lastViewDz.toFloat()
            scalarFeatures[scalarOffset + 6] = velocity.toFloat()
            scalarFeatures[scalarOffset + 7] = dYaw.toFloat()
            scalarFeatures[scalarOffset + 8] = dPitch.toFloat()
            scalarFeatures[scalarOffset + 9] = sin(sample.yaw.toDouble()).toFloat()
            scalarFeatures[scalarOffset + 10] = cos(sample.yaw.toDouble()).toFloat()
            scalarFeatures[scalarOffset + 11] = sin(sample.pitch.toDouble()).toFloat()
            scalarFeatures[scalarOffset + 12] = cos(sample.pitch.toDouble()).toFloat()
            scalarFeatures[scalarOffset + 13] = toBinaryFloat(sample.isSneaking)
            scalarFeatures[scalarOffset + 14] = toBinaryFloat(sample.isUsingItem)
            scalarFeatures[scalarOffset + 15] = toBinaryFloat(sample.isOnGround)
            scalarFeatures[scalarOffset + 16] = toBinaryFloat(sample.wasHit)
            scalarFeatures[scalarOffset + 17] = toBinaryFloat(sample.isSwinging)
            scalarFeatures[scalarOffset + 18] = arrDirX.toFloat()
            scalarFeatures[scalarOffset + 19] = arrDirY.toFloat()
            scalarFeatures[scalarOffset + 20] = arrDirZ.toFloat()
            scalarFeatures[scalarOffset + 21] = arrowDistanceFeature.toFloat()
            scalarFeatures[scalarOffset + 22] = accX.toFloat()
            scalarFeatures[scalarOffset + 23] = accY.toFloat()
            scalarFeatures[scalarOffset + 24] = accZ.toFloat()

            val mapOffset = index * mapSize
            for (cell in 0 until mapSize) {
                floorMaps[mapOffset + cell] = sample.floorMap[cell].toFloat() / 256.0f
                ceilMaps[mapOffset + cell] = sample.ceilMap[cell].toFloat() / 256.0f
                poiMaps[mapOffset + cell] = sample.poiMap[cell].coerceIn(0, 19).toLong()
            }

            mainHand[index] = sample.mainHandCategory.coerceIn(0, 15).toLong()
            offHand[index] = sample.offHandCategory.coerceIn(0, 15).toLong()
        }

        return PreparedModelInput(
            playerId = playerId,
            sequenceLength = seqLen,
            contextLength = seqLen,
            endpointPosX = endpoint.posX,
            endpointPosY = endpoint.posY,
            endpointPosZ = endpoint.posZ,
            endpointYaw = endpoint.yaw,
            floorMaps = floorMaps,
            ceilMaps = ceilMaps,
            poiMaps = poiMaps,
            scalarFeatures = scalarFeatures,
            mainHand = mainHand,
            offHand = offHand,
            yHorizons = horizonValues,
            gamemode = gamemode.toLong(),
            ctxLens = longArrayOf(seqLen.toLong())
        )
    }

    @Throws(TranslateException::class)
    fun inferPossibleHitPositions(
        preparedModelInput: PreparedModelInput
    ): List<List<PossibleHitPosition>> {
        val model = ensureModelLoaded()
        val output = model.predictor.predict(preparedModelInput)

        val resultsPerHorizon = ArrayList<List<PossibleHitPosition>>()

        for (horizonIdx in 0 until output.horizons) {
            val results = ArrayList<PossibleHitPosition>(output.horizons * output.components)

            val weights = softmax(output.piLogits, horizonIdx, output.components)

            for (component in 0 until output.components) {
                val base = (horizonIdx * output.components + component) * 3

                val muX = output.mu[base].toDouble()
                val muY = output.mu[base + 1].toDouble()
                val muZ = output.mu[base + 2].toDouble()

                val sigmaX = output.sigma[base].toDouble()
                val sigmaY = output.sigma[base + 1].toDouble()
                val sigmaZ = output.sigma[base + 2].toDouble()

                val worldDelta = rotateFromView(muX, muZ, preparedModelInput.endpointYaw.toDouble())
                val worldX = preparedModelInput.endpointPosX + worldDelta.first
                val worldY = preparedModelInput.endpointPosY + muY
                val worldZ = preparedModelInput.endpointPosZ + worldDelta.second

                val probability = weights[component] *
                    axisProbability(toleranceX, sigmaX) *
                    axisProbability(toleranceY, sigmaY) *
                    axisProbability(toleranceZ, sigmaZ)

                results += PossibleHitPosition(
                    worldPos = Vec3(worldX, worldY, worldZ),
                    probability = probability
                )
            }

            resultsPerHorizon.add(results)
        }

        return resultsPerHorizon
    }

    @Throws(TranslateException::class)
    fun inferPossibleHitPositions(
        playerId: UUID,
        horizons: IntArray = (1..15).toList().toIntArray(),
        gamemode: Int = 0
    ): List<List<PossibleHitPosition>> {
        val input = prepareModelInput(playerId, horizons, gamemode) ?: return emptyList()
        return inferPossibleHitPositions(input)
    }

    fun unloadModel() {
        synchronized(modelLock) {
            loadedModel?.predictor?.close()
            loadedModel?.model?.close()
            loadedModel = null
        }
    }

    private fun ensureModelLoaded(): LoadedModel {
        loadedModel?.let { return it }

        synchronized(modelLock) {
            loadedModel?.let { return it }

            require(DeepLearningEngine.isInitialized) { "DeepLearningEngine is not initialized" }

            val modelFolder = ConfigSystem.rootFolder.resolve("djl-prediction-model")
            require(modelFolder.exists()) { "Missing model folder: ${modelFolder.absolutePath}" }

            val maxSeqLen = readMaxSeqLenFromMetadata()
            val model = Model.newInstance("djl-prediction-model")
            model.load(modelFolder.toPath(), "djl_model")
            val predictor = model.newPredictor(PredictionTranslator())

            return LoadedModel(model, predictor, maxSeqLen).also {
                loadedModel = it
            }
        }
    }

    private fun readMaxSeqLenFromMetadata(): Int {
        val metadataFile = ConfigSystem.rootFolder.resolve("djl-prediction-model/djl_metadata.json")
        if (!metadataFile.exists()) {
            return defaultMaxSeqLen
        }

        return runCatching {
            metadataFile.bufferedReader().use { reader ->
                val root = JsonParser.parseReader(reader).asJsonObject
                root.getAsJsonObject("config").get("max_seq_len").asInt
            }
        }.getOrDefault(defaultMaxSeqLen)
    }

    private fun createSample(level: ClientLevel, player: Player, timestamp: Long): DataSample {
        val isSwinging = swingingPlayers.getOrDefault(player.uuid, false)
        val wasHit = damagedPlayers.getOrDefault(player.uuid, false) ||
            (player.hurtTime == player.hurtDuration && player.hurtDuration > 0)

        swingingPlayers[player.uuid] = false
        damagedPlayers[player.uuid] = false

        val mapper = SurroundingsMapper(player, level).apply { compute() }

        var closestArrowDistanceSq = Double.MAX_VALUE
        var closestArrowX = 0.0
        var closestArrowY = 0.0
        var closestArrowZ = 0.0

        for (entity in level.entitiesForRendering()) {
            if (entity is AbstractArrow && !entity.isInGround) {
                val distSq = entity.distanceToSqr(player)
                if (distSq < closestArrowDistanceSq) {
                    closestArrowDistanceSq = distSq
                    closestArrowX = entity.x
                    closestArrowY = entity.y
                    closestArrowZ = entity.z
                }
            }
        }

        return DataSample(
            timestamp = timestamp,
            playerId = player.uuid.toString(),
            posX = player.x,
            posY = player.y,
            posZ = player.z,
            yaw = Math.toRadians(player.yRot.toDouble()).toFloat(),
            pitch = Math.toRadians(player.xRot.toDouble()).toFloat(),
            isSneaking = player.isShiftKeyDown,
            isOnGround = player.onGround(),
            isUsingItem = player.isUsingItem,
            isSwinging = isSwinging,
            wasHit = wasHit,
            closestArrowX = closestArrowX,
            closestArrowY = closestArrowY,
            closestArrowZ = closestArrowZ,
            mainHandCategory = ItemCategoryProvider.getCategory(player.mainHandItem),
            offHandCategory = ItemCategoryProvider.getCategory(player.offhandItem),
            floorMap = mapper.floorMap,
            ceilMap = mapper.ceilMap,
            poiMap = mapper.poiMap
        )
    }

    private fun clampHorizons(horizons: IntArray): LongArray {
        if (horizons.isEmpty()) {
            return (1..15).map { it.toLong() }.toLongArray()
        }

        return horizons
            .map { it.coerceIn(1, 15).toLong() }
            .distinct()
            .sorted()
            .toLongArray()
    }

    private fun rotateToView(dx: Double, dz: Double, yaw: Double): Pair<Double, Double> {
        val cos = cos(-yaw)
        val sin = sin(-yaw)
        val rx = dx * cos - dz * sin
        val rz = dx * sin + dz * cos
        return rx to rz
    }

    private fun rotateFromView(viewX: Double, viewZ: Double, yaw: Double): Pair<Double, Double> {
        val cos = cos(yaw)
        val sin = sin(yaw)
        val worldX = viewX * cos - viewZ * sin
        val worldZ = viewX * sin + viewZ * cos
        return worldX to worldZ
    }

    private fun wrapToPi(value: Double): Double {
        var wrapped = value
        while (wrapped > PI) {
            wrapped -= 2.0 * PI
        }
        while (wrapped < -PI) {
            wrapped += 2.0 * PI
        }
        return wrapped
    }

    private fun toBinaryFloat(value: Boolean): Float = if (value) 1.0f else 0.0f

    private fun softmax(logits: FloatArray, horizonIndex: Int, components: Int): DoubleArray {
        val offset = horizonIndex * components
        var maxLogit = Double.NEGATIVE_INFINITY

        for (component in 0 until components) {
            val value = logits[offset + component].toDouble()
            if (value > maxLogit) {
                maxLogit = value
            }
        }

        val exponentials = DoubleArray(components)
        var sum = 0.0
        for (component in 0 until components) {
            val expValue = exp(logits[offset + component] - maxLogit)
            exponentials[component] = expValue
            sum += expValue
        }

        if (sum <= 0.0) {
            return DoubleArray(components) { 1.0 / components }
        }

        for (component in 0 until components) {
            exponentials[component] /= sum
        }

        return exponentials
    }

    private fun axisProbability(tolerance: Double, sigma: Double): Double {
        val safeSigma = maxOf(sigma, 1e-6)
        val value = tolerance / (sqrt(2.0) * safeSigma)
        return erf(value).coerceIn(0.0, 1.0)
    }

    private fun erf(x: Double): Double {
        val value = abs(x)
        val t = 1.0 / (1.0 + 0.3275911 * value)
        val polynomial = (((((1.061405429 * t - 1.453152027) * t + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t)
        val result = 1.0 - polynomial * exp(-value * value)
        return sign(x) * result
    }

    private class PredictionTranslator : NoBatchifyTranslator<PreparedModelInput, RawModelOutput> {

        override fun processInput(ctx: TranslatorContext, input: PreparedModelInput): NDList {
            val manager = ctx.ndManager
            val t = input.sequenceLength.toLong()
            val l = input.yHorizons.size.toLong()

            return NDList(
                manager.create(input.floorMaps, Shape(1, t, 15, 15)),
                manager.create(input.ceilMaps, Shape(1, t, 15, 15)),
                manager.create(input.poiMaps, Shape(1, t, 15, 15)),
                manager.create(input.scalarFeatures, Shape(1, t, 25)),
                manager.create(input.mainHand, Shape(1, t)),
                manager.create(input.offHand, Shape(1, t)),
                manager.create(input.yHorizons, Shape(1, l)),
                manager.create(longArrayOf(input.gamemode), Shape(1)),
                manager.create(input.ctxLens, Shape(1))
            )
        }

        override fun processOutput(ctx: TranslatorContext, list: NDList): RawModelOutput {
            require(list.size >= 3) { "Expected 3 output tensors but got ${list.size}" }

            val pi = list[0]
            val mu = list[1]
            val sigma = list[2]

            val piShape = pi.shape
            val horizons = piShape[1].toInt()
            val components = piShape[2].toInt()

            return RawModelOutput(
                piLogits = pi.toFloatArray(),
                mu = mu.toFloatArray(),
                sigma = sigma.toFloatArray(),
                horizons = horizons,
                components = components
            )
        }
    }
}
