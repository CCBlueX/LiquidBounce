package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Choice
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.sequenceHandler
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects

object ModuleFullBright : Module("FullBright", Category.RENDER) {
    private val modes = choices("Mode", "Gamma") {
        FullBrightGamma
        FullBrightNightVision
    }

    private object FullBrightGamma : Choice("Gamma", modes) {
        private var prevValue: Double = 0.0

        override fun enable() {
            prevValue = mc.options.gamma
        }

        val tickHandler = sequenceHandler<PlayerTickEvent> {
            if(mc.options.gamma <= 100) mc.options.gamma++
        }

        override fun disable() {
            mc.options.gamma = prevValue
        }
    }

    private object FullBrightNightVision : Choice("Night Vision", modes) {
        val tickHandler = sequenceHandler<PlayerTickEvent> {
            player.addStatusEffect(StatusEffectInstance(StatusEffects.NIGHT_VISION, 1337))
        }

        override fun disable() {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION)
        }
    }
}
