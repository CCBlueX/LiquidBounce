package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

object ModuleNoMissCooldown : ClientModule("NoMissCooldown", Category.COMBAT) {

    /**
     * Disables the miss-cooldown of 10 ticks when missing an attack.
     */
    val removeAttackCooldown by boolean("RemoveAttackCooldown", true)

    /**
     * Cancels the attack when missing an attack.
     */
    val cancelAttackOnMiss by boolean("CancelAttackOnMiss", false)

}
