package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoClicker
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.wouldDoCriticalHit
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.player.PlayerEntity

object CriticalsTimer : Choice("Timer") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleCriticals.modes

    private val speed by float("Speed", 0.8f, 0.1f..1.0f)
    private val range by float("Range", 4.0f, 0.0f..10.0f)
    private val fallOnly by boolean("Fall only", true)

    // 新增配置项
    private val optimizeForCooldown by boolean("OptimizeForCooldown", true)
    private val checkKillAura by boolean("CheckKillaura", false)
    private val checkAutoClicker by boolean("CheckAutoClicker", false)
    private val canBeSeen by boolean("CanBeSeen", true)

    private fun isActive(): Boolean {
        if (!ModuleCriticals.running) {
            return false
        }

        if (!checkKillAura && !checkAutoClicker) {
            return true
        }

        return (ModuleKillAura.running && checkKillAura) ||
            (ModuleAutoClicker.running && checkAutoClicker)
    }

    private fun calculateTicksUntilNextCrit(): Float {
        val durationToWait = player.attackCooldownProgressPerTick * 0.9F - 0.5F
        val waitedDuration = player.lastAttackedTicks.toFloat()

        return (durationToWait - waitedDuration).coerceAtLeast(0.0f)
    }

    private fun getCooldownDamageFactor(player: PlayerEntity, tickDelta: Float): Float {
        val base = ((tickDelta + 0.5f) / player.attackCooldownProgressPerTick)

        return (0.2f + base * base * 0.8f).coerceAtMost(1.0f)
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val world = mc.world ?: return@handler
        val player = mc.player ?: return@handler

        val enemy = world.findEnemy(0.0f..range) ?: return@handler

        if (!isActive()) {
            return@handler
        }

        if (optimizeForCooldown && calculateTicksUntilNextCrit() > 0.0f) {
            return@handler
        }

        if (canBeSeen && !player.canSee(enemy)) {
            return@handler
        }

        if (wouldDoCriticalHit(ignoreSprint = true)) {
            if (!fallOnly || player.velocity.y < 0.0) {
                Timer.requestTimerSpeed(speed, Priority.IMPORTANT_FOR_USAGE_1, ModuleCriticals)
            }
        }
    }
}
