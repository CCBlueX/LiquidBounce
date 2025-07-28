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
 * Marks a class of Event implementation which should be included in EventManager.
 *
 * Visibility should be `public`.
 *
 * It should be like:
 * ```kotlin
 * @InbuiltEvent("example")
 * class/object ExampleEvent : Event/CancellableEvent
 * ```
 * An Event can be singleton, which means it is stateless and immutable.
 *
 * Run Gradle task `kspKotlin` to generate.
 * It will be auto-executed when you run `compileKotlin` task.
 * Generated files could be found at `<rootProject>/build/generated/ksp/main/kotlin/`.
 *
 * All `Event` objects will be collected as an array.
 * You can get the array by `EventManager.allEventClasses`. Don't modify it.
 * This extension will be defined in the same package of `EventManager`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class InbuiltEvent(
    /**
     * In lower camel case. Used for:
     * - Script
     * - Web-based event flow
     *
     * This property should be unique in project-wide.
     * Generally it can be represented by the class name (Upper-Camel) as:
     * ```kotlin
     * className.removeSuffix("Event").replaceFirstChar { it.lowercaseChar() }
     * ```
     *
     * Example:
     * ```kotlin
     * @InbuiltEvent("myNew")
     * class MyNewEvent
     * ```
     */
    val name: String
)
