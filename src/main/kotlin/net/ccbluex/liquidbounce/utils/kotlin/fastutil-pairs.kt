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
@file:Suppress("NOTHING_TO_INLINE", "detekt:TooManyFunctions")

package net.ccbluex.liquidbounce.utils.kotlin

import it.unimi.dsi.fastutil.booleans.*
import it.unimi.dsi.fastutil.bytes.*
import it.unimi.dsi.fastutil.chars.*
import it.unimi.dsi.fastutil.doubles.*
import it.unimi.dsi.fastutil.floats.*
import it.unimi.dsi.fastutil.ints.*
import it.unimi.dsi.fastutil.longs.*
import it.unimi.dsi.fastutil.objects.*
import it.unimi.dsi.fastutil.shorts.*

// Reference: https://fastutil.di.unimi.it/docs/it/unimi/dsi/fastutil/Pair.html

inline operator fun BooleanBooleanPair.component1() = leftBoolean()
inline operator fun BooleanBooleanPair.component2() = rightBoolean()

inline operator fun BooleanBytePair.component1() = leftBoolean()
inline operator fun BooleanBytePair.component2() = rightByte()

inline operator fun BooleanCharPair.component1() = leftBoolean()
inline operator fun BooleanCharPair.component2() = rightChar()

inline operator fun BooleanDoublePair.component1() = leftBoolean()
inline operator fun BooleanDoublePair.component2() = rightDouble()

inline operator fun BooleanFloatPair.component1() = leftBoolean()
inline operator fun BooleanFloatPair.component2() = rightFloat()

inline operator fun BooleanIntPair.component1() = leftBoolean()
inline operator fun BooleanIntPair.component2() = rightInt()

inline operator fun BooleanLongPair.component1() = leftBoolean()
inline operator fun BooleanLongPair.component2() = rightLong()

inline operator fun <V> BooleanObjectPair<V>.component1() = leftBoolean()
inline operator fun <V> BooleanObjectPair<V>.component2() = right()

inline operator fun <V> BooleanReferencePair<V>.component1() = leftBoolean()
inline operator fun <V> BooleanReferencePair<V>.component2() = right()

inline operator fun BooleanShortPair.component1() = leftBoolean()
inline operator fun BooleanShortPair.component2() = rightShort()

inline operator fun ByteBooleanPair.component1() = leftByte()
inline operator fun ByteBooleanPair.component2() = rightBoolean()

inline operator fun ByteBytePair.component1() = leftByte()
inline operator fun ByteBytePair.component2() = rightByte()

inline operator fun ByteCharPair.component1() = leftByte()
inline operator fun ByteCharPair.component2() = rightChar()

inline operator fun ByteDoublePair.component1() = leftByte()
inline operator fun ByteDoublePair.component2() = rightDouble()

inline operator fun ByteFloatPair.component1() = leftByte()
inline operator fun ByteFloatPair.component2() = rightFloat()

inline operator fun ByteIntPair.component1() = leftByte()
inline operator fun ByteIntPair.component2() = rightInt()

inline operator fun ByteLongPair.component1() = leftByte()
inline operator fun ByteLongPair.component2() = rightLong()

inline operator fun <V> ByteObjectPair<V>.component1() = leftByte()
inline operator fun <V> ByteObjectPair<V>.component2() = right()

inline operator fun <V> ByteReferencePair<V>.component1() = leftByte()
inline operator fun <V> ByteReferencePair<V>.component2() = right()

inline operator fun ByteShortPair.component1() = leftByte()
inline operator fun ByteShortPair.component2() = rightShort()

inline operator fun CharBooleanPair.component1() = leftChar()
inline operator fun CharBooleanPair.component2() = rightBoolean()

inline operator fun CharBytePair.component1() = leftChar()
inline operator fun CharBytePair.component2() = rightByte()

inline operator fun CharCharPair.component1() = leftChar()
inline operator fun CharCharPair.component2() = rightChar()

inline operator fun CharDoublePair.component1() = leftChar()
inline operator fun CharDoublePair.component2() = rightDouble()

inline operator fun CharFloatPair.component1() = leftChar()
inline operator fun CharFloatPair.component2() = rightFloat()

inline operator fun CharIntPair.component1() = leftChar()
inline operator fun CharIntPair.component2() = rightInt()

inline operator fun CharLongPair.component1() = leftChar()
inline operator fun CharLongPair.component2() = rightLong()

inline operator fun <V> CharObjectPair<V>.component1() = leftChar()
inline operator fun <V> CharObjectPair<V>.component2() = right()

inline operator fun <V> CharReferencePair<V>.component1() = leftChar()
inline operator fun <V> CharReferencePair<V>.component2() = right()

inline operator fun CharShortPair.component1() = leftChar()
inline operator fun CharShortPair.component2() = rightShort()

