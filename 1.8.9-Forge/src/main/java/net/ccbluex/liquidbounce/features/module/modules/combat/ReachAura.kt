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
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.PathUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.Collidable
import net.ccbluex.liquidbounce.utils.block.BlockUtils.bBoxIntersectsBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockLiquid
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
import net.minecraft.potion.Potion
import net.minecraft.util.Vec3
import java.lang.Math.pow
import javax.vecmath.Vector3d
import kotlin.math.sqrt

@ModuleInfo(name = "Reachaura", description = "Experimental: extra reach kill aura (tp hit)",
        category = ModuleCategory.COMBAT)
class ReachAura : Module()
{

    /**
     * OPTIONS
     */

    // PPS:packets per sec
    private val pPS = IntegerValue("PPS",20,0,50)
    private val minPacketsPerGroup = IntegerValue("MinPacketsPerGroup",20,0,50)
    private val noPositionSet = BoolValue("NoPositionSet",true)

    private val rangeValue = FloatValue("Range", 20f, 1f, 100f)
    private val tpDistanceValue = FloatValue("TpDistance",4.0f,0.5f,10.0f)
    private val stopAtDistance = FloatValue("StopAtDistance",4.0f,0.0f, 6.0f)

    private val pathFindingMode = ListValue("PathFindingMode", arrayOf("Simple",
    "NaiveAstarGround","NaiveAstarFly"),"Simple")

    private val targetModeValue = ListValue("TargetMode", arrayOf("Switch", "Multi"), "Switch")

    private var packets = 0.0

    /**
     * MODULE
     */

    // Target
    private var target: EntityLivingBase? = null
    private var targetList: ArrayList<EntityLivingBase>? = null
    private var lastTargetPos: Vec3? = null
    private val prevTargetEntities = mutableListOf<Int>()

    // Bypass
    private val swingValue = BoolValue("Swing", true)

    //queue
    private val reachAuraQueue = mutableListOf<Packet<INetHandlerPlayServer>>()


    override fun onEnable()
    {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        updateTarget()
    }


    /**
     * Disable reach aura module
     */
    override fun onDisable()
    {
        prevTargetEntities.clear()
        packets = 0.0
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

        if (targetList!!.size > 0)
        {
            target = targetList!!.first()
        }
        else if (targetModeValue.get() == "Multi")
            state = false
            //idk wtf, it's a dirty fix
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
    fun onWorldEvent(event: WorldEvent)
    {
        state = false
    }

    @EventTarget
    fun onTick(event: TickEvent)
    {
        while (reachAuraQueue.size < pPS.get() * 0.1)
        {
            if (mc.thePlayer == null || mc.theWorld == null)
            {
                state = false
                return
            }

            if (targetList?.size == 0 || target == null)
            {
                targetList = null
                updateTarget()
                return
            }

            target = targetList!!.first()
            targetList!!.removeAt(0)

            val pos = target!!.positionVector


            runAttack()
            if (targetModeValue.get().equals("Switch"))
                returnInitial(pos)
        }

        packets += (pPS.get() / 20.0)
        if(packets >= minPacketsPerGroup.get())
            while (packets > 0 && reachAuraQueue.size > 0)
            {
                mc.netHandler.addToSendQueue(reachAuraQueue.first())
                reachAuraQueue.removeAt(0)
                packets--
            }
    }

    private fun pathFindToCoord(fromX : Double,fromY : Double,fromZ : Double,
            toX : Double,toY : Double,toZ : Double, fullPath:Boolean = false): MutableList<Vector3d>?
    {
        when (pathFindingMode.get().toLowerCase())
        {
            "simple" ->
            {
                val diffX = toX - fromX
                val diffY = toY - fromY
                val diffZ = toZ - fromZ
                val distance = sqrt(pow(diffX, 2.0) + pow(diffY, 2.0) + pow(diffZ, 2.0))
                val ratio = if (fullPath) 1.0 else (1.0 - (stopAtDistance.get() / distance))

                val endX = fromX + diffX * ratio
                val endY = fromY + diffY * ratio
                val endZ = fromZ + diffZ * ratio

                val path = PathUtils.findPath(fromX,fromY,fromZ,
                        endX,endY,endZ,tpDistanceValue.get().toDouble())

                val verify_path = PathUtils.findPath(fromX,fromY,fromZ,endX,endY,endZ,1.0)

                var valid = true

                val playerbbox = mc.thePlayer.entityBoundingBox


                for (sample in verify_path)
                {
                    val newbbox = playerbbox.offset(sample.x - mc.thePlayer.posX,
                    sample.y - mc.thePlayer.posY, sample.z - mc.thePlayer.posZ)
                    if(bBoxIntersectsBlock(newbbox,
                                    object : Collidable
                                    {
                                        override fun collideBlock(block: Block?) : Boolean
                                        {
                                            val collide = block !is BlockAir
                                            if (collide)
                                                valid = false
                                            return collide
                                        }
                                    }))
                        valid = false
                }

                if (valid) return path else return null
            }

            "naiveastarground" ->
            {

            }

            "naiveastarfly" ->
            {

            }
        }
        TODO("use astar and implement theta star")
    }

    private fun returnInitial(from : Vec3)
    {
        val me = mc.thePlayer
        // TP back
        val path =  pathFindToCoord(from.xCoord,from.yCoord,from.zCoord
        ,me.posX,me.posY,me.posZ,true)

        path ?: return

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

        val path: MutableList<Vector3d>?
        path = if (targetModeValue.get().equals("Switch") || lastTargetPos == null)
        {
            val me = mc.thePlayer
            pathFindToCoord(me.posX, me.posY,me.posZ,
                    target!!.posX, target!!.posY, target!!.posZ)
        }
        else
            pathFindToCoord(lastTargetPos!!.xCoord,lastTargetPos!!.yCoord,lastTargetPos!!.zCoord,
                    target!!.posX, target!!.posY, target!!.posZ)


        path ?: return

        lastTargetPos = target!!.positionVector

        if (path.size != 0)
        {
            for (vec3 in path)
            {
                reachAuraQueue.add(
                        C03PacketPlayer.C04PacketPlayerPosition(vec3.x, vec3.y, vec3.z, false))
            }

            attackEntity(target!!)
        }
    }

    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0 ||
            entity.hurtTime > 5


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

    override val tag:String?
        get() = targetModeValue.get() + ' ' + pathFindingMode.get() + ' ' + reachAuraQueue.size.toString()
}