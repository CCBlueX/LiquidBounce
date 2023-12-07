package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput

object ScaffoldSpeedLimiterFeature : ToggleableConfigurable(ModuleScaffold, "SpeedLimiter", false) {

    private val speedLimit by float("SpeedLimit", 0.11f, 0.01f..0.12f)

    val moveEvent = handler<MovementInputEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
        if (player.sqrtSpeed > speedLimit) {
            it.directionalInput = DirectionalInput.NONE
        }
    }

}