inline operator fun DoubleBooleanPair.component1() = leftDouble()
inline operator fun DoubleBooleanPair.component2() = rightBoolean()

inline operator fun DoubleBytePair.component1() = leftDouble()
inline operator fun DoubleBytePair.component2() = rightByte()

inline operator fun DoubleCharPair.component1() = leftDouble()
inline operator fun DoubleCharPair.component2() = rightChar()

inline operator fun DoubleDoublePair.component1() = leftDouble()
inline operator fun DoubleDoublePair.component2() = rightDouble()

inline operator fun DoubleFloatPair.component1() = leftDouble()
inline operator fun DoubleFloatPair.component2() = rightFloat()

inline operator fun DoubleIntPair.component1() = leftDouble()
inline operator fun DoubleIntPair.component2() = rightInt()

inline operator fun DoubleLongPair.component1() = leftDouble()
inline operator fun DoubleLongPair.component2() = rightLong()

inline operator fun <V> DoubleObjectPair<V>.component1() = leftDouble()
inline operator fun <V> DoubleObjectPair<V>.component2() = right()

inline operator fun <V> DoubleReferencePair<V>.component1() = leftDouble()
inline operator fun <V> DoubleReferencePair<V>.component2() = right()

inline operator fun DoubleShortPair.component1() = leftDouble()
inline operator fun DoubleShortPair.component2() = rightShort()

inline operator fun FloatBooleanPair.component1() = leftFloat()
inline operator fun FloatBooleanPair.component2() = rightBoolean()

inline operator fun FloatBytePair.component1() = leftFloat()
inline operator fun FloatBytePair.component2() = rightByte()

inline operator fun FloatCharPair.component1() = leftFloat()
inline operator fun FloatCharPair.component2() = rightChar()

inline operator fun FloatDoublePair.component1() = leftFloat()
inline operator fun FloatDoublePair.component2() = rightDouble()

inline operator fun FloatFloatPair.component1() = leftFloat()
inline operator fun FloatFloatPair.component2() = rightFloat()

inline operator fun FloatIntPair.component1() = leftFloat()
inline operator fun FloatIntPair.component2() = rightInt()

inline operator fun FloatLongPair.component1() = leftFloat()
inline operator fun FloatLongPair.component2() = rightLong()

inline operator fun <V> FloatObjectPair<V>.component1() = leftFloat()
inline operator fun <V> FloatObjectPair<V>.component2() = right()

inline operator fun <V> FloatReferencePair<V>.component1() = leftFloat()
inline operator fun <V> FloatReferencePair<V>.component2() = right()

inline operator fun FloatShortPair.component1() = leftFloat()
inline operator fun FloatShortPair.component2() = rightShort()

inline operator fun IntBooleanPair.component1() = leftInt()
inline operator fun IntBooleanPair.component2() = rightBoolean()

inline operator fun IntBytePair.component1() = leftInt()
inline operator fun IntBytePair.component2() = rightByte()

inline operator fun IntCharPair.component1() = leftInt()
inline operator fun IntCharPair.component2() = rightChar()

inline operator fun IntDoublePair.component1() = leftInt()
inline operator fun IntDoublePair.component2() = rightDouble()

inline operator fun IntFloatPair.component1() = leftInt()
inline operator fun IntFloatPair.component2() = rightFloat()

inline operator fun IntIntPair.component1() = leftInt()
inline operator fun IntIntPair.component2() = rightInt()

inline operator fun IntLongPair.component1() = leftInt()
inline operator fun IntLongPair.component2() = rightLong()

inline operator fun <V> IntObjectPair<V>.component1() = leftInt()
inline operator fun <V> IntObjectPair<V>.component2() = right()

inline operator fun <V> IntReferencePair<V>.component1() = leftInt()
inline operator fun <V> IntReferencePair<V>.component2() = right()

inline operator fun IntShortPair.component1() = leftInt()
inline operator fun IntShortPair.component2() = rightShort()

inline operator fun LongBooleanPair.component1() = leftLong()
inline operator fun LongBooleanPair.component2() = rightBoolean()

inline operator fun LongBytePair.component1() = leftLong()
inline operator fun LongBytePair.component2() = rightByte()

inline operator fun LongCharPair.component1() = leftLong()
inline operator fun LongCharPair.component2() = rightChar()

inline operator fun LongDoublePair.component1() = leftLong()
inline operator fun LongDoublePair.component2() = rightDouble()

inline operator fun LongFloatPair.component1() = leftLong()
inline operator fun LongFloatPair.component2() = rightFloat()

inline operator fun LongIntPair.component1() = leftLong()
inline operator fun LongIntPair.component2() = rightInt()

