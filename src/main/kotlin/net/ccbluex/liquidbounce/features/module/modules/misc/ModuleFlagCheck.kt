/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.damagesource.CombatRules
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.enchantment.EnchantmentHelper
import org.apache.commons.lang3.StringUtils
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

/**
 * Module Flag Check.
 *
 * Alerts you about set backs.
 */
object ModuleFlagCheck : ClientModule("FlagCheck", ModuleCategories.MISC, aliases = listOf("FlagDetect")) {

    private var chatMessage by boolean("ChatMessage", true)
    private var notification by boolean("Notification", false)
    private var invalidAttributes by boolean("InvalidAttributes", false)

    private object ResetFlags : ToggleableValueGroup(ModuleFlagCheck, "ResetFlags", true) {

        private val afterSeconds by int("After", 30, 1..300, "s")

        @Suppress("unused")
        private val repeatable = tickHandler(Dispatchers.Default) {
            flagCount.getAndSet(0)
            delay(afterSeconds.seconds)
        }

    }

    private object CombatMitigation : ValueGroup("CombatMitigation", ModuleFlagCheck) {
        val tolerance by float("Tolerance", 0.5f, 0.001f..2.0f)
    }

    private object Render : ToggleableValueGroup(ModuleFlagCheck, "Render", true) {

        private val notInFirstPerson by boolean("NotInFirstPerson", true)
        private val renderTime by int("Alive", 1000, 0..3000, "ms")
        private val fadeOut by easing("FadeOut", Easing.QUAD_OUT)
        private val outTime by int("OutTime", 500, 0..2000, "ms")
        private var color by color("Color", Color4b.RED.with(a = 100).darker())
        private var outlineColor by color("OutlineColor", Color4b.RED.darker())

        val wireframePlayer = WireframePlayer()
        var creationTime = 0L
        var finished = true

        override fun onEnabled() {
            finished = true
        }

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> {
            if (finished || notInFirstPerson && mc.options.cameraType.isFirstPerson) {
                return@handler
            }

            val time = System.currentTimeMillis()
            val withinRenderDuration = time - creationTime < renderTime

            if (withinRenderDuration) {
                wireframePlayer.render(it, color, outlineColor)
            } else {
                val factor = 1f - fadeOut.getFactor(creationTime + renderTime, time, outTime.toFloat())
                if (factor == 0f) {
                    finished = true
                    return@handler
                }

                wireframePlayer.render(it, color.fade(factor), outlineColor.fade(factor))
            }
        }

        fun reset() {
            creationTime = System.currentTimeMillis()
            finished = false
        }

    }

    init {
        tree(ResetFlags)
        tree(CombatMitigation)
        tree(Render)
    }

    private val flagCount = atomic(0)
    private var lastYaw = 0F
    private var lastPitch = 0F

    private var combatTarget: LivingEntity? = null
    private var healthBeforeHit: Float = 0f
    private var expectedDamage: Float = 0f
    private var attackTick: Int = 0

    @Suppress("unused")
    private val attackHandler = handler<AttackEvent> { event ->
        val target = event.target as? LivingEntity ?: return@handler

        combatTarget = target
        healthBeforeHit = target.health + target.absorptionAmount
        expectedDamage = calculateExpectedDamage(target)
        attackTick = player.tickCount
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (player.tickCount <= 25) {
            return@handler
        }

        when (val packet = event.packet) {
            is ClientboundPlayerPositionPacket -> {
                val change = packet.change
                val deltaYaw = calculateAngleDelta(change.yRot, lastYaw)
                val deltaPitch = calculateAngleDelta(change.xRot, lastPitch)

                flagCount.incrementAndGet()
                if (deltaYaw >= 90 || deltaPitch >= 90) {
                    alert(AlertReason.FORCEROTATE, "(${deltaYaw.roundToLong()}° | ${deltaPitch.roundToLong()}°)")
                } else {
                    alert(AlertReason.LAGBACK)
                }

                Render.reset()
                val position = change.position
                Render.wireframePlayer.setPosRot(position.x, position.y, position.z, change.yRot, change.xRot)

                lastYaw = player.yHeadRot
                lastPitch = player.xRot
            }

            is ClientboundDisconnectPacket -> {
                flagCount.getAndSet(0)
            }
        }
    }

