/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.network.INetworkPlayerInfo
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import java.util.*


@ModuleInfo(name = "AntiBot", description = "Prevents KillAura from attacking AntiCheat bots.", category = ModuleCategory.MISC)
class AntiBot : Module() {
    private val tabValue = BoolValue("Tab", true)
    private val tabModeValue = ListValue("TabMode", arrayOf("Equals", "Contains"), "Contains")
    private val entityIDValue = BoolValue("EntityID", true)
    private val colorValue = BoolValue("Color", false)
    private val livingTimeValue = BoolValue("LivingTime", false)
    private val livingTimeTicksValue = IntegerValue("LivingTimeTicks", 40, 1, 200)
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
    private val allwaysInRadiusValue = BoolValue("AlwaysInRadius", false)
    private val allwaysRadiusValue = FloatValue("AlwaysInRadiusBlocks", 20f, 5f, 30f)

    private val ground: MutableList<Int> = ArrayList()
    private val air: MutableList<Int> = ArrayList()
    private val invalidGround: MutableMap<Int, Int> = HashMap()
    private val swing: MutableList<Int> = ArrayList()
    private val invisible: MutableList<Int> = ArrayList()
    private val hitted: MutableList<Int> = ArrayList()
    private val notAlwaysInRadius: MutableList<Int> = ArrayList()

    override fun onDisable() {
        clearAll()
        super.onDisable()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return

        val packet = event.packet

        if (classProvider.isSPacketEntity(packet)) {
            val packetEntity = packet.asSPacketEntity()
            val entity = packetEntity.getEntity(mc.theWorld!!)

            if (classProvider.isEntityPlayer(entity) && entity != null) {
                if (packetEntity.onGround && !ground.contains(entity.entityId))
                    ground.add(entity.entityId)
                if (!packetEntity.onGround && !air.contains(entity.entityId))
                    air.add(entity.entityId)
                if (packetEntity.onGround) {
                    if (entity.prevPosY != entity.posY)
                        invalidGround[entity.entityId] = invalidGround.getOrDefault(entity.entityId, 0) + 1
                } else {
                    val currentVL = invalidGround.getOrDefault(entity.entityId, 0) / 2
                    if (currentVL <= 0)
                        invalidGround.remove(entity.entityId)
                    else
                        invalidGround[entity.entityId] = currentVL
                }
                if (entity.invisible && !invisible.contains(entity.entityId)) invisible.add(entity.entityId)

                if (!notAlwaysInRadius.contains(entity.entityId) && mc.thePlayer!!.getDistanceToEntity(entity) > allwaysRadiusValue.get())
                    notAlwaysInRadius.add(entity.entityId);

            }
        }
        if (classProvider.isSPacketAnimation(packet)) {
            val packetAnimation = packet.asSPacketAnimation()
            val entity = mc.theWorld!!.getEntityByID(packetAnimation.entityID)
            if (entity != null && classProvider.isEntityLivingBase(entity) && packetAnimation.animationType == 0 && !swing.contains(entity.entityId)) swing.add(entity.entityId)
        }
    }

    @EventTarget
    fun onAttack(e: AttackEvent) {
        val entity = e.targetEntity
        if (entity != null && classProvider.isEntityLivingBase(entity) && !hitted.contains(entity.entityId)) hitted.add(entity.entityId)
    }

    @EventTarget
    fun onWorld(event: WorldEvent?) {
        clearAll()
    }

    private fun clearAll() {
        hitted.clear()
        swing.clear()
        ground.clear()
        invalidGround.clear()
        invisible.clear()
        notAlwaysInRadius.clear();
    }

    companion object {
        @JvmStatic
        fun isBot(entity: IEntityLivingBase): Boolean {
            if (!classProvider.isEntityPlayer(entity)) return false
            val antiBot = LiquidBounce.moduleManager.getModule(AntiBot::class.java) as AntiBot?
            if (antiBot == null || !antiBot.state) return false
            if (antiBot.colorValue.get() && !entity.displayName!!.formattedText.replace("§r", "").contains("§")) return true
            if (antiBot.livingTimeValue.get() && entity.ticksExisted < antiBot.livingTimeTicksValue.get()) return true
            if (antiBot.groundValue.get() && !antiBot.ground.contains(entity.entityId)) return true
            if (antiBot.airValue.get() && !antiBot.air.contains(entity.entityId)) return true
            if (antiBot.swingValue.get() && !antiBot.swing.contains(entity.entityId)) return true
            if (antiBot.healthValue.get() && entity.health > 20f) return true
            if (antiBot.entityIDValue.get() && (entity.entityId >= 1000000000 || entity.entityId <= -1)) return true
            if (antiBot.derpValue.get() && (entity.rotationPitch > 90f || entity.rotationPitch < -90f)) return true
            if (antiBot.wasInvisibleValue.get() && antiBot.invisible.contains(entity.entityId)) return true
            if (antiBot.armorValue.get()) {
                val player = entity.asEntityPlayer()
                if (player.inventory.armorInventory[0] == null && player.inventory.armorInventory[1] == null && player.inventory.armorInventory[2] == null && player.inventory.armorInventory[3] == null) return true
            }
            if (antiBot.pingValue.get()) {
                val player = entity.asEntityPlayer()
                if (mc.netHandler.getPlayerInfo(player.uniqueID)!!.responseTime == 0) return true
            }
            if (antiBot.needHitValue.get() && !antiBot.hitted.contains(entity.entityId)) return true
            if (antiBot.invalidGroundValue.get() && antiBot.invalidGround.getOrDefault(entity.entityId, 0) >= 10) return true
            if (antiBot.tabValue.get()) {
                val equals = antiBot.tabModeValue.get().equals("Equals", ignoreCase = true)
                val targetName = stripColor(entity.displayName!!.formattedText)
                if (targetName != null) {
                    for (networkPlayerInfo in mc.netHandler.playerInfoMap) {
                        val networkName = stripColor(EntityUtils.getName(networkPlayerInfo)) ?: continue
                        if (if (equals) targetName == networkName else targetName.contains(networkName)) return false
                    }
                    return true
                }
            }
            if (antiBot.duplicateInWorldValue.get()) {
                if (mc.theWorld!!.loadedEntityList.stream()
                                .filter { currEntity: IEntity ->
                                    classProvider.isEntityPlayer(currEntity) && currEntity.asEntityPlayer()
                                            .displayNameString == currEntity.asEntityPlayer().displayNameString
                                }
                                .count() > 1) return true
            }
            if (antiBot.duplicateInTabValue.get()) {
                if (mc.netHandler.playerInfoMap.stream()
                                .filter { networkPlayer: INetworkPlayerInfo? -> entity.name == stripColor(EntityUtils.getName(networkPlayer)) }
                                .count() > 1) return true
            }
            if (antiBot.allwaysInRadiusValue.get() && !antiBot.notAlwaysInRadius.contains(entity.entityId))
                return true;
            return entity.name!!.isEmpty() || entity.name == mc.thePlayer!!.name
        }
    }
}