package net.ccbluex.liquidbounce.features.module.modules.world.autoFarm

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.RotatedMovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.item.getHotbarItems
import net.ccbluex.liquidbounce.utils.item.hasInventorySpace
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.ItemEntity
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

object AutoFarmAutoWalk : ToggleableConfigurable(ModuleAutoFarm, "AutoWalk", false){

    // Makes the player move to farmland blocks where there is a need for crop replacement
    private val toPlace by boolean("ToPlace", true)

    private val toItems by boolean("ToItems", true)

    private var invHadSpace = true

    private var walkTarget: Vec3d? = null

    private fun findWalkToItem() =
        world.entities.filter {it is ItemEntity && it.distanceTo(player) < 20}
            .minByOrNull { it.distanceTo(player) }?.pos
    fun updateWalkTarget(): Boolean {
        if (!enabled)
            return false

        val invHasSpace = hasInventorySpace()
        if(!invHasSpace && invHadSpace && toItems){
            notification("Inventory is Full", "autoFarm wont walk to items", NotificationEvent.Severity.ERROR)
        }
        invHadSpace = invHasSpace

        val walkToBlock = findWalkToBlock()
        val walkToItem = findWalkToItem()

        walkTarget = if (toItems && invHasSpace) {
            arrayOf(findWalkToBlock(), findWalkToItem())
                .minByOrNull { it?.squaredDistanceTo(player.pos) ?: Double.MAX_VALUE }
        } else {
            walkToBlock
        }

        val target = walkTarget ?: run {
            walkTarget = null
            return false
        }
        RotationManager.aimAt(RotationManager.makeRotation(target, player.eyes), configurable = ModuleAutoFarm.rotations)
        return true
    }

    private fun findWalkToBlock(): Vec3d?{

        if(AutoFarmBlockTracker.trackedBlockMap.isEmpty()){
            return null
        }

        val allowedItems = arrayOf(true, false, false)
        // 1. true: we should always walk to blocks we want to destroy because we can do so even without any items
        // 2. false: we should only walk to farmland blocks if we got the needed items
        // 3. false: same as 2. only go if we got the needed items for souldsand (netherwarts)
        val hotbarItems = getHotbarItems()
        for (item in hotbarItems){
            if(item in ModuleAutoFarm.itemsForFarmland) allowedItems[1] = true
            else if(item in ModuleAutoFarm.itemsForSoulsand) allowedItems[2] = true
        }

        val closestBlock =
            AutoFarmBlockTracker.trackedBlockMap
                .filter { allowedItems[it.value.ordinal] }
                .keys
                .map { Vec3d.ofCenter(Vec3i(it.x, it.y, it.z)) }
                .minByOrNull { it.distanceTo(player.pos)}

        return closestBlock
    }

    private fun shouldWalk() = (walkTarget != null && mc.currentScreen is HandledScreen<*>)

    val horizontalMovementHandling = handler<RotatedMovementInputEvent> { event ->
        if (!shouldWalk())
            return@handler

        event.forward = 1f

        player.isSprinting = true
    }

    val verticalMovementHandling = handler<MovementInputEvent> { event ->
        if (!shouldWalk())
            return@handler

        // We want to swim up in water, so we don't drown and can move onwards
        if (player.isTouchingWater) {
            event.jumping = true
        }
    }
}

