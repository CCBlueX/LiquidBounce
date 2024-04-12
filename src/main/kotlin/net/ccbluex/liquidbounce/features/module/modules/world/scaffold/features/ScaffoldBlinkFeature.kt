package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput

object ScaffoldBlinkFeature : ToggleableConfigurable(ModuleScaffold, "Blink", false) {

    private val edgeDistance by float("EdgeDistance", 0.6f, 0.01f..1f)
    private val fallCancel by boolean("FallCancel", true)

    var shouldBlink: Boolean = false
        private set
        get() = handleEvents() && field

    /**
     * The input handler tracks the movement of the player and calculates the predicted future position.
     */
    @Suppress("unused")
    val inputHandler = handler<MovementInputEvent>(
        priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING
    ) { event ->
        shouldBlink = if (event.directionalInput == DirectionalInput.NONE) {
            false
        } else {
            !player.isOnGround || player.isCloseToEdge(event.directionalInput, edgeDistance.toDouble())
        }

        if (fallCancel && player.fallDistance > 0.5f) {
            FakeLag.cancel()
        }
    }


}
