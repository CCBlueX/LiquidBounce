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
package net.ccbluex.liquidbounce.api.models.cosmetics

/**
 * @param category `null` for categories newer than this client
 * @param style the texture's name, `null` for a cosmetic without one
 * @param enabled only told about my own cosmetics; a disabled one is shown to nobody
 */
data class Cosmetic(val category: CosmeticCategory?, val style: String? = null, val enabled: Boolean? = null)

class Carrier(val cosmetics: Set<Cosmetic>)

/**
 * @param hashes MD5 of each carrier's UUID
 */
class CarrierHashes(val hashes: Set<String>)
