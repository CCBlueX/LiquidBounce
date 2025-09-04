package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRequirements
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationWithVector
import net.ccbluex.liquidbounce.utils.aiming.point.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.aiming.utils.facingEnemy
import net.ccbluex.liquidbounce.utils.block.isPathClearToEntity
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData
import net.minecraft.client.option.KeyBinding
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Items

object ModuleAutoRod : ClientModule("AutoRod", Category.COMBAT) {

    private var range by floatRange("Range", 3.5f..5f, 3f..10f)
    private val scanExtraRange by floatRange("ScanExtraRange", 0.0f..0.0f, 0.0f..5.0f).onChanged { range ->
        currentScanExtraRange = range.random()
    }
    private var currentScanExtraRange: Float = scanExtraRange.random()
    private val enemiesNearby by int("EnemiesNearby", 1, 1..10)
    private val escapeHealthThreshold by int("EscapeHealthThreshold", 10, 1..20)
    private val pushDelay by int("PushDelay", 100, 50..1000)
    private val pullbackDelay by int("PullbackDelay", 500, 50..1000)
    private val aimOffThreshold by float("AimOffThreshold", 2f, 0.5f..10f)
    private val requires by multiEnumChoice<KillAuraRequirements>("Requires")
    private val rotationConfigurable = RotationsConfigurable(this)
    private val targetTracker = tree(TargetTracker(TargetPriority.DIRECTION))
    private val pointTracker = tree(PointTracker(this))
    private val pushTimer = Chronometer()
    private val rodPullTimer = Chronometer()
    private var rodInUse = false
    private var switchBack: Int = -1
    private val requirementsMet
        get() = requires.all { it.meets() }

    init {
        tree(rotationConfigurable)
        tree(targetTracker)
        tree(pointTracker)
    }
    private fun findRotation(target: LivingEntity): Rotation? {
        val eyes = player.eyePos
        val projectileInfo = TrajectoryData.getRenderedTrajectoryInfo(player, Items.FISHING_ROD, true) ?: return null
        val point = pointTracker.findPoint(eyes, target, 0)
        return RotationWithVector(
            SituationalProjectileAngleCalculator.calculateAngleForEntity(projectileInfo, target) ?: return null,
            point.pos
        ).rotation
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> { _ ->
        if (!requirementsMet) {
            targetTracker.reset()
            return@handler
        }

        val fishRod = Slots.OffhandWithHotbar.findSlot(Items.FISHING_ROD)?.hotbarSlotForServer
        if (fishRod == null || player.isUsingItem || rodInUse) return@handler

        val maxRange = range.endInclusive + currentScanExtraRange
        val target = targetTracker.selectFirst { enemy ->
            val dist = player.squaredDistanceTo(enemy)
            if (dist > maxRange * maxRange) return@selectFirst false
            if (dist < range.start * range.start) return@selectFirst false
            val pathClear = isPathClearToEntity(player.eyePos, enemy, 3)
            if (!pathClear) return@selectFirst false
            findRotation(enemy) != null
        } ?: return@handler

        RotationManager.setRotationTarget(
            findRotation(target) ?: return@handler,
            priority = Priority.IMPORTANT_FOR_USAGE_1,
            provider = this@ModuleAutoRod,
            configurable = rotationConfigurable
        )
    }
    override fun onDisabled() {
        KeyBinding.setKeyPressed(mc.options.useKey.boundKey, false)
        targetTracker.reset()
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val usingRod = (player.isUsingItem && player.mainHandStack?.item == Items.FISHING_ROD) || rodInUse
        val fishRod = Slots.OffhandWithHotbar.findSlot(Items.FISHING_ROD)?.hotbarSlotForServer

        if ( fishRod == null || player.isUsingItem || KillAuraAutoBlock.blockVisual) return@tickHandler

        if (usingRod) {
            if (rodPullTimer.hasElapsed(pullbackDelay.toLong())) {
                KeyBinding.setKeyPressed(mc.options.useKey.boundKey, false)

                if (switchBack != -1 && player.inventory?.selectedSlot != switchBack) {
                    player.inventory?.selectedSlot = switchBack
                    interaction.syncSelectedSlot()
                } else {
                    player.stopUsingItem()
                }

                switchBack = -1
                rodInUse = false
                pushTimer.reset()
            }
        } else {
            var rod = false
            val enemiesList = targetTracker.targets()

            if (enemiesList.isNotEmpty()) {
                if (enemiesList.size <= enemiesNearby || player.health <= escapeHealthThreshold) {
                    if (shouldRod(enemiesList)) {
                        rod = true
                    }
                }
            }

            if (rod && pushTimer.hasElapsed(pushDelay.toLong())) {
                if (player.mainHandStack?.item != Items.FISHING_ROD) {
                    val rodSlot = Slots.Hotbar.findSlot(Items.FISHING_ROD)?.hotbarSlot
                    if (rodSlot == null) {
                        return@tickHandler
                    }

                    switchBack = player.inventory.selectedSlot
                    SilentHotbar.selectSlotSilently(
                        this, rodSlot,
                        ticksUntilReset = 1
                    )
                    interaction.syncSelectedSlot()
                }
                rod()
            }
        }
    }

    private fun shouldRod(enemiesList: List<LivingEntity>): Boolean {
        val target = enemiesList.firstOrNull() ?: return false
        val rotation = RotationManager.currentRotation ?: player.rotation
        val isFacingEnemy = facingEnemy(
            toEntity = target,
            rotation = rotation,
            range = range.endInclusive.toDouble(),
            wallsRange = 0.0
        )
        val rotationDifference = RotationManager.serverRotation.angleTo(rotation)
        return isFacingEnemy && rotationDifference <= aimOffThreshold
    }

    private fun rod() {
        val rodSlot = Slots.Hotbar.findSlot(Items.FISHING_ROD)?.hotbarSlot ?: return

        player.inventory?.selectedSlot = rodSlot
        KeyBinding.setKeyPressed(mc.options.useKey.boundKey, true)
        rodInUse = true
        rodPullTimer.reset()
        currentScanExtraRange = scanExtraRange.random()
    }
}
