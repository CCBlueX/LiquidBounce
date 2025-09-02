package net.ccbluex.liquidbounce.utils.pathing

import baritone.api.BaritoneAPI
import baritone.api.Settings
import baritone.api.utils.SettingsUtil
import baritone.api.pathing.goals.Goal
import baritone.api.pathing.goals.GoalGetToBlock
import baritone.api.pathing.goals.GoalXZ
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.SequenceManager
import net.ccbluex.liquidbounce.event.TickSequence
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.floor
import kotlin.math.sqrt

class BaritonePathManager(val parent: EventListener) : Configurable("Pathing"), EventListener {

    override fun parent() = parent

    val allowSprint by boolean("AllowSprint", true)
    val allowBreak by boolean("AllowBreak", false)
    val allowPlace by boolean("AllowPlace", false)
    val allowInventory by boolean("AllowInventory", false)
    val autoTool by boolean("AutoTool", true)
    val allowParkour by boolean("AllowParkour", true)
    val allowParkourPlace by boolean("AllowParkourPlace", true)
    val allowWaterBucketFall by boolean("AllowWaterBucketFall", false)
    val allowDiagonalAscend by boolean("AllowDiagonalAscend", false)
    val allowDiagonalDescend by boolean("AllowDiagonalDescend", false)
    val sprintInWater by boolean("SprintInWater", true)
    val antiCheatCompatibility by boolean("AntiCheatCompatibility", true)
    val avoidUpdatingFallingBlocks by boolean("AvoidUpdatingFallingBlocks", true)
    val renderPath by boolean("RenderPath", true)
    val renderGoal by boolean("RenderGoal", true)

    val maxFallHeight by int("MaxFallHeight", 3, 0..10, "blocks")
    val primaryTimeoutMS by int("PrimaryTimeoutMS", 500, 0..5000, "ms")
    val failureTimeoutMS by int("FailureTimeoutMS", 2000, 0..5000, "ms")
    val planningTickLookahead by int("PlanningTickLookahead", 150, 50..500, "ticks")

    val pathCutoffFactor by float("PathCutoffFactor", 0.9f, 0.0f..1.0f)
    val costHeuristic by float("CostHeuristic", 3.563f, 0.0f..10f)
    val pathRenderLineWidth by float("PathRenderLineWidth", 5f, 1f..10f, "pixels")
    val goalRenderLineWidth by float("GoalRenderLineWidth", 3f, 1f..10f, "pixels")

    private var directionGoal: GoalDirection? = null
    private var pathingPaused = false

    init {
        SequenceManager.sequences += TickSequence(this) {
            val settings = BaritoneAPI.getSettings() ?: return@TickSequence
            syncSettingsToBaritone(settings)

            directionGoal?.tick()
        }
    }

    val isPathing: Boolean
        get() = BaritoneAPI.getProvider().primaryBaritone?.pathingBehavior?.isPathing ?: false

    val targetYaw: Float
        get() = BaritoneAPI.getProvider().primaryBaritone?.playerContext?.playerRotations()?.yaw ?: 0f

    val targetPitch: Float
        get() = BaritoneAPI.getProvider().primaryBaritone?.playerContext?.playerRotations()?.pitch ?: 0f

    fun pause() {
        pathingPaused = true
    }

    fun resume() {
        pathingPaused = false
    }
    var currentTarget: BlockPos? = null
        private set

    fun moveTo(pos: BlockPos, ignoreY: Boolean = false, threshold: Double = 0.5) {
        val baritone = BaritoneAPI.getProvider().primaryBaritone ?: return
        val goal: Goal = if (ignoreY) GoalXZ(pos.x, pos.z) else GoalGetToBlock(pos)
        currentTarget = pos
        baritone.customGoalProcess.setGoalAndPath(goal)

        val player = mc.player ?: return
        val targetVec = Vec3d.ofCenter(pos)
        if (player.pos.distanceTo(targetVec) < threshold) {
            stop()
        }
    }

    fun stop() {
        val baritone = BaritoneAPI.getProvider().primaryBaritone ?: return
        baritone.customGoalProcess.goal = null
        currentTarget = null
    }



    fun moveInDirection(yaw: Float) {
        val baritone = BaritoneAPI.getProvider().primaryBaritone ?: return
        directionGoal = GoalDirection(yaw)
        baritone.customGoalProcess.setGoalAndPath(directionGoal)
    }

    private fun syncSettingsToBaritone(settings: Settings) {
        var changed = false

        fun <T> update(setting: Settings.Setting<T>, value: T) {
            if (setting.value != value) {
                setting.value = value
                changed = true
            }
        }

        update(settings.allowSprint, allowSprint)
        update(settings.allowBreak, allowBreak)
        update(settings.allowPlace, allowPlace)
        update(settings.allowInventory, allowInventory)
        update(settings.autoTool, autoTool)
        update(settings.allowParkourAscend, allowParkour)
        update(settings.allowParkourPlace, allowParkourPlace)
        update(settings.allowDiagonalAscend, allowDiagonalAscend)
        update(settings.allowDiagonalDescend, allowDiagonalDescend)
        update(settings.sprintInWater, sprintInWater)
        update(settings.antiCheatCompatibility, antiCheatCompatibility)
        update(settings.avoidUpdatingFallingBlocks, avoidUpdatingFallingBlocks)
        update(settings.maxFallHeightNoWater, maxFallHeight)
        update(settings.allowWaterBucketFall, allowWaterBucketFall)
        update(settings.primaryTimeoutMS, primaryTimeoutMS.toLong())
        update(settings.failureTimeoutMS, failureTimeoutMS.toLong())
        update(settings.planningTickLookahead, planningTickLookahead)
        update(settings.costHeuristic, costHeuristic.toDouble())
        update(settings.pathCutoffFactor, pathCutoffFactor.toDouble())
        update(settings.renderPath, renderPath)
        update(settings.renderGoal, renderGoal)
        update(settings.pathRenderLineWidthPixels, pathRenderLineWidth)
        update(settings.goalRenderLineWidthPixels, goalRenderLineWidth)

        if (changed) {
            try {
                SettingsUtil.save(settings)
            } catch (_: Throwable) {}
        }
    }
    private class GoalDirection(private val yaw: Float) : Goal {
        private var x = 0
        private var z = 0
        private var timer = 0

        init { tick() }

        fun tick() {
            if (timer-- > 0) return
            timer = 20
            val pos = mc.player?.pos ?: return
            val theta = Math.toRadians(yaw.toDouble())
            x = floor(pos.x - MathHelper.sin(theta.toFloat()) * 100).toInt()
            z = floor(pos.z + MathHelper.cos(theta.toFloat()) * 100).toInt()
        }

        override fun isInGoal(x: Int, y: Int, z: Int) = this.x == x && this.z == z

        override fun heuristic(x: Int, y: Int, z: Int): Double {
            val xDiff = x - this.x
            val zDiff = z - this.z
            val straight = kotlin.math.abs(xDiff - zDiff)
            val diagonal = kotlin.math.min(kotlin.math.abs(xDiff), kotlin.math.abs(zDiff)) * sqrt(2.0)
            return straight + diagonal
        }

        override fun toString() = "GoalDirection(x=$x, z=$z)"
    }
}
