/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.extensions.isAnimal
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.extensions.isMob
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

object HitBox : Module("HitBox", ModuleCategory.COMBAT) {

    val targetPlayers by BoolValue("TargetPlayers", true)
    val playerSize by FloatValue("PlayerSize", 0.4F, 0F..1F) { targetPlayers }
    val friendSize by FloatValue("FriendSize", 0.4F, 0F..1F) { targetPlayers }
    val teamMateSize by FloatValue("TeamMateSize", 0.4F, 0F..1F) { targetPlayers }
    val botSize by FloatValue("BotSize", 0.4F, 0F..1F) { targetPlayers }
    val targetMobs by BoolValue("TargetMobs", false)
    val mobSize by FloatValue("MobSize", 0.4F, 0F..1F) { targetMobs }
    val targetAnimals by BoolValue("TargetAnimals", false)
    val animalSize by FloatValue("AnimalSize", 0.4F, 0F..1F) { targetAnimals }

    fun determineSize(entity: Entity): Float {
        return when (entity) {
            is EntityPlayer -> {
                if (entity.isSpectator || !targetPlayers) {
                    return 0F
                }

                if (isBot(entity)) {
                    return botSize
                } else if (entity.isClientFriend() && !NoFriends.state) {
                    return friendSize
                } else if (Teams.state && Teams.isInYourTeam(entity)) {
                    return teamMateSize
                }

                playerSize
            }

            else -> {
                if (entity.isMob() && targetMobs) {
                    return mobSize
                } else if (entity.isAnimal() && targetAnimals) {
                    return animalSize
                }

                0F
            }
        }
    }
}