package net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider

internal object SpiderVanilla : Choice("Vanilla") {

    private val motion by float("Motion", 0.3F, 0.05f..0.8F)

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpider.modes

    val repeatable = repeatable {
        if (player.horizontalCollision) {
            player.velocity.y = motion.toDouble()
        }
    }
}
