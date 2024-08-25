package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlacementPlan
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.inventory.Hotbar
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i

object ModuleExtinguish: Module("Extinguish", Category.WORLD) {

    private val cooldown by float("Cooldown", 1.0F, 0.0F..20.0F, "s")
    private val notDuringCombat by boolean("NotDuringCombat", true)

    private object Pickup : ToggleableConfigurable(ModuleExtinguish, "Pickup", true) {
        val pickupSpan by floatRange("PickupSpan", 0.1F..10.0F, 0.0F..20.0F, "s")
    }

    init {
        tree(Pickup)
    }

    private var currentTarget: PlacementPlan? = null

    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    private val cooldownTimer = Chronometer()

    private var lastExtinguishPos: BlockPos? = null
    private val lastAttemptTimer = Chronometer()

    val tickMovementHandler = handler<SimulatedTickEvent> {
        this.currentTarget = null

        val target = findAction() ?: return@handler

        this.currentTarget = target

        RotationManager.aimAt(
            target.placementTarget.rotation,
            configurable = rotationsConfigurable,
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
            provider = ModuleNoFall
        )
    }

    private fun findAction(): PlacementPlan? {
        val pickupSpanStart = (Pickup.pickupSpan.start * 1000.0F).toLong()
        val pickupSpanEnd = (Pickup.pickupSpan.endInclusive * 1000.0F).toLong()

        if (lastExtinguishPos != null && lastAttemptTimer.hasElapsed(pickupSpanEnd)) {
            lastExtinguishPos = null
        }
        if (notDuringCombat && CombatManager.isInCombat) {
            return null
        }

        val pickupPos = this.lastExtinguishPos

        if (pickupPos != null && Pickup.enabled && this.lastAttemptTimer.hasElapsed(pickupSpanStart)) {
            planPickup(pickupPos)?.let {
                return it
            }
        }

        if (!player.isOnFire || !cooldownTimer.hasElapsed()) {
            return null
        }

        return planExtinguishing()
    }

    val repeatable = repeatable {
        val target = currentTarget ?: return@repeatable

        val rayTraceResult = raycast() ?: return@repeatable

        if (!target.doesCorrespondTo(rayTraceResult)) {
            return@repeatable
        }

        SilentHotbar.selectSlotSilently(this, target.hotbarItemSlot.hotbarSlotForServer, 1)

        val successFunction = {
            cooldownTimer.waitForAtLeast((cooldown * 1000.0F).toLong())
            lastAttemptTimer.reset()

            lastExtinguishPos = target.placementTarget.placedBlock

            true
        }

        doPlacement(rayTraceResult, onItemUseSuccess = successFunction, onPlacementSuccess = successFunction)
    }

    private fun planExtinguishing(): PlacementPlan? {
        val waterBucketSlot = Hotbar.findClosestItem(Items.WATER_BUCKET) ?: return null

        val simulation = PlayerSimulationCache.getSimulationForLocalPlayer()

        val frameOnGround = simulation.simulateBetween(0..20).firstOrNull {
            it.onGround
        } ?: return null

        val playerPos = frameOnGround.pos.toBlockPos()

        val options = BlockPlacementTargetFindingOptions(
            listOf(Vec3i(0, 0, 0)),
            waterBucketSlot.itemStack,
            CenterTargetPositionFactory,
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            frameOnGround.pos
        )

        val bestPlacementPlan = findBestBlockPlacementTarget(playerPos, options) ?: return null

        return PlacementPlan(playerPos, bestPlacementPlan, waterBucketSlot)
    }

    private fun planPickup(blockPos: BlockPos): PlacementPlan? {
        val bucket = Hotbar.findClosestItem(Items.BUCKET) ?: return null

        val options = BlockPlacementTargetFindingOptions(
            listOf(Vec3i(0, 0, 0)),
            bucket.itemStack,
            CenterTargetPositionFactory,
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            player.pos
        )

        val bestPlacementPlan = findBestBlockPlacementTarget(blockPos, options) ?: return null

        return PlacementPlan(blockPos, bestPlacementPlan, bucket)
    }

}
