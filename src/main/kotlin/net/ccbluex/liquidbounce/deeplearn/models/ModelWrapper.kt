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
package net.ccbluex.liquidbounce.deeplearn.models

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.model.LoadedModel
import net.ccbluex.liquidbounce.deeplearn.model.ModelFile
import java.io.Closeable
import java.util.Locale

/**
 * A model file bundled with the client, predicting through [LoadedModel].
 */
abstract class ModelWrapper(
    name: String,
    override val parent: ModeValueGroup<*>
) : Mode(name), Closeable {

    private var file: ModelFile? = null
    private var loaded: LoadedModel? = null
    private val lock = Any()

    @Volatile
    private var closed = false

    fun predict(input: FloatArray): FloatArray {
        require(DeepLearningEngine.isInitialized) { "DeepLearningEngine is not initialized" }

        return synchronized(lock) {
            check(!closed) { "Model '$name' is closed" }
            val model = loaded ?: LoadedModel(checkNotNull(file) { "Model '$name' is not loaded" })
                .also { loaded = it }
            model.predict(input).values
        }
    }

    fun load() {
        val resource = "/resources/liquidbounce/models/${name.lowercase(Locale.ENGLISH)}.${ModelFile.EXTENSION}"
        val file = javaClass.getResourceAsStream(resource)!!.use(ModelFile::read)

        synchronized(lock) {
            check(!closed) { "Model '$name' is closed" }
            this.file = file
            loaded?.close()
            loaded = null
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true

            loaded?.close()
        }
    }

}
