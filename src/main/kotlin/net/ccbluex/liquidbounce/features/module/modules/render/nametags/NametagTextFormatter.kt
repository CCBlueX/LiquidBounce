package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.ping
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import kotlin.math.roundToInt

class NametagTextFormatter(private val entity: Entity) {
    fun format(): String {
        val outputBuilder = StringBuilder()

        if (ModuleNametags.distance) {
            outputBuilder.append(this.distanceText).append(" ")
        }
        if (ModuleNametags.ping) {
            outputBuilder.append(this.pingText).append(" ")
        }

        outputBuilder.append("${this.nameColor}${entity.displayName.string}")

        if (ModuleNametags.health) {
            outputBuilder.append(" ").append(this.healthText)
        }

        if (this.isBot) {
            outputBuilder.append(" §c§lBot")
        }

        return outputBuilder.toString()
    }

    private val isBot = ModuleAntiBot.isBot(entity)

    private val nameColor: String
        get() =
            when {
                isBot -> "§3"
                entity.isInvisible -> "§6"
                entity.isSneaking -> "§4"
                else -> "§7"
            }

    private val distanceText: String
        get() {
            val playerDistanceRounded = mc.player!!.distanceTo(entity).roundToInt()

            return "§7${playerDistanceRounded}m"
        }

    private fun getPing(entity: Entity): Int? {
        return (entity as? PlayerEntity)?.ping
    }

    private val pingText: String
        get() {
            val playerPing = getPing(entity) ?: return ""

            val coloringBasedOnPing =
                when {
                    playerPing > 200 -> "§c"
                    playerPing > 100 -> "§e"
                    else -> "§a"
                }

            return " §7[" + coloringBasedOnPing + playerPing + "ms§7] "
        }

    private val healthText: String
        get() {
            if (entity !is LivingEntity) {
                return ""
            }

            return "§c${entity.health.toInt()} HP"
        }
}
