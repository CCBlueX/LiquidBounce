/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.extensions.getFullName
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket
import net.minecraft.network.packet.s2c.play.EntityS2CPacket
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket

object AntiBot : Module("AntiBot", Category.MISC, hideModule = false) {

    private val tab by BoolValue("Tab", true)
        private val tabMode by ListValue("TabMode", arrayOf("Equals", "Contains"), "Contains") { tab }

    private val entityID by BoolValue("EntityID", true)
    private val invalidUUID by BoolValue("InvalidUUID", true)
    private val color by BoolValue("Color", false)

    private val livingTime by BoolValue("LivingTime", false)
        private val livingTimeTicks by IntegerValue("LivingTimeTicks", 40, 1..200) { livingTime }

    private val capabilities by BoolValue("Capabilities", true)
    private val ground by BoolValue("Ground", true)
    private val air by BoolValue("Air", false)
    private val invalidGround by BoolValue("InvalidGround", true)
    private val swing by BoolValue("Swing", false)
    private val health by BoolValue("Health", false)
    private val derp by BoolValue("Derp", true)
    private val wasInvisible by BoolValue("WasInvisible", false)
    private val armor by BoolValue("Armor", false)
    private val ping by BoolValue("Ping", false)
    private val needHit by BoolValue("NeedHit", false)
    private val duplicateInWorld by BoolValue("DuplicateInWorld", false)
    private val duplicateInTab by BoolValue("DuplicateInTab", false)
    private val properties by BoolValue("Properties", false)

    private val alwaysInRadius by BoolValue("AlwaysInRadius", false)
        private val alwaysRadius by FloatValue("AlwaysInRadiusBlocks", 20f, 5f..30f) { alwaysInRadius }

    private val groundList = mutableListOf<Int>()
    private val airList = mutableListOf<Int>()
    private val invalidGroundList = mutableMapOf<Int, Int>()
    private val swingList = mutableListOf<Int>()
    private val invisibleList = mutableListOf<Int>()
    private val propertiesList = mutableListOf<Int>()
    private val hitList = mutableListOf<Int>()
    private val notAlwaysInRadiusList = mutableListOf<Int>()
    private val worldPlayerNames = mutableSetOf<String>()
    private val worldDuplicateNames = mutableSetOf<String>()
    private val tabPlayerNames = mutableSetOf<String>()
    private val tabDuplicateNames = mutableSetOf<String>()

    fun isBot(entity: LivingEntity): Boolean {
        // Check if entity is a player
        if (entity !is PlayerEntity)
            return false

        // Check if anti bot is enabled
        if (!handleEvents())
            return false

        // Anti Bot checks

        if (color && "§" !in entity.displayName.formattedText.replace("§r", ""))
            return true

        if (livingTime && entity.ticksAlive < livingTimeTicks)
            return true

        if (ground && entity.entityId !in groundList)
            return true

        if (air && entity.entityId !in airList)
            return true

        if (swing && entity.entityId !in swingList)
            return true

        if (health && (entity.health > 20F || entity.health < 0F))
            return true

        if (entityID && (entity.entityId >= 1000000000 || entity.entityId <= -1))
            return true

        if (derp && (entity.rotationPitch > 90F || entity.rotationPitch < -90F))
            return true

        if (wasInvisible && entity.entityId in invisibleList)
            return true

        if (properties && entity.entityId !in propertiesList)
            return true

        if (armor) {
            if (entity.inventory.armorInventory[0] == null && entity.inventory.armorInventory[1] == null &&
                    entity.inventory.armorInventory[2] == null && entity.inventory.armorInventory[3] == null)
                return true
        }

        if (ping) {
            if (mc.networkHandler.getPlayerInfo(entity.uniqueID)?.responseTime == 0 ||
                mc.networkHandler.getPlayerInfo(entity.uniqueID)?.responseTime == null)
                return true
        }

        if (invalidUUID && mc.networkHandler.getPlayerInfo(entity.uniqueID) == null) {
            return true
        }

        if (capabilities && (entity.isSpectator || entity.capabilities.flying || entity.capabilities.allowFlying
                    || entity.capabilities.invulnerable || entity.capabilities.isCreativeMode))
            return true

        if (needHit && entity.entityId !in hitList)
            return true

        if (invalidGround && invalidGroundList.getOrDefault(entity.entityId, 0) >= 10)
            return true

        if (tab) {
            val equals = tabMode == "Equals"
            val targetName = stripColor(entity.displayName.formattedText)

            val shouldReturn = mc.networkHandler.playerList.any { networkPlayerInfo ->
                val networkName = stripColor(networkPlayerInfo.getFullName())
                if (equals) {
                    targetName == networkName
                } else {
                    networkName in targetName
                }
            }
            return !shouldReturn
        }

        if (duplicateInWorld) {
            for (player in mc.world.playerEntities.filterNotNull()) {
                val playerName = player.name

                if (worldPlayerNames.contains(playerName)) {
                    worldDuplicateNames.add(playerName)
                } else {
                    worldPlayerNames.add(playerName)
                }
            }

            if (worldDuplicateNames.isNotEmpty()) {
                val duplicateCount = worldDuplicateNames.size
                if (mc.world.playerEntities.count { it.name in worldDuplicateNames } > duplicateCount) {
                    return true
                }
            }
        }

        if (duplicateInTab) {
            for (networkPlayerInfo in mc.networkHandler.playerList.filterNotNull()) {
                val playerName = stripColor(networkPlayerInfo.getFullName())

                if (tabPlayerNames.contains(playerName)) {
                    tabDuplicateNames.add(playerName)
                } else {
                    tabPlayerNames.add(playerName)
                }
            }

            if (tabDuplicateNames.isNotEmpty()) {
                val duplicateCount = tabDuplicateNames.size
                if (mc.networkHandler.playerList.count { stripColor(it.getFullName()) in tabDuplicateNames } > duplicateCount) {
                    return true
                }
            }
        }

        if (alwaysInRadius && entity.entityId !in notAlwaysInRadiusList)
            return true

        return entity.name.isEmpty() || entity.name == mc.player.name
    }

