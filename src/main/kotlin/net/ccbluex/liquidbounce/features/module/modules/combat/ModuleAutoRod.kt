package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRequirements
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoStuck
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.point.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.Items

object ModuleAutoRod : ClientModule("AutoRod", Category.COMBAT) {

    private val rotationMode by enumChoice("RotationMode", RotationMode.LINEAR)
    private val range by floatRange("Range", 3.5f..5f, 3f..10f)
    private val scanExtraRange by floatRange("ScanExtraRange", 0.0f..0.0f, 0.0f..5.0f).onChanged { range ->
        currentScanExtraRange = range.random()
    }
    private var currentScanExtraRange: Float = scanExtraRange.random()
    private val enemiesNearby by int("EnemiesNearby", 1, 1..10)
    private val escapeHealthThreshold by int("EscapeHealthThreshold", 10, 1..20)
    private val pushDelay by int("PushDelay", 100, 50..1000)
    private val pullbackDelay by int("PullbackDelay", 500, 50..1000)
    private val aimOffThreshold by float("AimOffThreshold", 5f, 0.5f..10f)
    private val tickUntilSlotReset by int("TicksUntilSlotReset", 1, 0..20)
    private val selectSlotAutomatically by boolean("SelectSlotAutomatically", true)

    private val requires by multiEnumChoice<KillAuraRequirements>(
        "Requires",
        KillAuraRequirements.VANILLA_NAME
    )
    private fun defaultHoldingItems(): MutableSet<Item> {
        val set = hashSetOf(Items.BOW, Items.CROSSBOW, Items.TRIDENT)
        return set
    }
    private val ignore by multiEnumChoice<Ignore>("Ignore")
    private val holdingItemsForIgnore by items("HoldingItemsForIgnore",defaultHoldingItems())
    private val rotationConfigurable = RotationsConfigurable(this)
    private val targetTracker = tree(TargetTracker(TargetPriority.DISTANCE))
    private val pointTracker = tree(PointTracker(this))
    private val targetRenderer = tree(WorldTargetRenderer(this))

    private val requirementsMet
        get() = requires.all { it.meets() }

    private val pushTimer = Chronometer()
    private val pullbackTimer = Chronometer()
    private var rodInUse = false

    init {
        tree(rotationConfigurable)
        tree(targetTracker)
        tree(pointTracker)
        tree(targetRenderer)
    }

    override val running: Boolean
        get() =
            super.running
                && player.mainHandStack.item !in holdingItemsForIgnore
                && !ModuleBlink.running
                && !ModuleScaffold.running
                && !ModuleAutoStuck.shouldActivate
                && !(Ignore.USING_ITEM !in ignore && player.isUsingItem)
                && !(Ignore.HOLD_CONSUME !in ignore && player.mainHandStack.isConsumable)
                && !(Ignore.OPEN_INVENTORY !in ignore
                && (InventoryManager.isInventoryOpen || mc.currentScreen is GenericContainerScreen))


    private fun HotbarItemSlot.needsSelection(): Boolean =
        this !is OffHandSlot && this.hotbarSlot != SilentHotbar.serversideSlot

    private fun trySelect(slot: HotbarItemSlot): Boolean {
        if (slot.needsSelection()) {
            if (!selectSlotAutomatically) return false
            SilentHotbar.selectSlotSilently(this, slot, tickUntilSlotReset)
            if (slot !is OffHandSlot && SilentHotbar.serversideSlot != slot.hotbarSlotForServer) return false
        }
        return true
    }

    private fun commonChecks(target: LivingEntity?, slot: HotbarItemSlot?): Boolean {
        if (target == null || slot == null) return false
        if (!trySelect(slot)) return false
        return true
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (!requirementsMet) {
            targetTracker.reset()
            targetRenderer.reset()
            return@handler
        }
        val target = targetTracker.selectFirst { enemy ->
            val dist = player.squaredDistanceTo(enemy)
            val maxRange = range.endInclusive + currentScanExtraRange
            if (dist > maxRange * maxRange || dist < range.start * range.start) return@selectFirst false
            player.canSee(enemy)
        } ?: return@handler

        val slot = Slots.OffhandWithHotbar.findSlot(Items.FISHING_ROD) ?: return@handler
        if (!commonChecks(target, slot)) return@handler

        val rotation = findRotation(target, rotationMode) ?: return@handler
        RotationManager.setRotationTarget(
            rotationConfigurable.toRotationTarget(rotation, considerInventory = false),
            Priority.IMPORTANT_FOR_USAGE_1,
            this
        )
    }

    @Suppress("unused")
    private val handleAutoRod = tickHandler {
        val target = targetTracker.target ?: return@tickHandler

        val slot = Slots.OffhandWithHotbar.findSlot(Items.FISHING_ROD) ?: return@tickHandler
        if (!commonChecks(target, slot)) return@tickHandler
        if (rodInUse && pullbackTimer.hasElapsed(pullbackDelay.toLong())) {
            KeyBinding.setKeyPressed(mc.options.useKey.boundKey, false)
            rodInUse = false
            pushTimer.reset()
            currentScanExtraRange = scanExtraRange.random()
            return@tickHandler
        }

        if (rodInUse) return@tickHandler

        val enemiesList = targetTracker.targets()
        if (enemiesList.isEmpty() || enemiesList.size > enemiesNearby || player.health <= escapeHealthThreshold){
            return@tickHandler
        }

        val rotation = findRotation(target, rotationMode) ?: return@tickHandler
        val rotationDifference = RotationManager.serverRotation.angleTo(rotation)
        if (rotationDifference > aimOffThreshold) return@tickHandler

        if (pushTimer.hasElapsed(pushDelay.toLong())) {
            SilentHotbar.selectSlotSilently(this, slot, tickUntilSlotReset)
            interaction.syncSelectedSlot()
            KeyBinding.setKeyPressed(mc.options.useKey.boundKey, true)
            rodInUse = true
            pullbackTimer.reset()
        }

    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val target = targetTracker.target
        if (target == null) {
            targetRenderer.reset()
            return@handler
        }
        renderEnvironmentForWorld(matrixStack) {
            targetRenderer.render(this, target, event.partialTicks)
        }
    }

    private fun findRotation(target: LivingEntity, mode: RotationMode): Rotation? {
        return when (mode) {
            RotationMode.LINEAR -> {
                val eyes = player.eyePos
                val point = pointTracker.findPoint(eyes, target, 1)
                Rotation.lookingAt(point.pos, eyes)
            }
            RotationMode.PROJECTILE -> {
                SituationalProjectileAngleCalculator.calculateAngleForEntity(
                    TrajectoryInfo.FISHING_ROD, target)
            }
        }
    }

    override fun onDisabled() {
        targetTracker.reset()
        targetRenderer.reset()
        rodInUse = false
        interaction.stopUsingItem(player)
    }

    private enum class RotationMode(override val choiceName: String) : NamedChoice {
        LINEAR("Linear"),
        PROJECTILE("Projectile")
    }

    private enum class Ignore(override val choiceName: String) : NamedChoice {
        OPEN_INVENTORY("OpenInventory"),
        USING_ITEM("UsingItem"),
        HOLD_CONSUME("HoldConsume"),
    }

}
