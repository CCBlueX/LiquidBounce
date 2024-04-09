package net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider


    /*
    * Vulcan mode for the Spider module.
    * Made for Vulcan286
    * Tested on Eu.loyisa.cn and Anticheat-test.com
    * It may still flag sometimes, particularly when going more then 15-30 blocks up.
     */

internal object SpiderVulcan286 : Choice("Vulcan") {
    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpider.modes

    private var tickCounter = 0
    private var jumpDelayCounter = 0

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
            if (jumpDelayCounter >= 3) {
                player.jump()
                player.forwardSpeed = 0F
                player.sidewaysSpeed = 0F
                jumpDelayCounter = 0
            }
        }
    }
}
