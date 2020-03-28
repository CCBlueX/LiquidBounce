/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.PathUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.block.BlockAir
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.potion.Potion
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import java.util.zip.DeflaterOutputStream
import javax.vecmath.Vector3d

@ModuleInfo(name = "Reachaura", description = "Experimental: extra reach kill aura (tp hit)",
        category = ModuleCategory.COMBAT)
class ReachAura : Module()
{
    /**
     * OPTIONS
     */

    // PPS:packets per sec
    private val pPS = IntegerValue("PPS",100,0,400)
    private val minPacketsPerGroup = IntegerValue("MinPacketsPerGroup",20,0,100)
    private val noPositionSet = BoolValue("NoPositionSet",true)

    private val rangeValue = FloatValue("Range", 20f, 1f, 100f)
    private val tpDistanceValue = FloatValue("TpDistance",4.0f,0.5f,10.0f)
    private val stopAtDistance = FloatValue("StopAtDistance",4.0f,0.0f, 6.0f)

    // Attack delay
    //private val attackTimer = MSTimer()
    //private var attackDelay = 0L
    //private var clicks = 0
    private var packets = 0

    /**
     * MODULE
     */

    // Target
    private var target: EntityLivingBase? = null
    private var targetList: ArrayList<EntityLivingBase>? = null
    private val prevTargetEntities = mutableListOf<Int>()

    // Bypass
    private val swingValue = BoolValue("Swing", true)


    // WTF
    private var firsttick = true

    //queue
    private val reachAuraQueue = mutableListOf<Packet<INetHandlerPlayServer>>()


    override fun onEnable()
    {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        updateTarget()
    }

    @EventTarget
    private fun onWorldEvent(event: TickEvent)
    {
        if (!firsttick) return

        firsttick = false

        state = false
    }


    /**
     * Disable reach aura module
     */
    override fun onDisable()
    {
        prevTargetEntities.clear()
        packets = 0
        if (target != null)
            returnInitial(target!!.positionVector)
        target = null
        targetList = null
    }

    /**
     * Range
     */
    private val maxRange: Float
        get() = rangeValue.get()

    private fun updateTarget()
    {
        target = null
        targetList = ArrayList<EntityLivingBase>()

        for (entity in mc.theWorld.loadedEntityList)
        {
            if (entity !is EntityLivingBase || !isEnemy(entity) || (prevTargetEntities.contains(entity.entityId)))
                continue

            val dist = mc.thePlayer.getDistanceToEntityBox(entity)

            if (dist <= maxRange)
                targetList!!.add(entity)
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook && noPositionSet.get())
        {
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onTick(event: TickEvent)
    {
        if (reachAuraQueue.size < pPS.get() * 5)
        {
            if (mc.thePlayer == null || mc.theWorld == null)
            {
                state = false
                return
            }

            if (targetList?.size == 0)
            {
                targetList = null
                updateTarget()

            }

            target = targetList?.first()
            targetList?.removeAt(0)


            var pos = target!!.positionVector
            runAttack()
            returnInitial(pos)
        }

        packets += (pPS.get() / 20 + 1)
        if(packets >= minPacketsPerGroup.get())
            while (packets > 0 && reachAuraQueue.size > 0)
            {
                mc.netHandler.addToSendQueue(reachAuraQueue.first())
                reachAuraQueue.removeAt(0)
                packets--
            }
    }

    private fun pathFindToCoord(fromX : Double,fromY : Double,fromZ : Double,
            toX : Double,toY : Double,toZ : Double): MutableList<Vector3d>?
    {

        return PathUtils.findPath(fromX,fromY,fromZ,
                toX,toY,toZ,tpDistanceValue.get().toDouble())
    }

    private fun returnInitial(from : Vec3)
    {
        var me = mc.thePlayer
        // TP back
        var path =  pathFindToCoord(from.xCoord,from.yCoord,from.zCoord
        ,me.posX,me.posY,me.posZ)!!

        if (path.size != 0)
            for (vector3d in path)
            {
                reachAuraQueue.add(C03PacketPlayer.C04PacketPlayerPosition(
                        vector3d.getX(), vector3d.getY(), vector3d.getZ(), false))
            }
    }


    private fun runAttack()
    {
        target ?: return
        targetList ?: return

        if (target !in mc.theWorld.loadedEntityList)
        {
            target = null

            updateTarget()
            return
        }

        val openInventory = mc.currentScreen is GuiInventory
        // Close inventory when open
        if (openInventory)
           reachAuraQueue.add(C0DPacketCloseWindow())

        // TP to entity
        val me = mc.thePlayer
        val path = pathFindToCoord(me.posX,me.posY,me.posZ,
                target!!.posX, target!!.posY, target!!.posZ)!!
        if (path.size != 0)
        {
            for (vec3 in path)
            {
                reachAuraQueue.add(
                        C03PacketPlayer.C04PacketPlayerPosition(vec3.x, vec3.y, vec3.z, true))
            }

            attackEntity(target!!)
        }
    }

    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0 ||
            entity.hurtTime > 5


    private fun isTpable(entity: EntityLivingBase): Boolean
    {
        for (vector3d in PathUtils.findPath(entity.posX,entity.posY,entity.posZ, 1.0))
        {
            var pass = BlockUtils.getBlock(BlockPos(vector3d.x,vector3d.y + 2,vector3d.z)) is BlockAir &&
                    BlockUtils.getBlock(BlockPos(vector3d.x,vector3d.y + 1,vector3d.z)) is BlockAir
            if (!pass) return false
        }
        return true
    }



    /**
     * Check if [entity] is selected as enemy with current target options and other modules
     */
    private fun isEnemy(entity: Entity?): Boolean
    {
        if (entity is EntityLivingBase && (EntityUtils.targetDead || isAlive(entity)) && entity != mc.thePlayer)
        {
            if (!EntityUtils.targetInvisible && entity.isInvisible())
                return false

            if (EntityUtils.targetPlayer && entity is EntityPlayer)
            {
                if (entity.isSpectator || AntiBot.isBot(entity))
                    return false

                if (EntityUtils.isFriend(entity) && !LiquidBounce.moduleManager[NoFriends::class.java]!!.state)
                    return false

                val teams = LiquidBounce.moduleManager[Teams::class.java] as Teams

                return !teams.state || !teams.isInYourTeam(entity)
            }

            return EntityUtils.targetMobs && EntityUtils.isMob(entity) || EntityUtils.targetAnimals &&
                    EntityUtils.isAnimal(entity)
        }

        return false
    }

    /**
     * Attack [entity]
     */
    private fun attackEntity(entity: EntityLivingBase)
    {

        // Call attack event
        LiquidBounce.eventManager.callEvent(AttackEvent(entity))

        // Attack target
        if (swingValue.get())
            mc.thePlayer.swingItem()
        reachAuraQueue.add(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))


        if (mc.thePlayer.fallDistance > 0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder &&
                !mc.thePlayer.isInWater && !mc.thePlayer.isPotionActive(Potion.blindness) && !mc.thePlayer.isRiding)
            mc.thePlayer.onCriticalHit(entity)

        // Enchant Effect
        if (EnchantmentHelper.getModifierForCreature(mc.thePlayer.heldItem, entity.creatureAttribute) > 0F)
            mc.thePlayer.onEnchantmentCritical(entity)
    }


}