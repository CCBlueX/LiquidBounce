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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import java.util.function.Predicate
import kotlin.math.floor
import kotlin.math.sqrt

object ModuleBaritone : ClientModule("Baritone", Category.MOVEMENT) {

    // Configuration settings
    private val pathingConfig = tree(PathingConfig(this))
    private var directionGoal: GoalDirection? = null
    private var pathingPaused = false

    // Properties for querying Baritone state
    val isPathing: Boolean
        get() = BaritoneAPI.getProvider().primaryBaritone.pathingBehavior.isPathing

    val targetYaw: Float
        get() = BaritoneAPI.getProvider().primaryBaritone.playerContext.playerRotations().yaw

    val targetPitch: Float
        get() = BaritoneAPI.getProvider().primaryBaritone.playerContext.playerRotations().pitch

    init {
        // Register tick handler for updating direction goals
        SequenceManager.sequences += TickSequence(this) { directionGoal?.tick() }
    }

    override fun onEnabled() {
        super.onEnabled()
        pathingPaused = false
        applyBaritoneSettings()
    }

    override fun onDisabled() {
        super.onDisabled()
        stop()
        directionGoal = null
        pathingPaused = false
    }

    fun pause() {
        pathingPaused = true
    }

    fun resume() {
        pathingPaused = false
    }

    fun stop() {
        BaritoneAPI.getProvider().primaryBaritone.pathingBehavior.cancelEverything()
    }

    fun moveTo(pos: BlockPos, ignoreY: Boolean = pathingConfig.ignoreY) {
        if (!running) return
        val baritone = BaritoneAPI.getProvider().primaryBaritone
        val goal = if (ignoreY) GoalXZ(pos.x, pos.z) else GoalGetToBlock(pos)
        baritone.customGoalProcess.setGoalAndPath(goal)
    }

    fun moveInDirection(yaw: Float) {
        if (!running) return
        directionGoal = GoalDirection(yaw)
        BaritoneAPI.getProvider().primaryBaritone.customGoalProcess.setGoalAndPath(directionGoal)
    }

    fun mine(vararg blocks: Block) {
        if (!running) return
        BaritoneAPI.getProvider().primaryBaritone.mineProcess.mine(*blocks)
    }

    fun follow(entity: Predicate<Entity>) {
        if (!running) return
        BaritoneAPI.getProvider().primaryBaritone.followProcess.follow(entity)
    }

    private fun applyBaritoneSettings() {
        val baritoneSettings = BaritoneAPI.getSettings()
        baritoneSettings.allowSprint.value = pathingConfig.allowSprint
        baritoneSettings.allowBreak.value = pathingConfig.allowBreak
        baritoneSettings.allowPlace.value = pathingConfig.allowPlace
        baritoneSettings.allowWaterBucket.value = pathingConfig.allowWaterBucket
        baritoneSettings.maxFallHeightNoWater.value = pathingConfig.maxFallHeight
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

    class PathingConfig(parent: EventListener) : Configurable("Pathing") {
        val allowSprint by boolean("AllowSprint", true)
        val allowBreak by boolean("AllowBreak", true)
        val allowPlace by boolean("AllowPlace", true)
        val allowWaterBucket by boolean("AllowWaterBucket", false)
        val maxFallHeight by int("MaxFallHeight", 3, 2..10, "blocks")
        val ignoreY by boolean("IgnoreY", false)
        val avoidBlocks by blocks("AvoidBlocks", mutableSetOf())

        init {
            parent(parent)
        }
    }
}
