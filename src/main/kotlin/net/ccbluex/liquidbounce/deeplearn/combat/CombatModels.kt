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
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.model.InputSchema
import net.ccbluex.liquidbounce.deeplearn.model.LoadedModel
import net.ccbluex.liquidbounce.deeplearn.model.ModelFile
import net.ccbluex.liquidbounce.deeplearn.model.ModelRegistry
import net.ccbluex.liquidbounce.deeplearn.model.ModelSlot
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import java.util.WeakHashMap

@UnstableAddonApi
class CombatHeads(val aim: Boolean, val attacks: Boolean, val movement: Boolean) {
    val any get() = aim || attacks || movement
    val description get() = names.joinToString(", ")
    val names get() = listOfNotNull("aim".takeIf { aim }, "attacks".takeIf { attacks }, "movement".takeIf { movement })

    fun covers(other: CombatHeads) =
        (aim || !other.aim) && (attacks || !other.attacks) && (movement || !other.movement)

    companion object {
        fun of(names: Collection<String>) = CombatHeads("aim" in names, "attacks" in names, "movement" in names)
    }
}

/** The combat part of a model's metadata: the largest turn per tick and the heads that passed validation. */
@UnstableAddonApi
class CombatModelInfo(val turnCap: Float, val heads: CombatHeads) {
    companion object {
        fun of(metadata: JsonObject): CombatModelInfo {
            val turnCap = metadata["turnCap"]?.let(ModelFile::float)
            val heads = metadata["heads"]?.takeIf { turnCap != null && it.isJsonArray }?.asJsonArray
                ?.map { it.asString }.orEmpty()
            return CombatModelInfo(turnCap ?: 0f, CombatHeads.of(heads))
        }
    }
}

/** The combat task's model slots, one per [CombatStyle]. */
@UnstableAddonApi
object CombatModels {
    const val TASK = "combat"
    val INPUT = InputSchema(TASK, CombatFeatures.VERSION, CombatFeatures.SIZE)

    private val slots = CombatStyle.entries.associateWith { ModelSlot(TASK, it.id, INPUT, CombatOutputs.SIZE) }
    private val info = WeakHashMap<ModelFile, CombatModelInfo>()

    fun slot(style: CombatStyle) = slots.getValue(style)

    fun available(style: CombatStyle) = ModelRegistry.active(slot(style)) != null && !ModelRegistry.failed(slot(style))

    fun info(file: ModelFile) = synchronized(info) { info.getOrPut(file) { CombatModelInfo.of(file.metadata) } }

    fun <T> withActive(style: CombatStyle, block: (LoadedModel, CombatModelInfo) -> T): T? =
        ModelRegistry.use(slot(style)) { model -> block(model, info(model.file)) }

    fun describe(style: CombatStyle): String {
        if (!DeepLearningEngine.isInitialized) {
            return "Engine unavailable"
        }
        val slot = slot(style)
        val file = ModelRegistry.active(slot) ?: return "No model for ${style.id} combat"
        val name = if (ModelRegistry.installed(slot) === file) file.name else "Default"
        if (ModelRegistry.failed(slot)) {
            return "$name: failed, see the log"
        }
        return "$name: ${info(file).heads.description.ifEmpty { "nothing validated" }}"
    }
}
