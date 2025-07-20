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
package net.ccbluex.liquidbounce.utils.kotlin

import org.apache.commons.codec.digest.DigestUtils
import java.nio.ByteBuffer
import java.util.UUID

/**
 * Convert UUID to 16 bytes array
 */
fun UUID.toByteArray(): ByteArray {
    val bytes = ByteArray(16)
    val buffer = ByteBuffer.wrap(bytes)
    buffer.putLong(mostSignificantBits)
    buffer.putLong(leastSignificantBits)
    return bytes
}

/**
 * Convert UUID to 16 bytes array and then to MD5 hash
 *
 * Compatible with Rust equivalent of hex::encode(*md5::compute(id.as_bytes()))
 */
fun UUID.toMD5(): String = DigestUtils.md5Hex(toByteArray())