    override fun onDisable() {
        super.onDisable()
    }

    @EventTarget(ignoreCondition=true)
    fun onPacket(event: PacketEvent) {
        if (mc.player == null || mc.world == null)
            return

        val packet = event.packet

        if (packet is EntityS2CPacket) {
            val entity = packet.getEntity(mc.world)

            if (entity is PlayerEntity) {
                if (entity.onGround && entity.entityId !in groundList)
                    groundList += entity.entityId

                if (!entity.onGround && entity.entityId !in airList)
                    airList += entity.entityId

                if (entity.onGround) {
                    if (entity.fallDistance > 0.0 || entity.posY == entity.prevY) {
                        invalidGroundList[entity.entityId] = invalidGroundList.getOrDefault(entity.entityId, 0) + 1
                    } else if (!entity.isCollidedVertically) {
                        invalidGroundList[entity.entityId] = invalidGroundList.getOrDefault(entity.entityId, 0) + 1
                    }
                } else {
                    val currentVL = invalidGroundList.getOrDefault(entity.entityId, 0)
                    if (currentVL > 0) {
                        invalidGroundList[entity.entityId] = currentVL - 1
                    } else {
                        invalidGroundList.remove(entity.entityId)
                    }
                }

                if ((entity.isInvisible || entity.isInvisibleToPlayer(mc.player)) && entity.entityId !in invisibleList)
                    invisibleList += entity.entityId

                if (alwaysInRadius) {
                    val distance = mc.player.getDistanceToEntity(entity)

                    if (distance < alwaysRadius) {
                        if (entity.entityId in notAlwaysInRadiusList) {
                            notAlwaysInRadiusList.remove(entity.entityId)
                        }
                    } else {
                        if (entity.entityId !in notAlwaysInRadiusList) {
                            notAlwaysInRadiusList.add(entity.entityId)
                        }
                    }
                }
            }
        }

        if (packet is EntityAnimationS2CPacket) {
            val entity = mc.world.getEntityById(packet.entityID)

            if (entity != null && entity is LivingEntity && packet.animationType == 0
                    && entity.entityId !in swingList)
                swingList += entity.entityId
        }

        if (packet is EntityAttributesS2CPacket) {
            propertiesList += packet.entityId
        }

        if (packet is EntitiesDestroyS2CPacket) {
            for (entityID in packet.entityIDs) {
                // Check if entityID exists in groundList and remove if found
                if (entityID in groundList) groundList -= entityID

                // Check if entityID exists in airList and remove if found
                if (entityID in airList) airList -= entityID

                // Check if entityID exists in invalidGroundList and remove if found
                if (entityID in invalidGroundList) invalidGroundList -= entityID

                // Check if entityID exists in swingList and remove if found
                if (entityID in swingList) swingList -= entityID

                // Check if entityID exists in invisibleList and remove if found
                if (entityID in invisibleList) invisibleList -= entityID

                // Check if entityID exists in notAlwaysInRadiusList and remove if found
                if (entityID in notAlwaysInRadiusList) notAlwaysInRadiusList -= entityID
            }
        }
    }

    @EventTarget(ignoreCondition=true)
    fun onAttack(e: AttackEvent) {
        val entity = e.targetEntity

        if (entity != null && entity is LivingEntity && entity.entityId !in hitList)
            hitList += entity.entityId
    }

    @EventTarget(ignoreCondition=true)
    fun onWorld(event: WorldEvent) {
        clearAll()
    }

    private fun clearAll() {
        hitList.clear()
        swingList.clear()
        groundList.clear()
        invalidGroundList.clear()
        invisibleList.clear()
        notAlwaysInRadiusList.clear()
        worldPlayerNames.clear()
        worldDuplicateNames.clear()
        tabPlayerNames.clear()
        tabDuplicateNames.clear()
    }

}