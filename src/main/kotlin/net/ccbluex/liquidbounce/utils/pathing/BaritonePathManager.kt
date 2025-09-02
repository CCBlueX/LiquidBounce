package net.ccbluex.liquidbounce.utils.pathing

import baritone.api.BaritoneAPI
import baritone.api.pathing.goals.Goal
import baritone.api.pathing.goals.GoalGetToBlock
import baritone.api.pathing.goals.GoalXZ
import baritone.api.process.IBaritoneProcess
import baritone.api.process.PathingCommand
import baritone.api.process.PathingCommandType
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.SequenceManager
import net.ccbluex.liquidbounce.event.TickSequence
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import java.util.function.Predicate
import kotlin.math.floor
import kotlin.math.sqrt

class BaritonePathManager(val parent: EventListener) : Configurable("Pathing"), EventListener {

    override fun parent() = parent

    // Existing movement settings
    val allowSprint by boolean("AllowSprint", true)
    val allowBreak by boolean("AllowBreak", false)
    val allowPlace by boolean("AllowPlace", false)
    val ignoreY by boolean("IgnoreY", false)
    val maxFallHeight by int("MaxFallHeight", 3, 2..10, "blocks")
    val allowWaterBucketFall by boolean("AllowWaterBucketFall", true)
    val allowParkour by boolean("AllowParkour", false)
    val allowParkourPlace by boolean("AllowParkourPlace", false)

    // New movement settings
    val allowInventory by boolean("AllowInventory", false)
    val autoTool by boolean("AutoTool", true)
    val allowDiagonalAscend by boolean("AllowDiagonalAscend", false)
    val allowDiagonalDescend by boolean("AllowDiagonalDescend", false)
    val sprintInWater by boolean("SprintInWater", true)
    val antiCheatCompatibility by boolean("AntiCheatCompatibility", true)

    // Existing block settings
    val avoidBlocks by blocks("AvoidBlocks", mutableSetOf())
    val avoidUpdatingFallingBlocks by boolean("AvoidUpdatingFallingBlocks", true)
    val blocksToAvoidBreaking by blocks("BlocksToAvoidBreaking", mutableSetOf())

    // New block settings
    val acceptableThrowawayItems by items("AcceptableThrowawayItems", mutableSetOf(
      Items.DIRT, Items.COBBLESTONE, Items.NETHERRACK, Items.STONE
    ))

    val pathCutoffFactor by float("PathCutoffFactor", 0.9f, 0.5f..1.0f)
    val primaryTimeoutMS by int("PrimaryTimeoutMS", 500, 200..2000, "ms")
    val failureTimeoutMS by int("FailureTimeoutMS", 2000, 1000..5000, "ms")

    // New performance settings
    val costHeuristic by float("CostHeuristic", 3.563f, 3.5f..4.6f)
    val planningTickLookahead by int("PlanningTickLookahead", 150, 50..500, "ticks")

    // New rendering settings
    val renderPath by boolean("RenderPath", true)
    val renderGoal by boolean("RenderGoal", true)
    val pathRenderLineWidth by float("PathRenderLineWidth", 5f, 1f..10f, "pixels")
    val goalRenderLineWidth by float("GoalRenderLineWidth", 3f, 1f..10f, "pixels")
    val colorCurrentPath by color("ColorCurrentPath", Color4b.RED)
    val colorGoalBox by color("ColorGoalBox", Color4b.GREEN)

    init {
        applyBaritoneSettings()
        SequenceManager.sequences += TickSequence(this) { directionGoal?.tick() }
    }

    private var directionGoal: GoalDirection? = null
    private var pathingPaused = false

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

    fun stop() {
        BaritoneAPI.getProvider().primaryBaritone?.pathingBehavior?.cancelEverything()
    }

    fun moveTo(pos: BlockPos, ignoreY: Boolean = this.ignoreY) {
        val baritone = BaritoneAPI.getProvider().primaryBaritone ?: return
        val goal = if (ignoreY) GoalXZ(pos.x, pos.z) else GoalGetToBlock(pos)
        baritone.customGoalProcess.setGoalAndPath(goal)
    }

    fun moveInDirection(yaw: Float) {
        val baritone = BaritoneAPI.getProvider().primaryBaritone ?: return
        directionGoal = GoalDirection(yaw)
        baritone.customGoalProcess.setGoalAndPath(directionGoal)
    }

    fun mine(vararg blocks: Block) {
        BaritoneAPI.getProvider().primaryBaritone?.mineProcess?.mine(*blocks)
    }

    fun follow(entity: Predicate<Entity>) {
        BaritoneAPI.getProvider().primaryBaritone?.followProcess?.follow(entity)
    }

    private fun applyBaritoneSettings() {
        val baritoneSettings = BaritoneAPI.getSettings() ?: return

        // Core movement settings
        baritoneSettings.allowSprint.value = allowSprint
        baritoneSettings.allowBreak.value = allowBreak
        baritoneSettings.allowPlace.value = allowPlace
        baritoneSettings.maxFallHeightNoWater.value = maxFallHeight
        baritoneSettings.allowWaterBucketFall.value = allowWaterBucketFall
        baritoneSettings.allowParkour.value = allowParkour
        baritoneSettings.allowParkourPlace.value = allowParkourPlace
        baritoneSettings.allowInventory.value = allowInventory
        baritoneSettings.autoTool.value = autoTool
        baritoneSettings.allowDiagonalAscend.value = allowDiagonalAscend
        baritoneSettings.allowDiagonalDescend.value = allowDiagonalDescend
        baritoneSettings.sprintInWater.value = sprintInWater
        baritoneSettings.antiCheatCompatibility.value = antiCheatCompatibility

        // Block interaction settings
        baritoneSettings.blocksToAvoid.value = avoidBlocks.toList()
        baritoneSettings.avoidUpdatingFallingBlocks.value = avoidUpdatingFallingBlocks
        baritoneSettings.blocksToAvoidBreaking.value = blocksToAvoidBreaking.toList()
        baritoneSettings.acceptableThrowawayItems.value = acceptableThrowawayItems.toList()

        // Performance settings
        baritoneSettings.pathCutoffFactor.value = pathCutoffFactor.toDouble()
        baritoneSettings.primaryTimeoutMS.value = primaryTimeoutMS.toLong()
        baritoneSettings.failureTimeoutMS.value = failureTimeoutMS.toLong()
        baritoneSettings.costHeuristic.value = costHeuristic.toDouble()
        baritoneSettings.planningTickLookahead.value = planningTickLookahead

        // Rendering settings
        baritoneSettings.renderPath.value = renderPath
        baritoneSettings.renderGoal.value = renderGoal
        baritoneSettings.pathRenderLineWidthPixels.value = pathRenderLineWidth
        baritoneSettings.goalRenderLineWidthPixels.value = goalRenderLineWidth
        baritoneSettings.colorCurrentPath.value = colorCurrentPath.toAwtColor()
        baritoneSettings.colorGoalBox.value = colorGoalBox.toAwtColor()
    }

    private class GoalDirection(private val yaw: Float) : Goal {
        private var x: Int = 0
        private var z: Int = 0
        private var timer: Int = 0

        init {
            tick()
        }

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

    private inner class BaritoneProcess : IBaritoneProcess {
        override fun isActive(): Boolean = pathingPaused
        override fun onTick(b: Boolean, b1: Boolean) = PathingCommand(null, PathingCommandType.REQUEST_PAUSE)
        override fun isTemporary(): Boolean = true
        override fun onLostControl() {}
        override fun priority(): Double = 0.0
        override fun displayName0(): String = "LiquidBounce"
    }
}
