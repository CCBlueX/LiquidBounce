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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.model.InputSchema
import net.ccbluex.liquidbounce.deeplearn.model.LoadedModel
import net.ccbluex.liquidbounce.deeplearn.model.ModelFile
import net.ccbluex.liquidbounce.deeplearn.model.ModelRegistry
import net.ccbluex.liquidbounce.deeplearn.model.ModelSlot
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import java.util.WeakHashMap

/** The combat part of a model's metadata: the largest turn per tick and whether its aim passed validation. */
@UnstableAddonApi
class CombatModelInfo(val turnCap: Float, val aim: Boolean) {
    companion object {
        fun of(metadata: JsonObject): CombatModelInfo {
            val turnCap = metadata["turnCap"]?.let(ModelFile::float)
            val aim = turnCap != null && metadata["heads"]?.takeIf { it.isJsonArray }?.asJsonArray
                ?.any { it.asString == "aim" } == true
            return CombatModelInfo(turnCap ?: 0f, aim)
        }
    }
}

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

    private val info = WeakHashMap<ModelFile, CombatModelInfo>()

    fun choose(model: BundledCombatModel) = ModelRegistry.choose(SLOT, model.id)

    fun available() = ModelRegistry.active(SLOT)?.takeIf { !ModelRegistry.failed(SLOT) }?.let { info(it).aim } == true

    fun info(file: ModelFile) = synchronized(info) { info.getOrPut(file) { CombatModelInfo.of(file.metadata) } }

    fun <T> withActive(block: (LoadedModel, CombatModelInfo) -> T): T? =
        ModelRegistry.use(SLOT) { model -> block(model, info(model.file)) }

    fun describe(): String {
        if (!DeepLearningEngine.isInitialized) {
            return "Engine unavailable"
        }
        val file = ModelRegistry.active(SLOT) ?: return "No model"
        val name = if (ModelRegistry.installed(SLOT) === file) file.name else ModelRegistry.chosen(SLOT)
        return when {
            ModelRegistry.failed(SLOT) -> "$name: failed, see the log"
            !info(file).aim -> "$name: aim not validated"
            else -> "$name: ready"
        }
    }
}
