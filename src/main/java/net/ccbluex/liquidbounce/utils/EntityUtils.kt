/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.combat.NoFriends
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.extensions.isAnimal
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.extensions.isMob
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer

class Targets {
    @JvmField
    var default = false

    @JvmField
    var invisible = false

    @JvmField
    var players = true

    @JvmField
    var mobs = true

    @JvmField
    var animals = false

    @JvmField
    var dead = false
}

object EntityUtils : MinecraftInstance() {
    @JvmField
    var defaultTargets = Targets()

    fun getTargets(targets: Targets): Targets {
        if (targets.default) {
            return defaultTargets
        } else {
            return targets
        }
    }

    @JvmStatic
    fun isSelected(entity: Entity?, canAttackCheck: Boolean): Boolean {
        return isSelected(entity, defaultTargets, canAttackCheck)
    }

    @JvmStatic
    fun isSelected(entity: Entity?, targets: Targets, canAttackCheck: Boolean): Boolean {
        var usedTargets = getTargets(targets)
        if (entity is EntityLivingBase && (usedTargets.dead || entity.isEntityAlive) && entity != mc.thePlayer) {
            if (usedTargets.invisible || !entity.isInvisible) {
                if (usedTargets.players && entity is EntityPlayer) {
                    if (canAttackCheck) {
                        if (isBot(entity))
                            return false

                        if (entity.isClientFriend() && !LiquidBounce.moduleManager.getModule(NoFriends::class.java).state)
                            return false

                        if (entity.isSpectator) return false
                        val teams = LiquidBounce.moduleManager.getModule(Teams::class.java) as Teams
                        return !teams.state || !teams.isInYourTeam(entity)
                    }
                    return true
                }

                return usedTargets.mobs && entity.isMob() || usedTargets.animals && entity.isAnimal()
            }
        }
        return false
    }
}