    @Suppress("unused")
    private val repeatable = handler<GameTickEvent> {
        // 1. Combat Mitigation (Стоит первым, чтобы не блокироваться return@handler)
        val target = combatTarget
        if (target == null || player.tickCount - attackTick > 20) {
            combatTarget = null
        } else if (target.hurtTime > 0) {
            val actualDamage = healthBeforeHit - (target.health + target.absorptionAmount)
            
            // Защита от спама: если у врага 0 брони (сервер скрыл), а урон ожидается большой - пропускаем.
            val isAttributeHidden = target.getAttributeValue(Attributes.ARMOR) == 0.0 && expectedDamage > 5.0f
            
            if (!isAttributeHidden && actualDamage > 0.1f && actualDamage < (expectedDamage - CombatMitigation.tolerance)) {
                flagCount.incrementAndGet()
                alert(AlertReason.MITIGATION, "Exp: ${"%.2f".format(expectedDamage)}, Got: ${"%.2f".format(actualDamage)}")
            }
            combatTarget = null
        }

        // 2. Original InvalidAttributes logic (Восстановлено полностью)
        if (!invalidAttributes) {
            return@handler
        }

        val invalidHeath = player.health <= 0f && player.isAlive
        val invalidHunger = player.foodData.foodLevel <= 0

        if (!invalidHeath && !invalidHunger) {
            return@handler
        }

        val invalidReasons = mutableListOf<String>()

        if (invalidHeath) {
            invalidReasons.add("Health")
        }

        if (invalidHunger) {
            invalidReasons.add("Hunger")
        }

        if (invalidReasons.isNotEmpty()) {
            flagCount.incrementAndGet()

            val reasonString = invalidReasons.joinToString()
            alert(AlertReason.INVALID, reasonString)
        }
    }

    private fun calculateExpectedDamage(target: LivingEntity): Float {
        var damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE).toFloat()
        damage += EnchantmentHelper.getDamageBonus(player.mainHandItem, target.type).toFloat()

        val strength = player.getEffect(MobEffects.DAMAGE_BOOST)
        if (strength != null) {
            damage += 3.0f * (strength.amplifier + 1)
        }

        val isCrit = player.fallDistance > 0.0f && !player.onGround() && !player.onClimbable() && !player.isInWater && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger
        if (isCrit) {
            damage *= 1.5f
        }

        val armor = target.getAttributeValue(Attributes.ARMOR).toFloat()
        val toughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS).toFloat()

        damage = CombatRules.getDamageAfterAbsorb(target, damage, target.damageSources().playerAttack(player), armor, toughness)

        val epf = EnchantmentHelper.getDamageProtection(target.level(), target, target.damageSources().playerAttack(player))
        damage = CombatRules.getDamageAfterMagicAbsorb(damage, epf.toFloat())

        return damage
    }

    private fun alert(reason: AlertReason, extra: String? = null) {
        val message = if (StringUtils.isEmpty(extra)) {
            message("alert", message(reason.key), flagCount)
        } else {
            message("alertWithExtra", message(reason.key), extra!!, flagCount)
        }

        if (notification) {
            notification(name, message, NotificationEvent.Severity.INFO)
        }

        if (chatMessage) {
            chat(message, metadata = MessageMetadata(id = "$name#${reason.key}"))
        }
    }

    private fun calculateAngleDelta(newAngle: Float, oldAngle: Float): Float {
        var delta = newAngle - oldAngle
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        return abs(delta)
    }

    @Suppress("SpellCheckingInspection")
    private enum class AlertReason(val key: String) {
        INVALID("invalid"),
        FORCEROTATE("forceRotate"),
        LAGBACK("lagback"),
        MITIGATION("mitigation")
    }

}
