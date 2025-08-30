package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.MouseRotationEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRequirements
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationWithVector
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.InterpolationAngleSmooth
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.LinearAngleSmooth
import net.ccbluex.liquidbounce.utils.aiming.point.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.preference.LeastDifferencePreference
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.aiming.utils.setRotation
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.Entity
import net.minecraft.util.math.MathHelper

/**
 * Aimbot module
 *
 * Automatically faces selected entities around you.
 */
@Suppress("MagicNumber")
object ModuleAimbot : ClientModule("Aimbot", Category.COMBAT, aliases = arrayOf("AimAssist", "AutoAim")) {

    private val range = float("Range", 4.2f, 1f..8f)

    val targetTracker = tree(TargetTracker(TargetPriority.DIRECTION, range = range))
    private val targetRenderer = tree(WorldTargetRenderer(this))
    private val pointTracker = tree(PointTracker(this))

    private val requires by multiEnumChoice<KillAuraRequirements>("Requires")

    private val requirementsMet
        get() = requires.all { it.meets() }

    private var angleSmooth = choices(this, "AngleSmooth") {
        arrayOf(
            InterpolationAngleSmooth(it),
            LinearAngleSmooth(it)
        )
    }

    private val ignores by multiEnumChoice<IgnoreOpened>("Ignore")

    private var lastTargetEntity: Entity? = null
    private var targetRotation: Rotation? = null
    private var playerRotation: Rotation? = null
    var mouseDeltaX = 0f
    var mouseDeltaY = 0f

    @Suppress("unused", "ComplexCondition")
    private val tickHandler = handler<RotationUpdateEvent> { _ ->
        playerRotation = player.rotation

        if (!requirementsMet) {
            targetTracker.reset()
            targetRenderer.reset()
            targetRotation = null

            if (!KillAuraRequirements.CLICK.meets()) {
                lastTargetEntity = null
            }
            return@handler
        }
        targetRotation = findNextTargetRotation()?.let { (target, rotation) ->
            lastTargetEntity = target
            angleSmooth.activeChoice.process(
                RotationTarget(
                    rotation = rotation.rotation,
                    entity = target,
                    processors = listOf(angleSmooth.activeChoice),
                    ticksUntilReset = 1,
                    resetThreshold = 1f,
                    considerInventory = true,
                    movementCorrection = MovementCorrection.CHANGE_LOOK
                ),
                player.rotation,
                rotation.rotation
            )
        }

        // Reset renderer if no target is found
        if (targetRotation == null) {
            if (KillAuraRequirements.ONLY_ON_ROTATE in requires && (mouseDeltaX != 0f || mouseDeltaY != 0f)) {
                lastTargetEntity = null
            }
            targetRenderer.reset()
        }

        // Reset mouse delta after checking
        mouseDeltaX = 0f
        mouseDeltaY = 0f

        // Update Auto Weapon
        ModuleAutoWeapon.onTarget(targetTracker.target)
    }

    override fun onDisabled() {
        targetTracker.reset()
        targetRenderer.reset() // Ensure renderer is reset when module is disabled
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val partialTicks = event.partialTicks
        val target = targetTracker.target ?: lastTargetEntity ?: return@handler

        if (IgnoreOpened.SCREEN !in ignores && mc.currentScreen != null) {
            return@handler
        }

        if (IgnoreOpened.CONTAINER !in ignores && (InventoryManager.isInventoryOpen ||
                mc.currentScreen is HandledScreen<*>)) {
            return@handler
        }

        renderEnvironmentForWorld(matrixStack) {
            targetRenderer.render(this, target, partialTicks)
        }

        val currentRotation = playerRotation ?: return@handler

        val timerSpeed = Timer.timerSpeed
        targetRotation?.let { rotation ->
            val interpolatedRotation = Rotation(
                currentRotation.yaw + (rotation.yaw - currentRotation.yaw) * (timerSpeed * partialTicks),
                currentRotation.pitch + (rotation.pitch - currentRotation.pitch) * (timerSpeed * partialTicks)
            )

            player.setRotation(interpolatedRotation)
        }
    }


    @Suppress("unused", "MagicNumber")
    private val mouseMovement = handler<MouseRotationEvent> { event ->
        // Track mouse movement delta
        mouseDeltaX = event.cursorDeltaX.toFloat()
        mouseDeltaY = event.cursorDeltaY.toFloat()

        val f = event.cursorDeltaY.toFloat() * 0.15f
        val g = event.cursorDeltaX.toFloat() * 0.15f

        playerRotation?.let { rotation ->
            rotation.pitch += f
            rotation.yaw += g
            rotation.pitch = MathHelper.clamp(rotation.pitch, -90.0f, 90.0f)
        }

        targetRotation?.let { rotation ->
            rotation.pitch += f
            rotation.yaw += g
            rotation.pitch = MathHelper.clamp(rotation.pitch, -90.0f, 90.0f)
        }
    }

    private fun findNextTargetRotation(): Pair<Entity, RotationWithVector>? {
        for (target in targetTracker.targets()) {
            val pointOnHitbox = pointTracker.findPoint(target, 0)
            val rotationPreference = LeastDifferencePreference(player.rotation, pointOnHitbox.pos)

            val spot = raytraceBox(
                pointOnHitbox.eyes,
                pointOnHitbox.box,
                range = targetTracker.maxRange.toDouble(),
                wallsRange = 0.0,
                rotationPreference = rotationPreference
            ) ?: continue

            targetTracker.target = target
            return target to spot
        }

        targetTracker.reset()
        return null
    }

    private enum class IgnoreOpened(
        override val choiceName: String
    ) : NamedChoice {
        SCREEN("Screen"),
        CONTAINER("Container")
    }
}
