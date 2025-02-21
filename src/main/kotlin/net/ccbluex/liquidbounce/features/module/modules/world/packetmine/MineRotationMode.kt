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
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine

import net.ccbluex.liquidbounce.config.types.NamedChoice

/**
 * Determines how rotating is handled.
 *
 * Also provides procedures for when we can't look at the target position.
 */
@Suppress("unused")
enum class MineRotationMode(override val choiceName: String) : NamedChoice {

    ON_START("OnStart") {

        override fun shouldRotate(mineTarget: MineTarget) = !mineTarget.started

        override fun getFailProcedure(mineTarget: MineTarget): FailProcedure {
            if (shouldRotate(mineTarget)) {
                return FailProcedure.PAUSE
            }

            return FailProcedure.CONTINUE
        }

    },
    ON_STOP("OnStop") {

        override fun shouldRotate(mineTarget: MineTarget) = mineTarget.progress >= ModulePacketMine.breakDamage

        override fun getFailProcedure(mineTarget: MineTarget): FailProcedure {
            if (shouldRotate(mineTarget)) {
                return FailProcedure.PAUSE
            }

            return FailProcedure.CONTINUE
        }

    },
    BOTH("Both") {

        override fun shouldRotate(mineTarget: MineTarget) =
            ON_START.shouldRotate(mineTarget) || ON_STOP.shouldRotate(mineTarget)

        override fun getFailProcedure(mineTarget: MineTarget): FailProcedure {
            if (shouldRotate(mineTarget)) {
                return FailProcedure.PAUSE
            }

            return FailProcedure.CONTINUE
        }

    },
    ALWAYS("Always") {

        override fun shouldRotate(mineTarget: MineTarget) = true

        override fun getFailProcedure(mineTarget: MineTarget): FailProcedure {
            if (!mineTarget.started) {
                return FailProcedure.PAUSE
            }

            return FailProcedure.ABORT
        }

    },
    NEVER("Never") {

        override fun shouldRotate(mineTarget: MineTarget) = false

    };

    abstract fun shouldRotate(mineTarget: MineTarget): Boolean

    open fun getFailProcedure(mineTarget: MineTarget) = FailProcedure.CONTINUE

}

/**
 * Determines what PacketMine should do if we can't look at the target.
 */
enum class FailProcedure {

    ABORT {

        override fun execute(mineTarget: MineTarget): Boolean {
            ModulePacketMine.mode.activeChoice.onCannotLookAtTarget(mineTarget)
            mineTarget.abort()
            return true
        }

    },
    PAUSE {

        override fun execute(mineTarget: MineTarget): Boolean {
            with (ModulePacketMine) {
                mode.activeChoice.onCannotLookAtTarget(mineTarget)

                // if required, we already switch
                val switchMode = switchMode.activeChoice
                switch(switchMode.getSlot(mineTarget.blockState), mineTarget)
                if (switchMode.getSwitchingMethod().shouldSync) {
                    interaction.syncSelectedSlot()
                }
            }

            return true
        }

    },
    CONTINUE {

        override fun execute(mineTarget: MineTarget) = false

    };

    /**
     * `true` when the actual logic should not be executed.
     */
    abstract fun execute(mineTarget: MineTarget): Boolean

}
