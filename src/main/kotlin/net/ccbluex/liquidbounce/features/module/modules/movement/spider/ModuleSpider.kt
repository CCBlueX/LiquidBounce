package net.ccbluex.liquidbounce.features.module.modules.movement.spider

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes.SpiderVanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes.SpiderVulcan286

object ModuleSpider : Module("Spider", Category.MOVEMENT) {

    init {
        enableLock()
    }

    internal val modes = choices("Mode", SpiderVanilla, arrayOf(
        SpiderVanilla,
        SpiderVulcan286
    ))
}
