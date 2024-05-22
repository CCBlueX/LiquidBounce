/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.features.module.modules.combat.NoFriends
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.extensions.isAnimal
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.extensions.isMob
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.ccbluex.liquidbounce.utils.misc.StringUtils.contains
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3
import kotlin.math.cos
import kotlin.math.sin

object EntityUtils : MinecraftInstance() {

    var targetInvisible = false

    var targetPlayer = true

    var targetMobs = true

    var targetAnimals = false

    var targetDead = false

    private val healthSubstrings = arrayOf("hp", "health", "❤", "lives")

    fun isSelected(entity: Entity?, canAttackCheck: Boolean): Boolean {
        if (entity is EntityLivingBase && (targetDead || entity.isEntityAlive) && entity != mc.thePlayer) {
            if (targetInvisible || !entity.isInvisible) {
                if (targetPlayer && entity is EntityPlayer) {
                    if (canAttackCheck) {
                        if (isBot(entity))
                            return false

                        if (entity.isClientFriend() && !NoFriends.handleEvents())
                            return false

                        if (entity.isSpectator) return false

                        if (entity.customNameTag == "FAKE_PLAYER") return false

                        return !Teams.handleEvents() || !Teams.isInYourTeam(entity)
                    }
                    return true
                }

                return targetMobs && entity.isMob() || targetAnimals && entity.isAnimal()
            }
        }
        return false
    }

    fun isLookingOnEntities(entity: Entity, maxAngleDifference: Double): Boolean {
        val player = mc.thePlayer ?: return false
        val playerRotation = player.rotationYawHead
        val playerPitch = player.rotationPitch

        val maxAngleDifferenceRadians = Math.toRadians(maxAngleDifference)

        val lookVec = Vec3(
            -sin(playerRotation.toRadiansD()),
            -sin(playerPitch.toRadiansD()),
            cos(playerRotation.toRadiansD())
        ).normalize()

        val playerPos = player.positionVector.addVector(0.0, player.eyeHeight.toDouble(), 0.0)
        val entityPos = entity.positionVector.addVector(0.0, entity.eyeHeight.toDouble(), 0.0)

        val directionToEntity = entityPos.subtract(playerPos).normalize()
        val dotProductThreshold = lookVec.dotProduct(directionToEntity)

        return dotProductThreshold > cos(maxAngleDifferenceRadians)
    }

    fun getHealth(entity: EntityLivingBase, fromScoreboard: Boolean = false, absorption: Boolean = true): Float {
        if (fromScoreboard && entity is EntityPlayer) run {
            val scoreboard = entity.worldScoreboard
            val objective = scoreboard.getValueFromObjective(entity.name, scoreboard.getObjectiveInDisplaySlot(2))

            if (healthSubstrings !in objective.objective?.displayName)
                return@run

            val scoreboardHealth = objective.scorePoints

            if (scoreboardHealth > 0)
                return scoreboardHealth.toFloat()
        }

        var health = entity.health

        if (absorption)
            health += entity.absorptionAmount

        return if (health > 0) health else 20f
    }

}