inline operator fun LongLongPair.component1() = leftLong()
inline operator fun LongLongPair.component2() = rightLong()

inline operator fun <V> LongObjectPair<V>.component1() = leftLong()
inline operator fun <V> LongObjectPair<V>.component2() = right()

inline operator fun <V> LongReferencePair<V>.component1() = leftLong()
inline operator fun <V> LongReferencePair<V>.component2() = right()

inline operator fun LongShortPair.component1() = leftLong()
inline operator fun LongShortPair.component2() = rightShort()

inline operator fun <K> ObjectBooleanPair<K>.component1() = left()
inline operator fun <K> ObjectBooleanPair<K>.component2() = rightBoolean()

inline operator fun <K> ObjectBytePair<K>.component1() = left()
inline operator fun <K> ObjectBytePair<K>.component2() = rightByte()

inline operator fun <K> ObjectCharPair<K>.component1() = left()
inline operator fun <K> ObjectCharPair<K>.component2() = rightChar()

inline operator fun <K> ObjectDoublePair<K>.component1() = left()
inline operator fun <K> ObjectDoublePair<K>.component2() = rightDouble()

inline operator fun <K> ObjectFloatPair<K>.component1() = left()
inline operator fun <K> ObjectFloatPair<K>.component2() = rightFloat()

inline operator fun <K> ObjectIntPair<K>.component1() = left()
inline operator fun <K> ObjectIntPair<K>.component2() = rightInt()

inline operator fun <K> ObjectLongPair<K>.component1() = left()
inline operator fun <K> ObjectLongPair<K>.component2() = rightLong()

inline operator fun <K, V> ObjectReferencePair<K, V>.component1() = left()
inline operator fun <K, V> ObjectReferencePair<K, V>.component2() = right()

inline operator fun <K> ObjectShortPair<K>.component1() = left()
inline operator fun <K> ObjectShortPair<K>.component2() = rightShort()

inline operator fun <K> ReferenceBooleanPair<K>.component1() = left()
inline operator fun <K> ReferenceBooleanPair<K>.component2() = rightBoolean()

inline operator fun <K> ReferenceBytePair<K>.component1() = left()
inline operator fun <K> ReferenceBytePair<K>.component2() = rightByte()

inline operator fun <K> ReferenceCharPair<K>.component1() = left()
inline operator fun <K> ReferenceCharPair<K>.component2() = rightChar()

inline operator fun <K> ReferenceDoublePair<K>.component1() = left()
inline operator fun <K> ReferenceDoublePair<K>.component2() = rightDouble()

inline operator fun <K> ReferenceFloatPair<K>.component1() = left()
inline operator fun <K> ReferenceFloatPair<K>.component2() = rightFloat()

inline operator fun <K> ReferenceIntPair<K>.component1() = left()
inline operator fun <K> ReferenceIntPair<K>.component2() = rightInt()

inline operator fun <K> ReferenceLongPair<K>.component1() = left()
inline operator fun <K> ReferenceLongPair<K>.component2() = rightLong()

inline operator fun <K, V> ReferenceObjectPair<K, V>.component1() = left()
inline operator fun <K, V> ReferenceObjectPair<K, V>.component2() = right()

inline operator fun <K, V> ReferenceReferencePair<K, V>.component1() = left()
inline operator fun <K, V> ReferenceReferencePair<K, V>.component2() = right()

inline operator fun <K> ReferenceShortPair<K>.component1() = left()
inline operator fun <K> ReferenceShortPair<K>.component2() = rightShort()

inline operator fun ShortBooleanPair.component1() = leftShort()
inline operator fun ShortBooleanPair.component2() = rightBoolean()

inline operator fun ShortBytePair.component1() = leftShort()
inline operator fun ShortBytePair.component2() = rightByte()

inline operator fun ShortCharPair.component1() = leftShort()
inline operator fun ShortCharPair.component2() = rightChar()

inline operator fun ShortDoublePair.component1() = leftShort()
inline operator fun ShortDoublePair.component2() = rightDouble()

inline operator fun ShortFloatPair.component1() = leftShort()
inline operator fun ShortFloatPair.component2() = rightFloat()

inline operator fun ShortIntPair.component1() = leftShort()
inline operator fun ShortIntPair.component2() = rightInt()

inline operator fun ShortLongPair.component1() = leftShort()
inline operator fun ShortLongPair.component2() = rightLong()

inline operator fun <V> ShortObjectPair<V>.component1() = leftShort()
inline operator fun <V> ShortObjectPair<V>.component2() = right()

inline operator fun <V> ShortReferencePair<V>.component1() = leftShort()
inline operator fun <V> ShortReferencePair<V>.component2() = right()

inline operator fun ShortShortPair.component1() = leftShort()
inline operator fun ShortShortPair.component2() = rightShort()
