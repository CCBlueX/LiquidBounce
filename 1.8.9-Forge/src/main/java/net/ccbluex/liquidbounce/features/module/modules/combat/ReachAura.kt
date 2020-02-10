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
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.block.BlockAir
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.potion.Potion
import net.minecraft.util.BlockPos

@ModuleInfo(name = "Reachaura", description = "Experimental: extra reach kill aura (tp hit)",
        category = ModuleCategory.COMBAT)
class ReachAura : Module()
{
    /**
     * OPTIONS
     */

    // CPS - Attack Speed
    private val maxCPS: IntegerValue = object : IntegerValue("MaxCPS", 8, 1, 20)
    {
        override fun onChanged(oldValue: Int, newValue: Int)
        {
            val i = minCPS.get()
            if (i > newValue) set(i)

            attackDelay = TimeUtils.randomClickDelay(minCPS.get(), this.get())
        }
    }

    private val minCPS: IntegerValue = object : IntegerValue("MinCPS", 5, 1, 20)
    {
        override fun onChanged(oldValue: Int, newValue: Int)
        {
            val i = maxCPS.get()
            if (i < newValue) set(i)

            attackDelay = TimeUtils.randomClickDelay(this.get(), maxCPS.get())
        }
    }

    private val rangeValue = FloatValue("Range", 20f, 1f, 100f)
    private val tpDistanceValue = FloatValue("TpDistance",4.0f,0.5f,10.0f)

    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0L
    private var clicks = 0

    /**
     * MODULE
     */

    // Target
    private var target: EntityLivingBase? = null
    private var targetList: ArrayList<EntityLivingBase>? = null
    private val prevTargetEntities = mutableListOf<Int>()

    // Bypass
    private val swingValue = BoolValue("Swing", true)

    // initial pos
    private var initialx: Double? = null
    private var initialy: Double? = null
    private var initialz: Double? = null

    // WTF
    private var firsttick = true


    override fun onEnable()
    {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        initialx = mc.thePlayer.posX
        initialy = mc.thePlayer.posY
        initialz = mc.thePlayer.posZ

        updateTarget()
    }

    @EventTarget
    private fun onWorldEvent(event: TickEvent)
    {
        if (!firsttick) return

        initialx = mc.thePlayer.posX
        initialy = mc.thePlayer.posY
        initialz = mc.thePlayer.posZ

        firsttick = false
    }


    /**
     * Disable reach aura module
     */
    override fun onDisable()
    {
        prevTargetEntities.clear()
        attackTimer.reset()
        clicks = 0
        target = null
        targetList = null
        returnInitial()
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
    fun onRender3D(event: Render3DEvent)
    {
        if (attackTimer.hasTimePassed(attackDelay))
        {
            clicks++
            attackTimer.reset()
            attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get())
        }
    }


    private fun update()
    {
        if (targetList?.size == 0)
        {
            targetList = null
            updateTarget()

        }

    }

    @EventTarget
    fun onUpdate(event: UpdateEvent)
    {
        update()

        target = targetList?.first()
        while (clicks > 0)
        {
            targetList?.removeAt(0)
            if (isTpable(target!!))
            {
                runAttack()
                clicks--
            }
        }
        returnInitial()
    }

    private fun returnInitial()
    {
        // TP back
        for (vector3d in PathUtils.findPath(initialx!!, initialy!!, initialz!!, tpDistanceValue.get().toDouble()))
        {
            mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(
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
            mc.netHandler.addToSendQueue(C0DPacketCloseWindow())

        // TP to entity
        for (vector3d in PathUtils.findPath(target!!.posX, target!!.posY, target!!.posZ, tpDistanceValue.get().toDouble()))
        {
            mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(
                    vector3d.getX(), vector3d.getY(), vector3d.getZ(), false))
        }

        attackEntity(target!!)
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
        mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))


        if (mc.thePlayer.fallDistance > 0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder &&
                !mc.thePlayer.isInWater && !mc.thePlayer.isPotionActive(Potion.blindness) && !mc.thePlayer.isRiding)
            mc.thePlayer.onCriticalHit(entity)

        // Enchant Effect
        if (EnchantmentHelper.getModifierForCreature(mc.thePlayer.heldItem, entity.creatureAttribute) > 0F)
            mc.thePlayer.onEnchantmentCritical(entity)
    }


}