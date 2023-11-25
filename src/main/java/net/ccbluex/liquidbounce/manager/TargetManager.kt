package net.ccbluex.liquidbounce.manager

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase

object TargetManager : Listenable, MinecraftInstance() {
    var target: EntityLivingBase? = null
        private set
    var inCombat = false
        private set
    private val lastAttackTimer = MSTimer()
    val attackedEntityList = mutableListOf<EntityLivingBase>()

    init {
        LiquidBounce.eventManager.registerListener(this)
    }

    override fun handleEvents() = true

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer == null) return

        inCombat = false

        if (!lastAttackTimer.hasTimePassed(1000)) {
            inCombat = true
            return
        }

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityLivingBase && entity.getDistanceToEntity(mc.thePlayer) < 7 && EntityUtils.isSelected(
                    entity, true
                ) && !entity.isDead
            ) {
                inCombat = true
                break
            }
        }

        if (target != null) {
            if (mc.thePlayer.getDistanceToEntity(target) > 7 || !inCombat || target!!.isDead) {
                target = null
            }
        }

        attackedEntityList.map { it }.forEach {
            if (it.isDead) {
                attackedEntityList.remove(it)
            }
        }
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        val target = event.targetEntity

        if (target is EntityLivingBase && EntityUtils.isSelected(target, true)) {
            this.target = target
            if (!attackedEntityList.contains(target)) attackedEntityList.add(target)
        }
        lastAttackTimer.reset()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        inCombat = false
        target = null
        attackedEntityList.clear()
    }

    fun getNearByEntity(radius: Float): EntityLivingBase? {
        return try {
            mc.theWorld.loadedEntityList.filter {
                mc.thePlayer.getDistanceToEntity(it) < radius && EntityUtils.isSelected(
                    it, true
                )
            }.sortedBy { it.getDistanceToEntity(mc.thePlayer) }[0] as EntityLivingBase?
        } catch (e: Exception) {
            null
        }
    }
}