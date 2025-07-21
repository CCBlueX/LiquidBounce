/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import it.unimi.dsi.fastutil.ints.Int2LongLinkedOpenHashMap
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.client.network.AbstractClientPlayerEntity
import kotlin.math.pow

/**
 * TargetLock module
 *
 * Locks on to a target and prevents targeting other entities,
 * either [Temporary]ly on attack or by [Filter]ing by username.
 */
object ModuleTargetLock : ClientModule("TargetLock", Category.MISC) {

    init {
        doNotIncludeAlways()
    }

    private val mode = choices("Mode", Temporary, arrayOf(Temporary, Filter))

    /**
     * This option will only lock the enemy on combat modules
     */
    private val combatOnly by boolean("Combat", false)

    private sealed class LockChoice(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = mode
        abstract fun isLockedOn(playerEntity: AbstractClientPlayerEntity): Boolean
    }

    private object Filter : LockChoice("Filter") {

        private val usernames by textList("Usernames", mutableListOf("Notch"))
        private val filterType by enumChoice("FilterType", FilterType.WHITELIST)

        enum class FilterType(override val choiceName: String) : NamedChoice {
            WHITELIST("Whitelist"),
            BLACKLIST("Blacklist")
        }

        override fun isLockedOn(playerEntity: AbstractClientPlayerEntity): Boolean {
            val name = playerEntity.gameProfile.name

            return when (filterType) {
                FilterType.WHITELIST -> usernames.any { it.equals(name, true) }
                FilterType.BLACKLIST -> usernames.none {
                    it.equals(name, true)
                }
            }

        }
    }

    private object Temporary : LockChoice("Temporary") {

        private val timeUntilReset by int("MaximumTime", 30, 0..120, "s")
        private val outOfRange by float("MaximumRange", 20f, 8f..40f)

        private val whenNoLock by enumChoice("WhenNoLock", NoLockMode.ALLOW_ALL)

        // Combination of [entityId] and [time]
        private val lockList = Int2LongLinkedOpenHashMap()

        enum class NoLockMode(override val choiceName: String) : NamedChoice {
            ALLOW_ALL("AllowAll"),
            ALLOW_NONE("AllowNone")
        }

        @Suppress("unused")
        private val attackHandler = handler<AttackEntityEvent> { event ->
            val target = event.entity as? AbstractClientPlayerEntity ?: return@handler

            if (!lockList.containsKey(target.id)) {
                notification(
                    "TargetLock",
                    message("lockedOn", target.gameProfile.name, timeUntilReset),
                    NotificationEvent.Severity.INFO
                )
            }
            lockList.put(target.id, System.currentTimeMillis() + timeUntilReset * 1000L)
        }

        @Suppress("unused")
        private val cleanUpTask = tickHandler {
            if (player.isDead) {
                lockList.clear()
                return@tickHandler
            }

            val currentTime = System.currentTimeMillis()
            lockList.int2LongEntrySet().removeIf { (entityId, time) ->
                // Remove if entity is out of range
                val entity = world.getEntityById(entityId) as? AbstractClientPlayerEntity ?: return@removeIf true

                if (entity.isRemoved || entity.squaredDistanceTo(player) > outOfRange.pow(2)) {
                    notification(
                        "TargetLock",
                        message("outOfRange", entity.gameProfile.name),
                        NotificationEvent.Severity.INFO
                    )
                    return@removeIf true
                }

                // Remove if time is up
                if (time < currentTime) {
                    notification(
                        "TargetLock",
                        message("timeUp", entity.gameProfile.name),
                        NotificationEvent.Severity.INFO
                    )
                    return@removeIf true
                }

                false
            }
        }

        override fun isLockedOn(playerEntity: AbstractClientPlayerEntity): Boolean {
            val entityId = playerEntity.id

            if (lockList.isEmpty()) {
                return when (whenNoLock) {
                    NoLockMode.ALLOW_ALL -> true
                    NoLockMode.ALLOW_NONE -> false
                }
            }

            return lockList.containsKey(entityId)
        }

    }

    @Suppress("unused")
    private val tagEntityEvent = handler<TagEntityEvent> { event ->
        if (event.entity !is AbstractClientPlayerEntity || this@ModuleTargetLock.isLockedOn(event.entity)) {
            return@handler
        }

        if (combatOnly) {
            event.dontTarget()
        } else {
           event.ignore()
        }
    }

    /**
     * Check if [entity] is in your focus
     */
    private fun isLockedOn(entity: AbstractClientPlayerEntity): Boolean {
        if (!running) {
            return false
        }

        return mode.activeChoice.isLockedOn(entity)
    }

}
