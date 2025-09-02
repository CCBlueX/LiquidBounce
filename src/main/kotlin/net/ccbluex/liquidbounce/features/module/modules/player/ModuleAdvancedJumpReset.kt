package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author _0x16z
 */

@Suppress("TooManyFunctions")
object ModuleAdvancedJumpReset: ClientModule("AdvancedJumpReset", Category.PLAYER) {

    private object TriggerSettings : ToggleableConfigurable(ModuleVelocity, "TriggerSettings", true) {
        val mode by enumChoice("Mode", TriggerMode.Tick)
        val tick by int("JumpTick", 8, 1..20)
        val chance by float("Chance", 60f, 0f..100f, "%")
    }

    private object JumpSettings : ToggleableConfigurable(ModuleVelocity, "JumpSettings", true) {
        val mode by enumChoice("Mode", JumpMode.Packet)
        val motionHeight by float("MotionHeight", 0.42f, 0f..2f)
        val allowJumpInAir by boolean("AllowJumpInAir", false)
        val jumpInInv by boolean("JumpInInv", false)
        val hurtTime by intRange("HurtMin", 5..8, 1..10)
    }

    private object ReduceSettings : ToggleableConfigurable(ModuleVelocity, "ReduceSettings", true) {
        val enable by boolean("Enabled", false)
        val mode by enumChoice("Mode", ReduceMode.Smooth)
        val event by enumChoice("Event", ReduceEvent.HurtTime)
        val hurtTime by intRange("HurtTime", 8..8, 1..10)

        val baseFactor by float("BaseFactor", 0.6f, 0f..1f)
        val sprintFactor by float("SprintFactor", 0.6f, 0f..1f)
        val hitFactor by float("HitFactor", 0.6f, 0f..1f)
        val hitSprintFactor by float("HitSprintFactor", 0.6f, 0f..1f)
    }

    private object CheckSettings : ToggleableConfigurable(ModuleVelocity, "CheckSettings", true) {
        val notSPressed by boolean("NotWhileSPressed", false)
        val notBlocking by boolean("NotWhileBlocking", false)
        val sneaking by boolean("CheckSneak", false)
        val ignoreFire by boolean("IgnoreFire", false)
        val notSpeed by boolean("NotWhileSpeed", false)
        val notJumpBoost by boolean("NotWhileJumpBoost", false)
    }


    private object BlockCheckSettings : ToggleableConfigurable(ModuleVelocity, "BlockChecks", true) {
        val liquid by boolean("Liquid", false)
        val cobweb by boolean("Cobweb", false)
        val ladder by boolean("Ladder", false)
    }

    private object PauseSettings : ToggleableConfigurable(ModuleVelocity, "PauseConditions", true) {
        val notCombating by boolean("PauseNotCombating", false)
        val combatTick by int("CombatTick", 8, 1..100)
    }


    init {
        tree(TriggerSettings)
        tree(JumpSettings)
        tree(ReduceSettings)
        tree(CheckSettings)
        tree(BlockCheckSettings)
        tree(PauseSettings)
    }

    private val moduleChecks by multiEnumChoice("ModuleChecks", ModuleCheck.WHILE_SPEED)

    private var valid = false
    private var jumpTicks = 0
    private var flagTicks = 0
    private var attackTicks = 0
    private var lastHT = 0
    private var wasJumped = false
    private var allowJump = false

    private enum class TriggerMode(override val choiceName: String) : NamedChoice {
        Tick("Tick"),
        Chance("Chance"),
        Matrix("Matrix"),
        Intave14("Intave14");
    }

    private enum class JumpMode(override val choiceName: String) : NamedChoice {
        Legit("Legit"),
        Motion("Motion"),
        Packet("Packet"),
        WTF("WTF"),
        Modify("Modify");
    }

    private enum class ReduceMode(override val choiceName: String) : NamedChoice {
        Standard("Standard"),
        Smooth("Smooth"),
        Accelerate("Accelerate");
    }

    private enum class ReduceEvent(override val choiceName: String) : NamedChoice {
        HurtTime("HurtTime"),
        Update("Update"),
        Jump("Jump");
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (player.hurtTime != lastHT) {
            lastHT = player.hurtTime
            tryJump()

            if (ReduceSettings.enable && ReduceSettings.event == ReduceEvent.HurtTime &&
                (player.hurtTime in ReduceSettings.hurtTime) && wasJumped
            ) {
                reduceMotion()
            }
        }

