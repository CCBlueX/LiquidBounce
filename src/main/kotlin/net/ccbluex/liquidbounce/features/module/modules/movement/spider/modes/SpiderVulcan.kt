package net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider

internal object SpiderVulcan286 : Choice("Vulcan") {
    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpider.modes

    private var tickCounter = 0
    private var jumpDelayCounter = 0
    private val jumpDelay = 3

    val repeatable = repeatable {
        if (player.isHoldingOntoLadder || player.isTouchingWater || player.isInLava) {
            tickCounter = 0
            return@repeatable
        }

        if (tickCounter >= 3)
            tickCounter = 0

        tickCounter++

        if (player.horizontalCollision && tickCounter == 2 % 3) {
            jumpDelayCounter++
            if (jumpDelayCounter >= jumpDelay) {
                player.jump()
                player.forwardSpeed = 0F
                player.sidewaysSpeed = 0F
                jumpDelayCounter = 0
            }
        }
    }
}
