@file:Suppress("detekt:all")
package net.ccbluex.liquidbounce.features.module.modules.player

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerInteractedItemEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAirJump
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withDisabledCull
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.raycast
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.VoidFallPrediction
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.getRandomInRange
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectorySegments
import net.minecraft.block.Blocks
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.entity.projectile.thrown.EnderPearlEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Util
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * By KotlinModule With ChatGPT4.0 And Gork3/4
 */

object ModuleAutoClutch : ClientModule("AutoClutch", Category.PLAYER) {

    enum class State { IDLE, FINDING_PEARL, CALCULATING, ROTATING, THROWING, PAUSED }
    enum class Algorithm(override val choiceName: String) : NamedChoice {
        SimulatedAnnealing("SimulatedAnnealing")
    }

    @Suppress("unused")
    private val algorithm by enumChoice("Algorithm", Algorithm.SimulatedAnnealing)

    private val voidFallPrediction = tree(VoidFallPrediction(this))
    private val adjacentSafeBlocks by int("AdjacentSafeBlocks", 0, 0..3)
    private val aimPrecision by float("AimPrecision", 0.1f, 0.1f..1f)
    private val pitchRange by floatRange("PitchLimit", -90f..0f, -90f..45f)
    private val pearlTrajectorySteps by int("PearlTrajectorySteps", 40, 20..100)
    private val postThrowCooldownTicks by int("PauseOnFinish", 0, 0..20, "ticks")
    private val allowClutchWithStuck by boolean("AllowClutchWithStuck", true)
    private val ensureCompleteSpace by boolean("EnsureCompleteSpace", true)
    private val onlyDuringCombat by boolean("OnlyDuringCombat", false)

    object PlayerTrajectory: ToggleableConfigurable(this,"PlayerTrajectory",false) {
        val trajectoryLength by int("TrajectoryLength", 30, 30..100)
        val securitySection by color("Security", Color4b.Companion.CYAN.withAlpha(75))
        val hazardSection by color("Hazard", Color4b.Companion.RED.withAlpha(75))
    }
    init {

        tree(PlayerTrajectory)
    }
    private val rotationConfig = tree(RotationsConfigurable(this))
    val unsafeBlocks by blocks("UnsafeBlocks", setOf(
        Blocks.LAVA,
        Blocks.COBWEB,
        Blocks.MAGMA_BLOCK,
        Blocks.CACTUS,
        Blocks.FIRE,
        Blocks.SOUL_FIRE,
        Blocks.SWEET_BERRY_BUSH,
        Blocks.WITHER_ROSE,
        Blocks.POWDER_SNOW,
        Blocks.POINTED_DRIPSTONE
    ).toMutableSet())

    class SimulatedAnnealing : Configurable("SimulatedAnnealing") {
        val maxIterations by int("MaxIterations", 2000, 50..10000)
        val stagnationLimit by int("StagnationLimit", 1337, 1000..10000)
        val iterationSpeed by float("IterationsSpeed", 5f, 1f..5f)
        val minTemperature by float("MinTemperature", 0.01f, 0.01f..0.1f)
        val initialTemperature by float("InitialTemp", 20f, 5f..50f)
        val temperatureDecayRate by float("TemperatureDecayRate", 0.97f, 0.80f..0.99f)
    }

    private val annealingConfig = SimulatedAnnealing()
    init {
        tree(annealingConfig)
    }

    var state = State.IDLE

    private val blockPositionScoreCache = ConcurrentHashMap<BlockPos, Double>()
    private var currentSolution = Rotation(0f, 0f)
    private var bestEnergy = Double.MAX_VALUE
    private var currentEnergy = Double.MAX_VALUE
    private var temperature = SimulatedAnnealing().initialTemperature
    private var iterations = 0
    private var safetyCheckCounter = 0
    private var iterationsWithoutImprovement = 0
    private var averageCalculationTimeSeconds = 0.1f
    private var pearlThrownTick: Long = 0L
    private var lastPearlThrowTime: Long = 0L
    private var lastTrajectoryUpdate: Long = 0L
    private var isPearlInFlight = false
    private var safetyCheckActive = false
    private var manualPearlThrown = false
    private var triggerPosition: Vec3d? = null
    private var bestSolution: Rotation? = null
    private var pearlSlot: HotbarItemSlot? = null
    private var lastPlayerPosition: Vec3d? = null
    private var predictedThrowPosition: Vec3d? = null
    private var cachedTrajectory: List<TrajectorySegments.TrajectorySegment>? = null
    private var lastPlayerState: Triple<Vec3d, Vec3d, DirectionalInput>? = null
    private val calculationComplete = AtomicBoolean(false)

