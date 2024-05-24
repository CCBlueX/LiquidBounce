package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.consume

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowSharedGrim2860
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowSharedGrim2860MC18
import net.ccbluex.liquidbounce.utils.client.inGame
import net.minecraft.util.UseAction

object Consume : ToggleableConfigurable(ModuleNoSlow, "Consume", true) {

    val forwardMultiplier by float("Forward", 1f, 0.2f..1f)
    val sidewaysMultiplier by float("Sideways", 1f, 0.2f..1f)
    val noInteract by boolean("NoInteract", false)

    val modes = choices<Choice>(this, "Mode", { it.choices[0] }) {
        arrayOf(NoneChoice(it), NoSlowSharedGrim2860(it), NoSlowSharedGrim2860MC18(it))
    }

    override fun handleEvents(): Boolean {
        if (!super.handleEvents() || !inGame) {
            return false
        }

        // Check if we are using a consume item
        return player.isUsingItem && player.activeItem.useAction == UseAction.EAT
    }

}
