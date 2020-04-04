/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import com.sun.security.ntlm.Client
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
import net.ccbluex.liquidbounce.utils.astar.Astar
import net.ccbluex.liquidbounce.utils.astar.NaiveAstarFlyNode
import net.ccbluex.liquidbounce.utils.astar.NaiveAstarGroundNode
import net.ccbluex.liquidbounce.utils.astar.NaiveAstarNode
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.Collidable
import net.ccbluex.liquidbounce.utils.block.BlockUtils.bBoxIntersectsBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils
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
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.potion.Potion
import net.minecraft.util.Vec3
import scala.collection.script.Update
import java.awt.Color
import java.lang.Math.abs
import java.lang.Math.pow
import javax.vecmath.Vector3d
import kotlin.math.nextDown
import kotlin.math.pow
import kotlin.math.sqrt

@ModuleInfo(name = "Reachaura", description = "Experimental: extra reach kill aura (tp hit)",
        category = ModuleCategory.COMBAT)
class ReachAura : Module()
{

    /**
     * OPTIONS
     */

    // PPS:packets per sec
    private val pPS = IntegerValue("PPS", 20, 0, 50)
    private val minPacketsPerGroup = IntegerValue("MinPacketsPerGroup", 20, 0, 50)
    private val noPositionSet = BoolValue("NoPositionSet", true)

    private val rangeValue = FloatValue("Range", 20f, 1f, 100f)
    private val tpDistanceValue = FloatValue("TpDistance", 4.0f, 0.5f, 10.0f)
    private val stopAtDistance = FloatValue("StopAtDistance", 4.0f, 0.0f, 6.0f)

    private val pathFindingMode = ListValue("PathFindingMode", arrayOf("Simple",
            "NaiveAstarGround", "NaiveAstarFly"), "Simple")

