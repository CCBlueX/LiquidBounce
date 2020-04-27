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
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.astar.Astar
import net.ccbluex.liquidbounce.utils.astar.NaiveAstarFlyNode
import net.ccbluex.liquidbounce.utils.astar.NaiveAstarGroundNode
import net.ccbluex.liquidbounce.utils.astar.NaiveAstarNode
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.bBoxIntersectsBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isBlockPassable
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
import kotlin.math.*

/**
 * ReachAura
 *@author commandblock2
 */

@ModuleInfo(
    name = "ReachAura", description = "ReachAura or TpAura or InfiniteAura, the glitchy version",
    category = ModuleCategory.COMBAT
)
class ReachAura : Module() {

    /**
     * OPTIONS
     */

    // PPS:packets per sec
    private val pPS = IntegerValue("PPS", 18, 0, 50)

    private val rangeValue = FloatValue("Range", 20f, 1f, 100f)
    private val tpDistanceValue = FloatValue("TpDistance", 4.0f, 0.5f, 10.0f)
    private val stopAtDistance = FloatValue("StopAtDistance", 0.0f, 0.0f, 6.0f)

    private val pathFindingMode = ListValue(
        "PathFindingMode", arrayOf(
            "Simple",
            "NaiveAstarGround", "NaiveAstarFly"
        ), "NaiveAstarFly"
    )

    private val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val priorityValue =
        ListValue("Priority", arrayOf("Health", "Distance", "Direction", "LivingTime"), "Distance")

    private val renderPath = BoolValue("RenderPath", true)

    private val rescheduleOnMove = BoolValue("rescheduleOnMove", true)
    private val rayCastLessNode = BoolValue("RayCastLessNode", true)
    private val astarTimeout = IntegerValue("AstarTimeout", 20, 10, 1000)
    private val disableOnReset = BoolValue("DisableOnReset", false)
    private val pretend = BoolValue("Pretend", false)
    val pulse = BoolValue("Pulse", true)

    private var packets = 0.0
    private var lastPacketPos: Vec3? = null
    private var attackPacketsCache = mutableListOf<C03PacketPlayer.C04PacketPlayerPosition>()

    /**
     * MODULE
     */

    // Target
    private var target: EntityLivingBase? = null
    private var targetList = mutableListOf<EntityLivingBase?>()
    private var lastTargetPos: Vec3? = null
    private var skipTick = 0

    // Bypass
    private val swingValue = BoolValue("Swing", true)

    //queue
    private val reachAuraQueue = mutableListOf<Packet<INetHandlerPlayServer>>()

    /**
     * Enable ReachAura module
     */

    override fun onEnable() {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        if (pathFindingMode.get() == "NaiveAstarGround") {
            var y = mc.thePlayer.posY.toInt()

            while (y > 0 && BlockUtils.getBlock(
                    BlockPos(
                        mc.thePlayer.posX,
                        y.toDouble(),
                        mc.thePlayer.posZ
                    )
                ) is BlockAir
            ) {
                y--
            }

            val path = PathUtils.findPath(mc.thePlayer.posX, y.toDouble(), mc.thePlayer.posZ, 1.0)

            if (path!!.size == 0) {
                state = false;return
            }

            for (i in path)
                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(i.x, i.y, i.z, true))

            mc.thePlayer.posY = y.toDouble()
        }

        lastPacketPos = mc.thePlayer.positionVector