        if (wasJumped && ReduceSettings.enable && ReduceSettings.event == ReduceEvent.Update &&
            (player.hurtTime in ReduceSettings.hurtTime)
        ) {
            reduceMotion()
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (event.packet) {
            is EntityVelocityUpdateS2CPacket -> {
                if (event.packet.entityId == player.id) {
                    allowJump = Math.random() <= TriggerSettings.chance / 100
                    valid = event.packet.velocityX != 0 || event.packet.velocityZ != 0
                    wasJumped = false
                }
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        attackTicks++
        flagTicks++
        jumpTicks++
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> {
        attackTicks = 0
    }

    private fun checkModuleEnabled(): Boolean {
        return moduleChecks.any { !it.testCondition() }
    }

    private fun tryJump() {
        if (!shouldAttemptJump()) return

        when (JumpSettings.mode) {
            JumpMode.Legit -> performLegitJump()
            JumpMode.Motion -> performMotionJump()
            JumpMode.Modify -> performModifiedJump()
            JumpMode.WTF -> performWTFJump()
            else -> {}
        }

        if (ReduceSettings.enable && ReduceSettings.event == ReduceEvent.Jump) {
            reduceMotion()
        }
        wasJumped = true
    }

    private fun shouldAttemptJump(): Boolean {
        if (checkModuleEnabled()) return false

        if (!checkJumpableState()) return false
        if (!checkKeyPress()) return false
        if (!checkHurtTimeRange()) return false
        if (!checkJumpConditions()) return false
        if (!checkPauseConditions()) return false
        if (!checkEffects()) return false
        if (!checkUI()) return false
        if (!checkBlocking()) return false
        if (!checkFire()) return false
        if (!checkSneaking()) return false
        if (!isValidToJump()) return false

        return true
    }

    private fun checkJumpableState(): Boolean {
        return player.isOnGround || JumpSettings.allowJumpInAir
    }

    private fun checkKeyPress(): Boolean {
        return !mc.options.backKey.isPressed || !CheckSettings.notSPressed
    }

    private fun checkHurtTimeRange(): Boolean {
        return player.hurtTime in JumpSettings.hurtTime
    }

    private fun checkJumpConditions(): Boolean {
        return canJump() && !wasJumped
    }

    private fun checkPauseConditions(): Boolean {
        return (jumpTicks >= TriggerSettings.tick || TriggerSettings.mode != TriggerMode.Tick) &&
            (attackTicks >= PauseSettings.combatTick || !PauseSettings.notCombating) &&
            valid
    }

    private fun checkEffects(): Boolean {
        val hasSpeed = player.hasStatusEffect(StatusEffects.SPEED)
        val hasJB = player.hasStatusEffect(StatusEffects.JUMP_BOOST)

        return (!CheckSettings.notJumpBoost || !hasJB) &&
            (!CheckSettings.notSpeed || !hasSpeed)
    }

    private fun checkUI(): Boolean {
        return JumpSettings.jumpInInv || mc.currentScreen == null
    }

    private fun checkBlocking(): Boolean {
        return !CheckSettings.notBlocking || !player.isUsingItem
    }

    private fun checkFire(): Boolean {
        return !CheckSettings.ignoreFire || !player.isOnFire
    }

    private fun checkSneaking(): Boolean {
        return !CheckSettings.sneaking || !player.isSneaking
    }

    private fun performLegitJump() {
        if (!JumpSettings.allowJumpInAir) {
            mc.options.jumpKey.isPressed = true
        } else {
            player.jump()
        }
    }

    private fun performMotionJump() {
        player.velocity.y = JumpSettings.motionHeight.toDouble()
    }

    private fun performModifiedJump() {
        player.jump()
        player.velocity.y = JumpSettings.motionHeight.toDouble()
    }

    private fun performWTFJump() {
        network.sendPacket(
            PlayerMoveC2SPacket.PositionAndOnGround(
                player.x,
                player.y + 0.42,
                player.z,
                false,
                false,
            )
        )
    }


    private fun isValidToJump(): Boolean {
        if (BlockCheckSettings.ladder && player.isClimbing) return false
        if (BlockCheckSettings.liquid && (player.isTouchingWater || player.isInLava)) return false
        return !BlockCheckSettings.cobweb || !player.isInsideWall
    }

    private fun canJump(): Boolean {
        return when (TriggerSettings.mode) {
            TriggerMode.Matrix -> player.age % 4 == 3
            TriggerMode.Intave14 -> player.age % 2 == 0
            TriggerMode.Chance -> allowJump
            TriggerMode.Tick -> jumpTicks >= TriggerSettings.tick
        }
    }

    private fun reduceMotion() {
        val factor = when {
            player.isSprinting && attackTicks <= 1 -> ReduceSettings.hitSprintFactor
            player.isSprinting -> ReduceSettings.sprintFactor
            attackTicks <= 1 -> ReduceSettings.hitFactor
            else -> ReduceSettings.baseFactor
        }

        when (ReduceSettings.mode) {
            ReduceMode.Standard -> {
                player.velocity = player.velocity.multiply(factor.toDouble(), 1.0, factor.toDouble())
            }

            ReduceMode.Smooth -> {
                player.velocity = player.velocity.multiply(1.0 - factor.toDouble(), 1.0, 1.0 - factor.toDouble())
            }

            ReduceMode.Accelerate -> {
                val yaw = Math.toRadians(player.yaw.toDouble())
                player.velocity = player.velocity.add(
                    -sin(yaw) * factor.toDouble(),
                    0.0,
                    cos(yaw) * factor.toDouble()
                )
            }
        }
    }

    @Suppress("unused")
    private enum class ModuleCheck(
        override val choiceName: String,
        val testCondition: () -> Boolean
    ) : NamedChoice {
        WHILE_KILL_AURA("WhileKillAura", { !ModuleKillAura.running }),
        WHILE_SCAFFOLD("WhileScaffold", { !ModuleScaffold.running }),
        WHILE_LONG_JUMP("WhileLongJump", { !ModuleLongJump.running }),
        WHILE_FLY("WhileFly", { !ModuleFly.running }),
        WHILE_SPEED("WhileSpeed", { !ModuleSpeed.running }),
        WHILE_NO_SLOW("WhileNoSlow", { !ModuleNoSlow.running })
    }
}
