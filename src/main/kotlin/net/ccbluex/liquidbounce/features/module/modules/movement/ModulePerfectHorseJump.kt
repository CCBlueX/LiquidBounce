package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object ModulePerfectHorseJump : Module("PerfectHorseJump", Category.MOVEMENT) {
    val repeatable = repeatable {
        // See https://gist.github.com/phase/9b2a3a7db6bc6aec8dae4f3ea197921d
        player.field_3938 = 9 // jumpRidingTicks aka horseJumpPowerCounter
        player.field_3922 = 1f // jumpRidingScale aka horseJumpPower
    }
}