        updateTarget()
    }


    /**
     * Disable reach aura module
     */
    override fun onDisable() {
        if (lastTargetPos != null && targetModeValue.get() == "Multi")
            returnInitial(lastTargetPos!!)
        packets = 0.0
        target = null
        targetList.clear()
        reachAuraQueue.clear()
        lastTargetPos = null
    }

    private fun returnInitial(from: Vec3): Boolean {
        val me = mc.thePlayer
        // TP back
        val path = pathFindToCoord(
            from.xCoord, from.yCoord, from.zCoord
            , me.posX, me.posY, me.posZ
        )

        path ?: return false

        path += Vector3d(me.posX, me.posY, me.posZ)

        if (path.size != 0)

            for (vector3d in path) {
                if (isNodeValid(vector3d))
                    reachAuraQueue += C03PacketPlayer.C04PacketPlayerPosition(
                        vector3d.getX(), vector3d.getY(), vector3d.getZ(), true
                    )
            }
        return true
    }

    /**
     * Range
     */
    private val maxRange: Float
        get() = rangeValue.get()

    /**
     *  updates the target list and current target
     */

    private fun updateTarget() {
        target = null
        targetList.clear()

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || !isEnemy(entity))
                continue

            val dist = mc.thePlayer.getDistanceToEntityBox(entity)

            if (dist <= maxRange)
                targetList.add(entity)
        }

        if (targetList.size > 0) {
            if (priorityValue.get() == "Distance" && targetModeValue.get() == "Multi" && lastTargetPos != null) {
                targetList.sortBy {
                    PathUtils.getDistance(
                        it!!.posX,
                        it.posY,
                        it.posZ,
                        lastTargetPos!!.xCoord,
                        lastTargetPos!!.yCoord,
                        lastTargetPos!!.zCoord
                    )
                }
            } else

                when (priorityValue.get().toLowerCase()) {
                    "distance" -> targetList.sortBy { mc.thePlayer.getDistanceToEntityBox(it!!) } // Sort by distance
                    "health" -> targetList.sortBy { it!!.health } // Sort by health
                    "direction" -> targetList.sortBy { RotationUtils.getRotationDifference(it) } // Sort by FOV
                    "livingtime" -> targetList.sortBy { -it!!.ticksExisted } // Sort by existence
                }

            target = targetList.first()

        } else {
            skipTick = 3
            reachAuraQueue.clear()
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!renderPath.get())
            return

        val renderMgr = mc.renderManager

        var index = 0

        for (i in reachAuraQueue) {
            if (i is C03PacketPlayer.C04PacketPlayerPosition) {
                val dist = PathUtils.getDistance(i.x, i.y, i.z, mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)

                val red = max(min(((1.0 - (maxRange - dist) / maxRange) * 255.0).toInt(), 255), 0)
                val green = max(min((((maxRange - dist) / maxRange) * 255.0).toInt(), 255), 0)
                val blue = max(min((index.toDouble() / reachAuraQueue.size * 255.0).toInt(), 255), 0)
                index++


                RenderUtils.drawAxisAlignedBB(
                    mc.thePlayer.entityBoundingBox.offset(
                        i.x - mc.thePlayer.posX - renderMgr.renderPosX,
                        i.y - mc.thePlayer.posY - renderMgr.renderPosY, i.z - mc.thePlayer.posZ - renderMgr.renderPosZ
                    )
                    , Color(red, green, blue, 30)
                )
            }
        }

        if (target != null)
            RenderUtils.drawAxisAlignedBB(
                target!!.entityBoundingBox.offset(
                    -renderMgr.renderPosX,
                    -renderMgr.renderPosY, -renderMgr.renderPosZ
                ), Color(86, 156, 214, 170)
            )
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook && disableOnReset.get()) {
            state = false
        }
    }

    @EventTarget
    fun onWorldEvent(event: WorldEvent) {
        state = false
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        var scheduled = false

        if (skipTick > 0) {
            skipTick--
            return
        }

        while (reachAuraQueue.size < pPS.get() * 0.3) {
            if (mc.thePlayer == null || mc.theWorld == null) {
                state = false
                return
            }

            if (targetList.size == 0 || target == null) {
                targetList.clear()//when did i write this???? wtf
                if (targetModeValue.get() == "Multi" && lastTargetPos != null) {
                    if (returnInitial(lastTargetPos!!))
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

            schedule()
            scheduled = true
        }

        packets += (pPS.get() / 20.0)

        val begin = reachAuraQueue.first()
        if (!scheduled && pulse.get() && rescheduleOnMove.get() && packets > reachAuraQueue.size && reachAuraQueue.size > 0)
            if (begin is C03PacketPlayer.C04PacketPlayerPosition && !(begin.x.isNaN() || begin.y.isNaN() || begin.z.isNaN()) &&
                (mc.thePlayer.getDistance(
                    begin.positionX,
                    begin.positionY,
                    begin.z
                ) > tpDistanceValue.get() || //if the player moves too much
                        target != null && lastPacketPos != null &&
                        target!!.getDistance(
                            lastTargetPos!!.xCoord,
                            lastTargetPos!!.yCoord,
                            lastTargetPos!!.zCoord
                        ) > stopAtDistance.get()) //or the target moves too much
            ) {
                // reschedule
                reachAuraQueue.clear()
                schedule()
            }

        while (packets > 0 && reachAuraQueue.size > 0 && (!pulse.get() || packets > reachAuraQueue.size)) {
            val first = reachAuraQueue.first()
            if (first is C03PacketPlayer.C04PacketPlayerPosition
                && (first.x.isNaN() || first.y.isNaN() || first.z.isNaN())
            ) {
                if (BlockUtils.getBlock(BlockPos(first.x, first.y - 1, first.z)) is BlockAir)
                    first.onGround = false

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

    private fun schedule() {
        if (runAttack() && targetModeValue.get() != "Multi") //Short circuit exists in && ?
            tpBack()
    }

    private fun pathFindToCoord(
        fromX: Double, fromY: Double, fromZ: Double,
        toX: Double, toY: Double, toZ: Double
    ): MutableList<Vector3d>? {
        target ?: return null

        val lowerCasePathfindString = pathFindingMode.get().toLowerCase()

        if (lowerCasePathfindString == "simple") {
            val diffX = toX - fromX
            val diffY = toY - fromY
            val diffZ = toZ - fromZ
            val distance = sqrt(diffX.pow(2.0) + diffY.pow(2.0) + diffZ.pow(2.0))
            val ratio = (1.0 - (stopAtDistance.get() / distance))

            val endX = fromX + diffX * ratio
            val endY = fromY + diffY * ratio
            val endZ = fromZ + diffZ * ratio

            val pair = raycastPlayerBBox(fromX, fromY, fromZ, endX, endY, endZ)
            val path = pair.first
            val valid = pair.second

            return if (valid) path else null
        } else if (pathFindingMode.get().toLowerCase().contains("naiveastar")) {
            val ground = lowerCasePathfindString.contains("ground")

            val begin = if (ground)
                NaiveAstarGroundNode(floor(fromX).toInt(), floor(fromY).toInt(), floor(fromZ).toInt())
            else
                NaiveAstarFlyNode(floor(fromX).toInt(), floor(fromY).toInt(), floor(fromZ).toInt())


            val end = if (ground)
                NaiveAstarGroundNode(floor(toX).toInt(), floor(toY).toInt(), floor(toZ).toInt())
            else
                NaiveAstarFlyNode(floor(toX).toInt(), floor(toY).toInt(), floor(toZ).toInt())


            val nodes = Astar.findPath(begin, end,
                { current, end ->
                    val c = current as NaiveAstarNode
                    val e = end as NaiveAstarNode
                    val dist = PathUtils.getDistance(
                        c.x.toDouble(),
                        c.y.toDouble(),
                        c.z.toDouble(),
                        e.x.toDouble(),
                        e.y.toDouble(),
                        e.z.toDouble()
                    )
                    dist < stopAtDistance.get()
                }, astarTimeout.get()
            ) as ArrayList<NaiveAstarNode>

            var path = mutableListOf<Vector3d>()
            for (i in nodes)
                path.add(Vector3d(i.getPos().xCoord, i.getPos().yCoord, i.getPos().zCoord))

            val rayCastLength = tpDistanceValue.maximum.toInt() * 2
            if (rayCastLessNode.get() && path.size != 0) {
                val tmp = mutableListOf<Vector3d>()
                tmp.add(path.first())

                var rayCastBegin = 0
                while (rayCastBegin < path.size) {
                    var pathValid = false
                    var rayCastEnd = min(rayCastBegin + rayCastLength, path.size - 1)
                    val begin = path[rayCastBegin]

                    while (rayCastEnd > rayCastBegin) {
                        val end = path[rayCastEnd]
                        val rayCastResult = raycastPlayerBBox(begin.x, begin.y, begin.z, end.x, end.y, end.z)
                        if (rayCastResult.second && PathUtils.getDistance(
                                begin.x,
                                begin.y,
                                begin.z,
                                end.x,
                                end.y,
                                end.z
                            ) < tpDistanceValue.get()
                        ) {
                            tmp.add(path[rayCastEnd])
                            rayCastBegin = rayCastEnd
                            pathValid = true
                            break
                        } else
                            rayCastEnd--
                    }

                    if (pathValid)
                        continue
                    else {
                        tmp.add(path[rayCastBegin])
                        rayCastBegin++
                    }
                }
                path = tmp
            }

            return if (path.size != 0) path else null
        }

        return null // I am too noob to implement theta* lol
    }

    /**
     *  Check if the player can tp from a coord to another coord
     */

    private fun raycastPlayerBBox(
        fromX: Double,
        fromY: Double,
        fromZ: Double,
        endX: Double,
        endY: Double,
        endZ: Double
    ): Pair<MutableList<Vector3d>, Boolean> {
        val path = PathUtils.findPath(
            fromX, fromY, fromZ,
            endX, endY, endZ, tpDistanceValue.get().toDouble()
        )

        val verifyPath = PathUtils.findPath(fromX, fromY, fromZ, endX, endY, endZ, 0.1)

        var valid = true

        val playerBBox = mc.thePlayer.entityBoundingBox


        for (sample in (verifyPath + path)) {
            val newBBox = playerBBox.offset(
                sample.x - mc.thePlayer.posX,
                sample.y - mc.thePlayer.posY, sample.z - mc.thePlayer.posZ
            ).expand(0.1, 0.0, 0.1)
            if (bBoxIntersectsBlock(newBBox,
                    object : BlockUtils.Collidable {
                        override fun collideBlock(block: Block?): Boolean {
                            val collide = !isBlockPassable(block)
                            if (collide)
                                valid = false
                            return collide
                        }
                    })
            )
                valid = false
        }
        return Pair(path, valid)
    }

    private fun isNodeValid(vec3: Vector3d): Boolean {
        if (reachAuraQueue.size == 0) return true

        val lastQueuePacket: C03PacketPlayer.C04PacketPlayerPosition =
            (when {
                reachAuraQueue.last() is C03PacketPlayer.C04PacketPlayerPosition -> reachAuraQueue.last()
                reachAuraQueue[reachAuraQueue.size - 2] is C03PacketPlayer.C04PacketPlayerPosition -> reachAuraQueue[reachAuraQueue.size - 2]
                else -> null
            }
                    ) as C03PacketPlayer.C04PacketPlayerPosition?
                ?: return true

        return PathUtils.getDistance(
            vec3.x,
            vec3.y,
            vec3.z,
            lastQueuePacket.x,
            lastQueuePacket.y,
            lastQueuePacket.z
        ) < 10
    }

    private fun tpBack() {
        val me = mc.thePlayer
        // TP back
        reachAuraQueue += attackPacketsCache
        attackPacketsCache.clear()
    }


    private fun runAttack(): Boolean {
        target ?: return false

        if (target !in mc.theWorld.loadedEntityList) {
            target = null

            return false
        }

        val openInventory = mc.currentScreen is GuiInventory
        // Close inventory when open
        if (openInventory)
            reachAuraQueue.add(C0DPacketCloseWindow())

        // TP to entity

        val path: MutableList<Vector3d>?
        path = if (targetModeValue.get() != "Multi" || lastTargetPos == null) {
            val me = mc.thePlayer
            pathFindToCoord(
                me.posX, me.posY, me.posZ,
                target!!.posX, target!!.posY, target!!.posZ
            )
        } else
            pathFindToCoord(
                lastTargetPos!!.xCoord, lastTargetPos!!.yCoord, lastTargetPos!!.zCoord,
                target!!.posX, target!!.posY, target!!.posZ
            )

        lastTargetPos = if (path != null) Vec3(path.last().x, path.last().y, path.last().z) else lastTargetPos

        if (mc.thePlayer.getDistanceToEntity(target) < stopAtDistance.get() || path != null) {
            if (path != null)
                for (vec3 in path) {
                    if (isNodeValid(vec3)) {
                        reachAuraQueue.add(C03PacketPlayer.C04PacketPlayerPosition(vec3.x, vec3.y, vec3.z, true))
                        attackPacketsCache.add(0, C03PacketPlayer.C04PacketPlayerPosition(vec3.x, vec3.y, vec3.z, true))
                    }
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
    private fun isEnemy(entity: Entity?): Boolean {
        if (entity is EntityLivingBase && (EntityUtils.targetDead || isAlive(entity)) && entity != mc.thePlayer) {
            if (!EntityUtils.targetInvisible && entity.isInvisible())
                return false

            if (EntityUtils.targetPlayer && entity is EntityPlayer) {
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
    private fun attackEntity(entity: EntityLivingBase) {

        // Call attack event
        LiquidBounce.eventManager.callEvent(AttackEvent(entity))

        // Attack target
        if (swingValue.get())
            mc.thePlayer.swingItem()
        reachAuraQueue.add(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))


        if (mc.thePlayer.fallDistance > 0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder &&
            !mc.thePlayer.isInWater && !mc.thePlayer.isPotionActive(Potion.blindness) && !mc.thePlayer.isRiding
        )
            mc.thePlayer.onCriticalHit(entity)

        // Enchant Effect
        if (EnchantmentHelper.getModifierForCreature(mc.thePlayer.heldItem, entity.creatureAttribute) > 0F)
            mc.thePlayer.onEnchantmentCritical(entity)
    }

    override val tag: String?
        get() = targetModeValue.get() + ' ' + pathFindingMode.get() + ' ' + reachAuraQueue.size.toString()
}