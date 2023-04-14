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
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.extensions.getFullName
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0BPacketAnimation
import net.minecraft.network.play.server.S14PacketEntity

object AntiBot : Module("AntiBot", ModuleCategory.MISC) {

    private val tabValue = BoolValue("Tab", true)
    private val tabModeValue = object : ListValue("TabMode", arrayOf("Equals", "Contains"), "Contains") {
        override fun isSupported() = tabValue.get()
    }
    private val entityIDValue = BoolValue("EntityID", true)
    private val colorValue = BoolValue("Color", false)
    private val livingTimeValue = BoolValue("LivingTime", false)
    private val livingTimeTicksValue = object : IntegerValue("LivingTimeTicks", 40, 1, 200) {
        override fun isSupported() = livingTimeValue.get()
    }
    private val groundValue = BoolValue("Ground", true)
    private val airValue = BoolValue("Air", false)
    private val invalidGroundValue = BoolValue("InvalidGround", true)
    private val swingValue = BoolValue("Swing", false)
    private val healthValue = BoolValue("Health", false)
    private val derpValue = BoolValue("Derp", true)
    private val wasInvisibleValue = BoolValue("WasInvisible", false)
    private val armorValue = BoolValue("Armor", false)
    private val pingValue = BoolValue("Ping", false)
    private val needHitValue = BoolValue("NeedHit", false)
    private val duplicateInWorldValue = BoolValue("DuplicateInWorld", false)
    private val duplicateInTabValue = BoolValue("DuplicateInTab", false)
    private val alwaysInRadiusValue = BoolValue("AlwaysInRadius", false)
    private val alwaysRadiusValue = object : FloatValue("AlwaysInRadiusBlocks", 20f, 5f, 30f) {
        override fun isSupported() = alwaysInRadiusValue.get()
    }

    private val ground = mutableListOf<Int>()
    private val air = mutableListOf<Int>()
    private val invalidGround = mutableMapOf<Int, Int>()
    private val swing = mutableListOf<Int>()
    private val invisible = mutableListOf<Int>()
    private val hit = mutableListOf<Int>()
    private val notAlwaysInRadius = mutableListOf<Int>()

    fun isBot(entity: EntityLivingBase): Boolean {
        // Check if entity is a player
        if (entity !is EntityPlayer)
            return false

        // Check if anti bot is enabled
        if (!state)
            return false

        // Anti Bot checks

        if (colorValue.get() && "§" !in entity.displayName.formattedText.replace("§r", ""))
            return true

        if (livingTimeValue.get() && entity.ticksExisted < livingTimeTicksValue.get())
            return true

        if (groundValue.get() && entity.entityId !in ground)
            return true

        if (airValue.get() && entity.entityId !in air)
            return true

        if (swingValue.get() && entity.entityId !in swing)
            return true

        if (healthValue.get() && entity.health > 20F)
            return true

        if (entityIDValue.get() && (entity.entityId >= 1000000000 || entity.entityId <= -1))
            return true

        if (derpValue.get() && (entity.rotationPitch > 90F || entity.rotationPitch < -90F))
            return true

        if (wasInvisibleValue.get() && entity.entityId in invisible)
            return true

        if (armorValue.get()) {
            if (entity.inventory.armorInventory[0] == null && entity.inventory.armorInventory[1] == null &&
                    entity.inventory.armorInventory[2] == null && entity.inventory.armorInventory[3] == null)
                return true
        }

        if (pingValue.get()) {
            if (mc.netHandler.getPlayerInfo(entity.uniqueID)?.responseTime == 0)
                return true
        }

        if (needHitValue.get() && entity.entityId !in hit)
            return true

        if (invalidGroundValue.get() && invalidGround.getOrDefault(entity.entityId, 0) >= 10)
            return true

        if (tabValue.get()) {
            val equals = tabModeValue.get() == "Equals"
            val targetName = stripColor(entity.displayName.formattedText)

            for (networkPlayerInfo in mc.netHandler.playerInfoMap) {
                val networkName = stripColor(networkPlayerInfo.getFullName())

                if (if (equals) targetName == networkName else networkName in targetName)
                    return false
            }

            return true
        }

        if (duplicateInWorldValue.get() &&
            mc.theWorld.loadedEntityList.count { it is EntityPlayer && it.displayNameString == it.displayNameString } > 1) // TODO: I'm 99% certain this doesn't make sense
            return true

        if (duplicateInTabValue.get() &&
            mc.netHandler.playerInfoMap.count { entity.name == stripColor(it.getFullName()) } > 1)
            return true

        if (alwaysInRadiusValue.get() && entity.entityId !in notAlwaysInRadius)
            return true

        return entity.name.isEmpty() || entity.name == mc.thePlayer.name
    }

    override fun onDisable() {
        clearAll()
        super.onDisable()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return

        val packet = event.packet

        if (packet is S14PacketEntity) {
            val entity = packet.getEntity(mc.theWorld)

            if (entity is EntityPlayer) {
                if (entity.onGround && entity.entityId !in ground)
                    ground.add(entity.entityId)

                if (!entity.onGround && entity.entityId !in air)
                    air.add(entity.entityId)

                if (entity.onGround) {
                    if (entity.prevPosY != entity.posY)
                        invalidGround[entity.entityId] = invalidGround.getOrDefault(entity.entityId, 0) + 1
                } else {
                    val currentVL = invalidGround.getOrDefault(entity.entityId, 0) / 2
                    if (currentVL <= 0)
                        invalidGround.remove(entity.entityId)
                    else
                        invalidGround[entity.entityId] = currentVL
                }

                if (entity.isInvisible && entity.entityId !in invisible)
                    invisible.add(entity.entityId)

                if (entity.entityId !in notAlwaysInRadius && mc.thePlayer.getDistanceToEntity(entity) > alwaysRadiusValue.get())
                    notAlwaysInRadius.add(entity.entityId)
            }
        }

        if (packet is S0BPacketAnimation) {
            val entity = mc.theWorld.getEntityByID(packet.entityID)

            if (entity != null && entity is EntityLivingBase && packet.animationType == 0
                    && entity.entityId !in swing)
                swing.add(entity.entityId)
        }
    }

    @EventTarget
    fun onAttack(e: AttackEvent) {
        val entity = e.targetEntity

        if (entity != null && entity is EntityLivingBase && entity.entityId !in hit)
            hit.add(entity.entityId)
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        clearAll()
    }

    private fun clearAll() {
        hit.clear()
        swing.clear()
        ground.clear()
        invalidGround.clear()
        invisible.clear()
        notAlwaysInRadius.clear()
    }

}