    override val running: Boolean
        get() =
            super.running
                && !(ModuleAutoStuck.shouldActivate || ModuleAutoStuck.alwaysInVoid)
                && !(onlyDuringCombat && !CombatManager.isInCombat)
                && !ModuleScaffold.running
                && !ModuleFreeze.running
                && !ModuleAirJump.running
                && !ModuleFly.running

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        when (state) {
            State.CALCULATING -> if (calculationComplete.get()) {
                calculationComplete.set(false)
                state = State.ROTATING
            }
            else -> handleStateMachine()
        }
        checkPlayerMovement()
    }

    @Suppress("unused")
    private val interactedItemHandler = handler<PlayerInteractedItemEvent> { event ->
        if (event.actionResult != ActionResult.PASS &&
            event.player == mc.player &&
            event.hand == Hand.MAIN_HAND &&
            player.getStackInHand(event.hand).item == Items.ENDER_PEARL &&
            state != State.PAUSED
        ) {
            manualPearlThrown = true
            state = State.PAUSED
            scheduleSafetyCheck()
        }
    }

    @Suppress("unused")
    private val worldRenderHandler = handler<WorldRenderEvent> { event ->
        drawPlayerTrajectory(event.matrixStack)
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        if (!isPearlInFlight) return@handler
        if (event.packet is PlayerPositionLookS2CPacket) {
            isPearlInFlight = false
            pearlThrownTick = 0L
            notification(
                "AutoClutch",
                "Pearl landed, resuming calculations.",
                NotificationEvent.Severity.INFO
            )
            if (!voidFallPrediction.isPlayerSafe()) {
                triggerPosition = player.pos
                state = State.FINDING_PEARL
            } else {
                state = State.IDLE
            }
        }
    }

    private fun handleStateMachine() {
        if (isPearlInFlight) {
            state = State.PAUSED
            return
        }
        if (state == State.PAUSED) {
            safetyCheckCounter--
            if (safetyCheckCounter <= 0) {
                safetyCheckActive = false
                manualPearlThrown = false
                state = if (voidFallPrediction.isPlayerSafe()) {
                    State.IDLE
                } else {
                    triggerPosition = player.pos
                    State.FINDING_PEARL
                }
            }
            return
        }
        if (state == State.IDLE) {
            pruneCache()
        }
        if (safetyCheckActive) {
            safetyCheckCounter--
            if (safetyCheckCounter <= 0) {
                safetyCheckActive = false
                state = if (voidFallPrediction.isPlayerSafe()) {
                    State.IDLE
                } else {
                    triggerPosition = player.pos
                    State.FINDING_PEARL
                }
            }
            return
        }

        if (isPearlInFlight && pearlThrownTick > 100) {
            isPearlInFlight = false
            pearlThrownTick = 0L
            notification(
                "AutoClutch",
                "Pearl timed out, resuming calculations.",
                NotificationEvent.Severity.ERROR
            )
        }

        if (isPearlInFlight && state == State.FINDING_PEARL) {
            return
        }

        if (ModuleScaffold.enabled && ModuleScaffold.blockCount == 0) {
            ModuleScaffold.enabled = false
        }

        when (state) {
            State.IDLE -> checkActivationConditions()
            State.FINDING_PEARL -> findPearl()
            State.CALCULATING -> calculateSolution()
            State.ROTATING -> rotateToSolution()
            State.THROWING -> throwPearl()
            State.PAUSED -> {}
        }
    }

    private fun pruneCache() {
        while (blockPositionScoreCache.size > 2560) {
            blockPositionScoreCache.entries.take(100).forEach {
                blockPositionScoreCache.remove(it.key)
            }
        }
    }

    private fun checkActivationConditions() {
        if (System.currentTimeMillis() - lastPearlThrowTime < 1000L) return

        if (player.isCreative || player.isSpectator || player.isGliding || player.isSneaking) {
            resetAllVariables()
            return
        }

        if (voidFallPrediction.isPlayerSafe()
            || voidFallPrediction.canReachSafeBlockFrom()
            || voidFallPrediction.isBlockUnder(2.0)) {
            resetAllVariables()
            return
        }

        if (voidFallPrediction.isVoidFallImminent) {
            triggerPosition = player.pos
            state = State.FINDING_PEARL
            if (allowClutchWithStuck && !ModuleAutoStuck.enabled) {
                ModuleAutoStuck.enabled = true
            }
        }
    }

    private fun checkPlayerMovement() {
        val currentPos = player.pos
        lastPlayerPosition?.let { lastPos ->
            if (currentPos.distanceTo(lastPos) > 2.5) {
                resetAllVariables()
            }
        }
        lastPlayerPosition = currentPos
    }

    private fun drawPlayerTrajectory(matrixStack: MatrixStack) {
        if (!PlayerTrajectory.enabled) {
            clearTrajectoryAndCache()
            return
        }

        val camera = mc.entityRenderDispatcher.camera ?: return
        val currentPlayerState = Triple(
            player.pos,
            player.velocity,
            DirectionalInput(player.input)
        )
        val now = System.currentTimeMillis()

        val needsUpdate = cachedTrajectory == null
            || now - lastTrajectoryUpdate > 100
            || lastPlayerState?.let {
            it.first.distanceTo(currentPlayerState.first) > 0.5
                || it.second.distanceTo(currentPlayerState.second) > 0.1
        } != false

        if (needsUpdate) {
            lastTrajectoryUpdate = now
            lastPlayerState = currentPlayerState

            val cache = PlayerSimulationCache.getSimulationForLocalPlayer()
            val points = List(PlayerTrajectory.trajectoryLength) { tick ->
                val snapshot = cache.getSnapshotAt(tick)
                val isSafe = voidFallPrediction.canReachSafeBlockFrom()
                    && !voidFallPrediction.isInVoid(snapshot.pos)
                snapshot.pos to isSafe
            }

            cachedTrajectory = TrajectorySegments.generateTrajectorySegments(
                points,
                PlayerTrajectory.securitySection,
                PlayerTrajectory.hazardSection
            )
        }

        renderEnvironmentForWorld(matrixStack) {
            withDisabledCull {
                val matrix = matrixStack.peek().positionMatrix
                val buffer = RenderSystem.renderThreadTesselator()
                    .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)
                RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

                val camPos = camera.pos
                cachedTrajectory?.forEach { (start, end, color) ->
                    buffer.vertex(
                        matrix,
                        start.x - camPos.x.toFloat(),
                        start.y - camPos.y.toFloat(),
                        start.z - camPos.z.toFloat()
                    )
                        .color(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f)
                    buffer.vertex(
                        matrix,
                        end.x - camPos.x.toFloat(),
                        end.y - camPos.y.toFloat(),
                        end.z - camPos.z.toFloat()
                    )
                        .color(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f)
                }

                BufferRenderer.drawWithGlobalProgram(buffer.end())
            }
        }
    }


    private fun clearTrajectoryAndCache() {
        cachedTrajectory = null
        lastPlayerState = null
        lastTrajectoryUpdate = 0
        blockPositionScoreCache.clear()
    }


    private fun findPearl() {
        val startTime = System.currentTimeMillis()
        val pearlSlot = Slots.Hotbar
            .firstOrNull {
                it.itemStack.item == Items.ENDER_PEARL
            }
        if (pearlSlot !is HotbarItemSlot) {
            state = State.IDLE
            return
        }
        predictedThrowPosition = predictFuturePosition(averageCalculationTimeSeconds.toDouble())
        resetAnnealing()

        CompletableFuture.supplyAsync({
            calculateSolutionBackground()
            calculationComplete.set(true)
        }, Util.getMainWorkerExecutor()).exceptionally { e ->
            notification(
                "AutoClutch",
                "Calculation failed: ${e.message}",
                NotificationEvent.Severity.ERROR
            )
            state = State.IDLE
            null
        }
        state = State.CALCULATING
        val endTime = System.currentTimeMillis()
        averageCalculationTimeSeconds = (
            averageCalculationTimeSeconds * 0.9 + (endTime - startTime) / 1000.0 * 0.1).toFloat()
    }

    private fun calculateSolutionBackground() {
        resetAnnealing()
        val batchSize = 1000
        while (iterations < annealingConfig.maxIterations && temperature >= annealingConfig.minTemperature) {
            repeat(batchSize) {
                if (iterations >= annealingConfig.maxIterations ||
                    temperature < annealingConfig.minTemperature || bestEnergy < 1000.0) {
                    return@repeat
                }
                val newSolution = Rotation(
                    (currentSolution.yaw + getRandomInRange(-temperature * 18f, temperature * 18f)),
                    (currentSolution.pitch + getRandomInRange(-temperature * 9f, temperature * 9f)).coerceIn(pitchRange)
                )
                val newEnergy = assessRotation(newSolution)
                val delta = newEnergy - currentEnergy
                if (delta < 0 || Random.Default.nextDouble() < exp(-delta / temperature)) {
                    currentSolution = newSolution
                    currentEnergy = newEnergy
                    if (currentEnergy < bestEnergy) {
                        bestSolution = currentSolution
                        bestEnergy = currentEnergy
                        iterationsWithoutImprovement = 0
                    } else {
                        iterationsWithoutImprovement++
                        if (iterationsWithoutImprovement > annealingConfig.stagnationLimit && bestEnergy > 5000.0) {
                            state = State.IDLE
                            return
                        }
                    }
                }
                iterations++
            }
            temperature *= annealingConfig.temperatureDecayRate
        }
    }


    private fun predictFuturePosition(deltaTimeSeconds: Double): Vec3d {
        val deltaTicks = (deltaTimeSeconds * 20).toInt()
        val cache = PlayerSimulationCache.getSimulationForLocalPlayer()
        return if (deltaTicks < 30) {
            cache.getSnapshotAt(deltaTicks).pos
        } else {
            var futurePos = player.pos
            var futureVelocity = player.velocity
            repeat(deltaTicks) {
                futureVelocity = futureVelocity.add(0.0, -0.08, 0.0).multiply(0.99, 0.98, 0.99)
                futurePos = futurePos.add(futureVelocity)
            }
            futurePos
        }
    }

    private fun simulatePearlTrajectory(rotation: Rotation): Vec3d? {
        val predictedPos = predictedThrowPosition ?: player.pos
        val yawRad = Math.toRadians(rotation.yaw.toDouble())
        val pitchRad = Math.toRadians(rotation.pitch.toDouble())

        val trajectoryInfo = TrajectoryInfo.Companion.GENERIC
        val velocity = trajectoryInfo.initialVelocity
        var motion = Vec3d(
            -sin(yawRad) * cos(pitchRad) * velocity,
            -sin(pitchRad) * velocity,
            cos(yawRad) * cos(pitchRad) * velocity
        )
        if (trajectoryInfo.copiesPlayerVelocity) {
            motion = motion.add(player.velocity)
        }

        val pearlEntity = EnderPearlEntity(mc.world!!, player, player.getStackInHand(Hand.MAIN_HAND))
        var pos = Vec3d(predictedPos.x, predictedPos.y + player.standingEyeHeight, predictedPos.z)

        repeat(pearlTrajectorySteps) {
            val newPos = pos + motion
            val drag = if (!world.getBlockState(newPos.toBlockPos()).fluidState.isEmpty) {
                trajectoryInfo.dragInWater
            } else {
                trajectoryInfo.drag
            }
            motion = motion.multiply(drag, drag, drag)
            motion = motion.add(0.0, -trajectoryInfo.gravity, 0.0)

            val blockHitResult = world.raycast(
                RaycastContext(
                    pos,
                    newPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    pearlEntity
                ),
                exclude = null,
                include = null,
                maxBlastResistance = null
            )

            val entityHitResult = ProjectileUtil.getEntityCollision(
                mc.world!!,
                pearlEntity,
                pos,
                newPos,
                Box(pos, newPos).expand(trajectoryInfo.hitboxRadius)
            ) { entity ->
                entity.isAlive && !entity.isSpectator && entity.canHit()
                    && entity != player && !pearlEntity.isConnectedThroughVehicle(entity)
            }

            val blockPos = newPos.toBlockPos()
            val directions = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
            for (dir in directions) {
                val nearby = blockPos.offset(dir)
                val state = world.getBlockState(nearby)
                if (!state.isAir && state.isFullCube(world, nearby)) {
                    val collisionBox = state.getCollisionShape(world, nearby).boundingBox.offset(nearby)
                    if (collisionBox.contains(newPos)) {
                        break
                    }
                }
            }

            if (blockHitResult.type != HitResult.Type.MISS) {
                val hitPos = blockHitResult.pos
                val blockPos = blockHitResult.blockPos
                val state = world.getBlockState(blockPos)
                if (state.isFullCube(world, blockPos) || !state.getCollisionShape(world, blockPos).isEmpty) {
                    val direction = blockHitResult.side
                    val adjustedPos = hitPos.add(Vec3d.of(direction.vector).multiply(0.01))
                    return adjustedPos
                }
                return hitPos
            }

            if (entityHitResult != null && entityHitResult.type != HitResult.Type.MISS) {
                return entityHitResult.pos
            }

            pos = newPos
        }
        return pos
    }

    private fun resetAnnealing() {
        var bestInitialSolution = currentSolution
        var bestInitialEnergy = Double.MAX_VALUE
        repeat(5) {
            val initialSolution = Rotation(
                getRandomInRange(-180f, 180f),
                getRandomInRange(-90f, 90f)
            )
            val initialEnergy = assessRotation(initialSolution)
            if (initialEnergy < bestInitialEnergy) {
                bestInitialSolution = initialSolution
                bestInitialEnergy = initialEnergy
            }
        }
        currentSolution = bestInitialSolution
        currentEnergy = bestInitialEnergy
        bestSolution = currentSolution
        bestEnergy = currentEnergy
        temperature = SimulatedAnnealing().initialTemperature
        iterations = 0
        iterationsWithoutImprovement = 0
    }

    private fun calculateSolution() {
        val scaledTemperature = temperature * 100
        repeat((SimulatedAnnealing().maxIterations * SimulatedAnnealing().iterationSpeed).toInt()) {
            if (iterations >= SimulatedAnnealing().maxIterations || temperature < SimulatedAnnealing().minTemperature) {
                state = State.ROTATING
                return@repeat
            }
            val newSolution = Rotation((
                currentSolution.yaw + getRandomInRange(-temperature * 18f, temperature * 18f)),
                (currentSolution.pitch + getRandomInRange(-temperature * 9f, temperature * 9f)).coerceIn(pitchRange)
            )
            val newEnergy = assessRotation(newSolution)
            val deltaEnergy = newEnergy - currentEnergy
            if (deltaEnergy < 0 || getRandomInRange(0.0f, 1.0f) < exp(-deltaEnergy / temperature)) {
                currentSolution = newSolution
                currentEnergy = newEnergy
                if (currentEnergy < bestEnergy) {
                    bestSolution = currentSolution
                    bestEnergy = currentEnergy
                    iterationsWithoutImprovement = 0
                } else {
                    iterationsWithoutImprovement++
                    if (iterationsWithoutImprovement > SimulatedAnnealing().stagnationLimit) {
                        state = State.ROTATING
                        return@repeat
                    }
                }
            }
            temperature = (scaledTemperature * SimulatedAnnealing().temperatureDecayRate) / 100
            iterations++
        }
    }

    private fun assessRotation(rotation: Rotation): Double {
        val pearlPos = simulatePearlTrajectory(rotation) ?: return Double.MAX_VALUE
        return evaluateLandingPosition(pearlPos)
    }

    private fun evaluateLandingPosition(pos: Vec3d): Double {
        val blockPos = BlockPos(pos.x.toInt(), (pos.y - 0.5).toInt(), pos.z.toInt())
        val maxThrowDistance = 50.0
        val horizontalDistance = distanceSq2D(pos, triggerPosition ?: player.pos)
        if (horizontalDistance > maxThrowDistance * maxThrowDistance) {
            return Double.MAX_VALUE
        }
        val blockState = blockPos.getState()
        if (blockState != null) {
            if (blockState.isFullCube(world, blockPos) || !blockState.getCollisionShape(world, blockPos).isEmpty) {
                return Double.MAX_VALUE
            }
        }

        return blockPositionScoreCache.computeIfAbsent(blockPos) { _ ->
            val belowPos = blockPos.down()
            val belowState = world.getBlockState(belowPos)
            val hasGround = belowPos.canStandOn() && belowState.block !in unsafeBlocks
            var allPositionsSafe = true
            val offsetChecks = listOf(
                Vec3d(0.0, 0.0, 0.0),
                Vec3d(0.4, 0.0, 0.4),
                Vec3d(-0.4, 0.0, 0.4),
                Vec3d(0.4, 0.0, -0.4),
                Vec3d(-0.4, 0.0, -0.4)
            )
            val playerBoxCache = offsetChecks.map { offset ->
                val testPos = pos.add(offset)
                Box(
                    testPos.x - 0.3, testPos.y, testPos.z - 0.3,
                    testPos.x + 0.3, testPos.y + player.height, testPos.z + 0.3
                )
            }

            for (i in offsetChecks.indices) {
                val testPos = pos.add(offsetChecks[i])
                val playerBox = playerBoxCache[i]
                val hasSpace = !world.getBlockCollisions(player, playerBox).any()
                val entityCollisions = world.getEntitiesByClass(
                    Entity::class.java, playerBox) { e -> e != player && e.isCollidable }.isNotEmpty()
                if (!hasSpace || entityCollisions) {
                    allPositionsSafe = false
                    break
                }

                val belowTestPos = BlockPos(
                    testPos.x.toInt(),
                    (testPos.y - 0.5).toInt(),
                    testPos.z.toInt()
                ).down()
                val belowTestState = world.getBlockState(belowTestPos)
                if (belowTestState.isAir || belowTestState.block in unsafeBlocks
                    || (belowTestState.block == Blocks.CAMPFIRE && belowTestState.get(
                        Properties.LIT))) {
                    allPositionsSafe = false
                    break
                }

                if (ensureCompleteSpace) {
                    val abovePos = belowTestPos.up(2)
                    val aboveState = world.getBlockState(abovePos)
                    if (!aboveState.isAir || world.getBlockCollisions(
                            player,
                            playerBox.offset(0.0, 2.0, 0.0)).any()
                        ) {
                        allPositionsSafe = false
                        break
                    }
                }
            }

            var safeBlockCount = 0
            val directions = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
            for (dir in directions) {
                val nearby = blockPos.offset(dir)
                val state = world.getBlockState(nearby)
                if (!state.isAir && state.block !in unsafeBlocks && state.isFullCube(world, nearby)) {
                    safeBlockCount++
                }
            }

            val nearestSafeDistance = findNearestSafeBlock(pos)?.let { distanceSq2D(pos, it) } ?: 10000.0
            val xFrac = pos.x - pos.x.toInt()
            val zFrac = pos.z - pos.z.toInt()
            val edgePenalty = if (abs(xFrac - 0.5) < 0.3 || abs(zFrac - 0.5) < 0.3) 500.0 else 0.0

            when {
                !hasGround -> 5000.0 + nearestSafeDistance
                !allPositionsSafe -> 3000.0 + nearestSafeDistance
                safeBlockCount < adjacentSafeBlocks -> 1000.0 + nearestSafeDistance - safeBlockCount * 50.0
                else -> nearestSafeDistance + edgePenalty - safeBlockCount * 100.0
            }.also { System.currentTimeMillis().toDouble() }
        }
    }

    private fun findNearestSafeBlock(pos: Vec3d): Vec3d? {
        val maxThrowDistance = 10f

        return pos.searchBlocksInCuboid(maxThrowDistance) { checkPos, state ->
            !state.isAir && state.block !in unsafeBlocks && state.isFullCube(world, checkPos) &&
                (!ensureCompleteSpace || checkPos.up().getState()?.isAir == true)
        }.minByOrNull { (checkPos, _) ->
            checkPos.getCenterDistanceSquared()
        }?.let { (checkPos, _) ->
            checkPos.toVec3d().add(0.5, 1.0, 0.5)
        }
    }
    private fun rotateToSolution() {
        if (allowClutchWithStuck) {
            ModuleAutoStuck.shouldEnableStuck = true
        }

        if (bestSolution == null || bestEnergy >= 10000.0) {
            notification(
                "AutoClutch",
                "Trying to evade Void...",
                NotificationEvent.Severity.INFO
            )
            state = State.IDLE
            resetAllVariables()
            return
        }

        if (player.isSneaking || voidFallPrediction.isPlayerSafe()) {
            state = State.IDLE
            resetAllVariables()
            return
        }

        if (!mc.options.pickItemKey.isPressed) {
            bestSolution?.let { sol ->
                if (allowClutchWithStuck) {
                    sendPacketSilently(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            sol.yaw, sol.pitch, player.isOnGround, player.horizontalCollision
                        )
                    )
                } else {
                    RotationManager.setRotationTarget(
                        rotationConfig.toRotationTarget(sol),
                        priority = Priority.IMPORTANT_FOR_USAGE_1,
                        provider = this
                    )
                }

                if (RotationManager.serverRotation.angleTo(sol) <= aimPrecision) {
                    val pearlSlot = Slots.OffhandWithHotbar.findSlot(Items.ENDER_PEARL)?.hotbarSlotForServer
                    pearlSlot?.let { SilentHotbar.selectSlotSilently(this, it, 5) }
                    state = State.THROWING
                }
            } ?: run {
                state = State.IDLE
            }
        }
    }

    private fun throwPearl() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPearlThrowTime < postThrowCooldownTicks * 50L) {
            state = State.IDLE
            return
        }
        if (!voidFallPrediction.isVoidFallImminent) {
            state = State.IDLE
            return
        }
        if (isPearlInFlight) {
            return
        }
        Slots.Hotbar.findSlot(Items.ENDER_PEARL)?.let {
            useHotbarSlotOrOffhand(
                it,
                0,
                bestSolution?.yaw ?: 0f,
                bestSolution?.pitch ?: 0f
            )
            lastPearlThrowTime = currentTime
            isPearlInFlight = true
            pearlThrownTick = player.age.toLong()
            scheduleSafetyCheck()
        }
        ModuleAutoStuck.shouldEnableStuck = false
        state = State.PAUSED
    }

    private fun scheduleSafetyCheck() {
        safetyCheckCounter = 30
        safetyCheckActive = true
    }

    private fun resetAllVariables() {
        state = if (manualPearlThrown) State.PAUSED else State.IDLE
        bestSolution = null
        bestEnergy = Double.MAX_VALUE
        currentSolution = Rotation(0f, 0f)
        currentEnergy = Double.MAX_VALUE
        temperature = SimulatedAnnealing().initialTemperature
        iterations = 0
        triggerPosition = null
        pearlSlot = null
        safetyCheckCounter = 0
        safetyCheckActive = false
        predictedThrowPosition = null
        clearTrajectoryAndCache()
    }

    private fun distanceSq2D(a: Vec3d, b: Vec3d): Double {
        val dx = a.x - b.x
        val dz = a.z - b.z
        return dx * dx + dz * dz
    }

    override fun onEnabled() {
        super.onEnabled()
        resetAllVariables()
    }

    override fun onDisabled() {
        resetAllVariables()
        lastPlayerPosition = null
        manualPearlThrown = false
        blockPositionScoreCache.clear()
    }
}