    private val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "Direction", "LivingTime"), "Distance")

    private val renderPath = BoolValue("RenderPath", true)
    private val astarTimeout = IntegerValue("AstarTimeout",20,10,1000)

    private var packets = 0.0
    private val positionSetList = mutableListOf<S08PacketPlayerPosLook>()

    /**
     * MODULE
     */

    // Target
    private var target: EntityLivingBase? = null
    private var targetList = mutableListOf<EntityLivingBase?>()
    private var lastTargetPos: Vec3? = null

    // Bypass
    private val swingValue = BoolValue("Swing", true)

    //queue
    private val reachAuraQueue = mutableListOf<Packet<INetHandlerPlayServer>>()


    override fun onEnable()
    {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        updateTarget()

        //if (pathFindingMode.get()=="NaiveAstarGround")
            TODO("make it teleport to ground before all starts")
    }


    /**
     * Disable reach aura module
     */
    override fun onDisable()
    {
        if (lastTargetPos != null)
            returnInitial(lastTargetPos!!)
        packets = 0.0
        target = null
        targetList.clear()
    }

    /**
     * Range
     */
    private val maxRange: Float
        get() = rangeValue.get()

    private fun updateTarget()
    {
        target = null

        for (entity in mc.theWorld.loadedEntityList)
        {
            if (entity !is EntityLivingBase || !isEnemy(entity))
                continue

            val dist = mc.thePlayer.getDistanceToEntityBox(entity)

            if (dist <= maxRange)
                targetList.add(entity)
        }

        if (targetList.size > 0)
        {
            if (priorityValue.get().equals("Distance") && targetModeValue.get().equals("Multi") && lastTargetPos != null)
            {
                targetList.sortBy {
                    (it!!.posX - lastTargetPos!!.xCoord).pow(2) +
                            (it!!.posY - lastTargetPos!!.yCoord).pow(2) +
                            (it!!.posZ - lastTargetPos!!.zCoord).pow(2)
                }
            }
            else

                when (priorityValue.get().toLowerCase())
                {
                    "distance" -> targetList.sortBy { mc.thePlayer.getDistanceToEntityBox(it!!) } // Sort by distance
                    "health" -> targetList.sortBy { it!!.health } // Sort by health
                    "direction" -> targetList.sortBy { RotationUtils.getRotationDifference(it) } // Sort by FOV
                    "livingtime" -> targetList.sortBy { -it!!.ticksExisted } // Sort by existence
                }

            target = targetList.first()

        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent)
    {
        if (!renderPath.get())
            return
        for (i in reachAuraQueue)
        {
            if (i is C03PacketPlayer.C04PacketPlayerPosition)
            {
                val dist = sqrt((i.x - mc.thePlayer.posX).pow(2.0) + (i.y - mc.thePlayer.posY).pow(2.0) + (i.z - mc.thePlayer.posZ).pow(2.0))

                val render_mgr = mc.renderManager

                RenderUtils.drawAxisAlignedBB(mc.thePlayer.entityBoundingBox.offset(i.x - mc.thePlayer.posX - render_mgr.renderPosX,
                        i.y - mc.thePlayer.posY - render_mgr.renderPosY, i.z - mc.thePlayer.posZ - render_mgr.renderPosZ)
                        , Color((255F * dist / rangeValue.get()).toInt(), (255F * (1F - dist / rangeValue.get())).toInt(), 30, 50))
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook && noPositionSet.get())
        {
            var avgPos = Vec3(0.0, 0.0, 0.0)

            for (i in positionSetList)
            {
                avgPos.addVector(i.x / positionSetList.size,
                        i.y / positionSetList.size,
                        i.z / positionSetList.size)
            }



            if ((avgPos.xCoord - packet.x).pow(2.0) + (avgPos.yCoord - packet.y).pow(2.0) + (avgPos.zCoord - packet.z)
                    > 3.0.pow(2.0) || positionSetList.size < 3)
            {
                event.cancelEvent()
                positionSetList.add(packet)
            } else
            {
                ClientUtils.displayChatMessage("Failed to return to position")

                reachAuraQueue.clear()

                mc.thePlayer.posX = packet.x
                mc.thePlayer.posY = packet.y
                mc.thePlayer.posZ = packet.z

                state = false
            }

            if (positionSetList.size > 6) positionSetList.removeAt(0)
        }
    }

    @EventTarget
    fun onWorldEvent(event: WorldEvent)
    {
        state = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent)
    {
        updateTarget()
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

            if (targetList.size == 0 || target == null)
            {
                targetList.clear()
                updateTarget()
                return
            }

            target = targetList.first()
            if (!priorityValue.get().equals("Single"))
                targetList.removeAt(0)

            val pos = target!!.positionVector

            if (runAttack() && !targetModeValue.get().equals("Multi")) //Short circuit exists in && ?
                returnInitial(pos)
        }

        packets += (pPS.get() / 20.0)
        if (packets >= minPacketsPerGroup.get())
            while (packets > 0 && reachAuraQueue.size > 0)
            {
                val first = reachAuraQueue.first()
                if (first is C03PacketPlayer.C04PacketPlayerPosition
                        && (first.x.isNaN() ||first.y.isNaN() ||first.z.isNaN()))
                {
                    reachAuraQueue.removeAt(0)
                    packets--
                    continue
                }

                mc.netHandler.addToSendQueue(first)
                reachAuraQueue.removeAt(0)
                packets--
            }
    }

    private fun pathFindToCoord(fromX: Double, fromY: Double, fromZ: Double,
                                toX: Double, toY: Double, toZ: Double, fullPath: Boolean = false): MutableList<Vector3d>?
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

                val path = PathUtils.findPath(fromX, fromY, fromZ,
                        endX, endY, endZ, tpDistanceValue.get().toDouble())

                val verify_path = PathUtils.findPath(fromX, fromY, fromZ, endX, endY, endZ, 1.0)

                var valid = true

                val playerbbox = mc.thePlayer.entityBoundingBox


                for (sample in verify_path)
                {
                    val newbbox = playerbbox.offset(sample.x - mc.thePlayer.posX,
                            sample.y - mc.thePlayer.posY, sample.z - mc.thePlayer.posZ)
                    if (bBoxIntersectsBlock(newbbox,
                                    object : Collidable
                                    {
                                        override fun collideBlock(block: Block?): Boolean
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
                val begin = NaiveAstarGroundNode(mc.thePlayer.posX.toInt(), mc.thePlayer.posY.toInt(), mc.thePlayer.posZ.toInt())
                val end = NaiveAstarGroundNode(target!!.posX.toInt(), target!!.posY.toInt(), target!!.posZ.toInt())

                begin ?: return null
                end ?: return null

                val nodes = Astar.find_path(begin, end,
                        { current, end ->
                            val c = current as NaiveAstarGroundNode
                            val e = end as NaiveAstarGroundNode
                            val dist = sqrt(pow((c.x - e.x).toDouble(), 2.0) + pow((c.y - e.y).toDouble(), 2.0) + pow((c.z - e.z).toDouble(), 2.0))
                            (dist < stopAtDistance.get() && fullPath) || dist < 1
                        }, 20) as ArrayList<NaiveAstarNode>

                val path = mutableListOf<Vector3d>()
                for (i in nodes)
                    path.add(Vector3d(i.get_pos().xCoord, i.get_pos().yCoord, i.get_pos().zCoord))

                return if (path.size != 0) path else null
            }

            "naiveastarfly" ->
            {
                val begin = NaiveAstarFlyNode(mc.thePlayer.posX.toInt(), mc.thePlayer.posY.toInt(), mc.thePlayer.posZ.toInt())
                val end = NaiveAstarFlyNode(target!!.posX.toInt(), target!!.posY.toInt(), target!!.posZ.toInt())

                begin ?: return null
                end ?: return null

                val nodes = Astar.find_path(begin, end,
                        { current, end ->
                            val c = current as NaiveAstarFlyNode
                            val e = end as NaiveAstarFlyNode
                            val dist = sqrt(pow((c.x - e.x).toDouble(), 2.0) + pow((c.y - e.y).toDouble(), 2.0) + pow((c.z - e.z).toDouble(), 2.0))
                            (dist < stopAtDistance.get() && fullPath) || dist < 1
                        }, 20) as ArrayList<NaiveAstarNode>

                val path = mutableListOf<Vector3d>()
                for (i in nodes)
                    path.add(Vector3d(i.get_pos().xCoord, i.get_pos().yCoord, i.get_pos().zCoord))

                return if (path.size != 0) path else null
            }
        }
        TODO("implement theta star")
    }

    private fun returnInitial(from: Vec3)
    {
        val me = mc.thePlayer
        // TP back
        val path = pathFindToCoord(from.xCoord, from.yCoord, from.zCoord
                , me.posX, me.posY, me.posZ, true)

        path ?: return

        if (path.size != 0)
            for (vector3d in path)
            {
                reachAuraQueue.add(C03PacketPlayer.C04PacketPlayerPosition(
                        vector3d.getX(), vector3d.getY(), vector3d.getZ(), false))
            }
    }


    private fun runAttack(): Boolean
    {
        target ?: return false

        if (target !in mc.theWorld.loadedEntityList)
        {
            target = null

            updateTarget()
            return false
        }

        val openInventory = mc.currentScreen is GuiInventory
        // Close inventory when open
        if (openInventory)
            reachAuraQueue.add(C0DPacketCloseWindow())

        // TP to entity

        val path: MutableList<Vector3d>?
        path = if (!targetModeValue.get().equals("Multi") || lastTargetPos == null)
        {
            val me = mc.thePlayer
            pathFindToCoord(me.posX, me.posY, me.posZ,
                    target!!.posX, target!!.posY, target!!.posZ)
        } else
            pathFindToCoord(lastTargetPos!!.xCoord, lastTargetPos!!.yCoord, lastTargetPos!!.zCoord,
                    target!!.posX, target!!.posY, target!!.posZ)


        path ?: return false

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

        return true
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

    override val tag: String?
        get() = targetModeValue.get() + ' ' + pathFindingMode.get() + ' ' + reachAuraQueue.size.toString()
}