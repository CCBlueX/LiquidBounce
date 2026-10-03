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
package net.ccbluex.liquidbounce.deeplearn.combat

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.deeplearn.model.InputSchema
import net.ccbluex.liquidbounce.deeplearn.model.ModelFile
import net.ccbluex.liquidbounce.deeplearn.model.ModelRegistry
import net.ccbluex.liquidbounce.deeplearn.model.ModelSlot
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import kotlin.random.Random

/** The rotation models bundled with the client, each trained on cooldown and 1.8 combat alike. */
@UnstableAddonApi
enum class BundledCombatModel(override val tag: String, val id: String) : Tagged {
    DEFAULT("Default", "default"),
    JUGGLE("Juggle", "juggle"),
    EXPERT("Expert", "expert"),
    DUELS("Duels", "duels"),
}

/** The combat task's model slot: rotations, whatever the server's combat style. */
@UnstableAddonApi
object CombatModels {
    const val TASK = "combat"
    val INPUT = InputSchema(TASK, CombatFeatures.VERSION, CombatFeatures.SIZE)
    val SLOT = ModelSlot(TASK, "rotation", INPUT, CombatOutputs.SIZE, BundledCombatModel.entries.map { it.id })

    /** The largest turn per tick the model makes. */
    fun turnCap(file: ModelFile) =
        ModelFile.float(checkNotNull(file.metadata["turnCap"]) { "${file.name} has no turnCap" })

    /** The installed model's decision, else [model]'s, or null without a working one. */
    fun decide(model: BundledCombatModel, input: FloatArray, randomness: Float): CombatDecision? =
        ModelRegistry.use(SLOT, model.id) { loaded ->
            val output = loaded.predict(input)
            CombatOutputs.decide(output.values, output.clamped, randomness, turnCap(loaded.file), Random.Default)
        }
}
