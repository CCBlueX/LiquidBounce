package net.ccbluex.liquidbounce.features.module.modules.movement.speed

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes.CriticalsJump
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed.OnlyInCombat.modes
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed.OnlyOnPotionEffect.modes
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed.OnlyOnPotionEffect.potionEffects
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedCustom
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedLegitHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedSpeedYPort
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.server.blocksmc.SpeedBlocksMC
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.grim.SpeedGrimCollide
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.server.hylex.SpeedHylexGround
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.server.hylex.SpeedHylexLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.intave.SpeedIntave
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.martix.SpeedMatrix
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.ncp.SpeedNCP
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.sentinel.SpeedSentinelDamage
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.spartan.SpeedSpartanV4043
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.verus.SpeedVerus
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.vulcan.SpeedVulcan
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.watchdog.SpeedHypxiel
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.server.loyisa.SpeedLoyisa
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import java.util.function.BooleanSupplier

/**
 * Speed module
 *
 * Allows you to move faster.
 */
object ModuleSpeed : ClientModule("Speed", Category.MOVEMENT) {

    init {
        enableLock()
    }

    /**
     * Initialize speeds choices independently
     *
     * This is useful for the `OnlyOnPotionEffect` choice, which has its own set of modes
     */
    private fun initializeSpeeds(configurable: ChoiceConfigurable<*>) = arrayOf(
        SpeedLegitHop(configurable),
        SpeedCustom(configurable),
        SpeedSpeedYPort(configurable),

        SpeedBlocksMC(configurable),

        SpeedGrimCollide(configurable),

        SpeedHylexGround(configurable),
        SpeedHylexLowHop(configurable),

        SpeedHypxiel(configurable),

        SpeedIntave(configurable),
        SpeedLoyisa(configurable),
        SpeedMatrix(configurable),


        SpeedNCP(configurable),

        SpeedSentinelDamage(configurable),
        SpeedSpartanV4043(configurable),

        SpeedVerus(configurable),
        SpeedVulcan(configurable)
    )

    val modes = choices("Mode", 0, this::initializeSpeeds).apply(::tagBy)

    private val notCondition by multiEnumChoice("Not", NotCondition.DURING_SCAFFOLD)

    private val avoidEdgeBump by boolean("AvoidEdgeBump", true)

    init {
        tree(OnlyInCombat)
        tree(OnlyOnPotionEffect)
        tree(SpeedYawOffset)
    }

    override val running: Boolean
        get() {
            // Early return if the module is not ready to be used -
            // prevents accessing player when it's null below
            // in case it was forgotten to be checked
            return when {
                !(super.running || ModuleScaffold.running && ModuleScaffold.autoSpeed) -> false
                !passesRequirements() -> false
                OnlyInCombat.enabled && CombatManager.isInCombat -> false
                OnlyOnPotionEffect.enabled && potionEffects.activeChoice.checkPotionEffects() -> false
                else -> true
            }
        }

    private fun passesRequirements() = when {
        // DO NOT REMOVE - PLAYER COULD BE NULL!
        !inGame || isDestructed -> false
        else -> notCondition.all { it.testCondition.asBoolean }
    }

    private object OnlyInCombat : ToggleableConfigurable(this, "OnlyInCombat", false) {

        val modes = choices(this, "Mode", activeIndex = 0, ModuleSpeed::initializeSpeeds)

        /**
         * Controls [modes] activation state.
         */
        override val running: Boolean
            get() = when {
                !inGame || isDestructed -> false
                !ModuleSpeed.enabled || !this.enabled || !passesRequirements() -> false
                else -> CombatManager.isInCombat
            }

    }

    private object OnlyOnPotionEffect : ToggleableConfigurable(this, "OnlyOnPotionEffect", false) {

        val potionEffects = choices(
            this,
            "PotionEffect",
            SpeedPotionEffectChoice,
            arrayOf(SpeedPotionEffectChoice, SlownessPotionEffectChoice, BothEffectsChoice)
        )

        val modes = choices(this, "Mode", activeIndex = 0, ModuleSpeed::initializeSpeeds)

        /**
         * Controls [modes] activation state.
         */
        override val running: Boolean
            get() = when {
                !inGame || isDestructed -> false
                !ModuleSpeed.enabled || !this.enabled || !passesRequirements() -> false
                else -> potionEffects.activeChoice.checkPotionEffects()
            }

    }

    abstract class PotionEffectChoice(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<PotionEffectChoice>
            get() = potionEffects

        abstract fun checkPotionEffects(): Boolean
    }

    @Suppress("ReturnCount", "MagicNumber")
    internal fun doOptimizationsPreventJump(): Boolean {
        if (CriticalsJump.running && CriticalsJump.shouldWaitForJump(0.42f)) {
            return true
        }

        if (avoidEdgeBump && SpeedAntiCornerBump.shouldDelayJump()) {
            return true
        }

        return false
    }

    @Suppress("unused")
    private enum class NotCondition(
        override val choiceName: String,
        val testCondition: BooleanSupplier
    ) : NamedChoice {
        WHILE_USING_ITEM("WhileUsingItem", {
            !player.isUsingItem
        }),
        DURING_SCAFFOLD("DuringScaffold", {
            !(ModuleScaffold.running || ModuleFly.running)
        }),
        WHILE_SNEAKING("WhileSneaking", {
            !player.isSneaking
        })
    }
}
