package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.EventScheduler
import net.ccbluex.liquidbounce.utils.client.QuickAccess.player
import net.ccbluex.liquidbounce.utils.client.QuickAccess.world

object ScaffoldAutoJumpFeature : ToggleableConfigurable(ModuleScaffold, "AutoJump", false) {
    private val whenGoingDiagonal by boolean("WhenGoingDiagonal", false)
    private val predictFactor by float("PredictFactor", 0.54f, 0f..2f)
    private val useDelay by boolean("UseDelay", true)

    private val maxBlocks by int("MaxBlocks", 8, 3..17)

    private var blocksPlaced = 0

    fun onBlockPlacement() {
        blocksPlaced++
    }

    fun jumpIfNeeded(ticksUntilNextBlock: Int) {
        if (shouldJump(ticksUntilNextBlock)) {
            EventScheduler.schedule<MovementInputEvent>(ModuleScaffold) {
                it.jumping = true
            }
            blocksPlaced = 0
        }
    }

    var isGoingDiagonal = false

    fun shouldJump(ticksUntilNextBlock: Int): Boolean {
        if (!enabled)
            return false
        if (player.isOnGround)
            return false
        if (player.isSneaking)
            return false
        if (!whenGoingDiagonal && isGoingDiagonal)
            return false

        val extraPrediction =
            if (blocksPlaced >= maxBlocks) 1
            else if (useDelay) ticksUntilNextBlock
            else 0

        val predictedBoundingBox = player.boundingBox.offset(0.0, -1.5, 0.0)
            .offset(player.velocity.multiply(predictFactor.toDouble() + extraPrediction))

        return world.getBlockCollisions(player, predictedBoundingBox).none()
    }
}
