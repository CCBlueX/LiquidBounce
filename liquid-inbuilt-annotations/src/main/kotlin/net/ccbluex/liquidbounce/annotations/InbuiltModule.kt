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

package net.ccbluex.liquidbounce.annotations

/**
 * Marks a ClientModule `object` which should be included in `ModuleManager`.
 *
 * Visibility should be `public`.
 *
 * It should be like:
 * ```kotlin
 * @InbuiltModule
 * object ModuleExample : ClientModule("Example", ...) { ... }
 * ```
 * All modules should have unique name.
 *
 * Run Gradle task `kspKotlin` to generate.
 * It will be auto-executed when you run `compileKotlin` task.
 * Generated files could be found at `<rootProject>/build/generated/ksp/main/kotlin/`.
 *
 * All `ClientModule` objects will be collected as an array.
 * You can get the array by `ModuleManager.allClientModules`. Don't modify it.
 * This extension will be defined in the same package of `ModuleManager`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class InbuiltModule
