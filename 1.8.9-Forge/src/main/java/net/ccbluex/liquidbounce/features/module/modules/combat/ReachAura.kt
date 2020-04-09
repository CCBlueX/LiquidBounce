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
import net.ccbluex.liquidbounce.utils.astar.Astar
import net.ccbluex.liquidbounce.utils.astar.NaiveAstarFlyNode
import net.ccbluex.liquidbounce.utils.astar.NaiveAstarGroundNode
import net.ccbluex.liquidbounce.utils.astar.NaiveAstarNode
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.Collidable
import net.ccbluex.liquidbounce.utils.block.BlockUtils.bBoxIntersectsBlock
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Block
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
import net.minecraft.potion.Potion
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import java.awt.Color
import javax.vecmath.Vector3d
import kotlin.math.min
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
    private val pPS = IntegerValue("PPS", 13, 0, 50)
    private val minPacketsPerGroup = IntegerValue("MinPacketsPerGroup", 0, 0, 50)
    private val noPositionSet = BoolValue("IgnorePositionSet", false)

    private val rangeValue = FloatValue("Range", 20f, 1f, 100f)
    private val tpDistanceValue = FloatValue("TpDistance", 4.0f, 0.5f, 10.0f)
    private val stopAtDistance = FloatValue("StopAtDistance", 0.0f, 0.0f, 6.0f)

    private val pathFindingMode = ListValue("PathFindingMode", arrayOf("Simple",
            "NaiveAstarGround", "NaiveAstarFly"), "Simple")
    private val rayCastLessNode = BoolValue("RayCastLessNode",true)

    private val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "Direction", "LivingTime"), "Distance")

    private val renderPath = BoolValue("RenderPath", true)
    private val astarTimeout = IntegerValue("AstarTimeout", 20, 10, 1000)

    private val pretend = BoolValue("Pretend", false)

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

        if (pathFindingMode.get() == "NaiveAstarGround")
        {
            var y = mc.thePlayer.posY.toInt()

            while (y > 0 && BlockUtils.getBlock(BlockPos(mc.thePlayer.posX, y.toDouble(), mc.thePlayer.posZ)) is BlockAir)
            {
                y--
            }

            val path = PathUtils.findPath(mc.thePlayer.posX, y.toDouble(), mc.thePlayer.posZ, 1.0)

            if (path!!.size == 0)
            {
                state = false;return
            }

            for (i in path)
                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(i.x, i.y, i.z, false))

            mc.thePlayer.posY = y.toDouble()
        }

        updateTarget()
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
        reachAuraQueue.clear()
        lastTargetPos = null
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
            if (priorityValue.get().equals("Distance") && targetModeValue.get().equals("Multi") && target != null)
            {
                targetList.sortBy {
                    (it!!.posX - target!!.posX).pow(2) +
                            (it!!.posY - target!!.posY).pow(2) +
                            (it!!.posZ - target!!.posZ).pow(2)
                }
            } else

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

        val render_mgr = mc.renderManager

        for (i in reachAuraQueue)
        {
            if (i is C03PacketPlayer.C04PacketPlayerPosition)
            {
                val dist = sqrt((i.x - mc.thePlayer.posX).pow(2.0) + (i.y - mc.thePlayer.posY).pow(2.0) + (i.z - mc.thePlayer.posZ).pow(2.0))


                RenderUtils.drawAxisAlignedBB(mc.thePlayer.entityBoundingBox.offset(i.x - mc.thePlayer.posX - render_mgr.renderPosX,
                        i.y - mc.thePlayer.posY - render_mgr.renderPosY, i.z - mc.thePlayer.posZ - render_mgr.renderPosZ)
                        , Color(((1.0 - (maxRange - dist) / maxRange) * 255.0).toInt(), (((maxRange - dist) / maxRange) * 255.0).toInt(), 30, 50))
            }
        }

        RenderUtils.drawAxisAlignedBB(target!!.entityBoundingBox.offset(-render_mgr.renderPosX,
            -render_mgr.renderPosY,-render_mgr.renderPosZ), Color(86,156,214,170))
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook && noPositionSet.get())
        {
            event.cancelEvent()
            positionSetList.add(packet)
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

            if (targetList.size == 0 || target == null)
            {
                targetList.clear()//when did i write this???? wtf
                if (targetModeValue.get() == "Multi")
                {
                    returnInitial(lastTargetPos!!)
                    lastTargetPos = null
                }
                updateTarget()
                return
            }


            if (targetModeValue.get() == "Single")
                updateTarget()

            target = targetList.first()
            if (targetModeValue.get() != "Single")
                targetList.removeAt(0)

            val pos = target!!.positionVector

            if (runAttack() && targetModeValue.get() != "Multi") //Short circuit exists in && ?
                returnInitial(pos)
        }

        packets += (pPS.get() / 20.0)
        if (packets >= minPacketsPerGroup.get())
            while (packets > 0 && reachAuraQueue.size > 0)
            {
                val first = reachAuraQueue.first()
                if (first is C03PacketPlayer.C04PacketPlayerPosition
                        && (first.x.isNaN() || first.y.isNaN() || first.z.isNaN()))
                {
                    reachAuraQueue.removeAt(0)
                    packets--
                    continue
                }

                if (!pretend.get())
                    mc.netHandler.addToSendQueue(first)
                reachAuraQueue.removeAt(0)
                packets--
            }
    }

    private fun pathFindToCoord(fromX: Double, fromY: Double, fromZ: Double,
                                toX: Double, toY: Double, toZ: Double, fullPath: Boolean = false): MutableList<Vector3d>?
    {
        target ?: return null

        val lowerCasePathfindString = pathFindingMode.get().toLowerCase()

        if (lowerCasePathfindString == "simple")
        {
            val diffX = toX - fromX
            val diffY = toY - fromY
            val diffZ = toZ - fromZ
            val distance = sqrt(diffX.pow(2.0) + diffY.pow(2.0) + diffZ.pow(2.0))
            val ratio = if (fullPath) 1.0 else (1.0 - (stopAtDistance.get() / distance))

            val endX = fromX + diffX * ratio
            val endY = fromY + diffY * ratio
            val endZ = fromZ + diffZ * ratio

            val pair = raycastBBox(fromX, fromY, fromZ, endX, endY, endZ)
            val path = pair.first
            val valid = pair.second

            if (valid) return path else return null
        } else if (pathFindingMode.get().toLowerCase().contains("naiveastar"))
        {
            val ground = lowerCasePathfindString.contains("ground")

            val begin = if (ground)
                NaiveAstarGroundNode(fromX.toInt(), fromY.toInt(), fromZ.toInt())
            else
                NaiveAstarFlyNode(fromX.toInt(), fromY.toInt(), fromZ.toInt())


            val end = if (ground)
                NaiveAstarGroundNode(toX.toInt(), toY.toInt(), toZ.toInt())
            else
                NaiveAstarFlyNode(toX.toInt(), toY.toInt(), toZ.toInt())


            val nodes = Astar.find_path(begin, end,
                    { current, end ->
                        val c = current as NaiveAstarNode
                        val e = end as NaiveAstarNode
                        val dist = sqrt((c.x - e.x).toDouble().pow(2.0) + (c.y - e.y).toDouble().pow(2.0) + (c.z - e.z).toDouble().pow(2.0))
                        (dist < stopAtDistance.get() && !fullPath) || dist < 2
                    }, astarTimeout.get()) as ArrayList<NaiveAstarNode>

            var path = mutableListOf<Vector3d>()
            for (i in nodes)
                path.add(Vector3d(i.get_pos().xCoord, i.get_pos().yCoord, i.get_pos().zCoord))

            val rayCastLength = tpDistanceValue.maximum.toInt()
            if (rayCastLessNode.get())
            {
                val tmp = mutableListOf<Vector3d>()
                tmp.add(path.first())

                var rayCastBegin = 0
                while (rayCastBegin < path.size)
                {
                    var pathValid = false
                    var rayCastEnd = min(rayCastBegin + rayCastLength,path.size - 1)
                    val begin = path[rayCastBegin]

                    while (rayCastEnd > rayCastBegin)
                    {
                        val end = path[rayCastEnd]
                        val rayCastResult = raycastBBox(begin.x,begin.y,begin.z,end.x,end.y,end.z)
                        if (rayCastResult.second && sqrt((begin.x-end.x).pow(2)+(begin.y-end.y).pow(2)+(begin.z-end.z).pow(2)) < tpDistanceValue.get())
                        {
                            tmp.add(path[rayCastEnd])
                            rayCastBegin = rayCastEnd
                            pathValid = true
                            break
                        }
                            else
                        rayCastEnd--
                    }

                    if (pathValid)
                        continue
                    else
                        rayCastBegin++
                }
                path = tmp
            }

            return if (path.size != 0) path else null
        }

        return null // I don't want to implement theta* lol
    }

    private fun raycastBBox(fromX: Double, fromY: Double, fromZ: Double, endX: Double, endY: Double, endZ: Double): Pair<MutableList<Vector3d>, Boolean>
    {
        val path = PathUtils.findPath(fromX, fromY, fromZ,
                endX, endY, endZ, tpDistanceValue.get().toDouble())

        val verify_path = PathUtils.findPath(fromX, fromY, fromZ, endX, endY, endZ, 1.0)

        var valid = true

        val playerbbox = mc.thePlayer.entityBoundingBox


        for (sample in (verify_path + path))
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
        return Pair(path, valid)
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

            return false
        }

        val openInventory = mc.currentScreen is GuiInventory
        // Close inventory when open
        if (openInventory)
            reachAuraQueue.add(C0DPacketCloseWindow())

        // TP to entity

        val path: MutableList<Vector3d>?
        path = if (targetModeValue.get() != "Multi" || lastTargetPos == null)
        {
            val me = mc.thePlayer
            pathFindToCoord(me.posX, me.posY, me.posZ,
                    target!!.posX, target!!.posY, target!!.posZ)
        } else
            pathFindToCoord(lastTargetPos!!.xCoord, lastTargetPos!!.yCoord, lastTargetPos!!.zCoord,
                    target!!.posX, target!!.posY, target!!.posZ)

        lastTargetPos = target!!.positionVector

        if (mc.thePlayer.getDistanceToEntity(target) < stopAtDistance.get() || path!!.size != 0)
        {
            path ?: return false

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