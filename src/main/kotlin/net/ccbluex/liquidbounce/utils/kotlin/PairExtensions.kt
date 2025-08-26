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

inline operator fun BooleanBooleanPair.component1() = keyBoolean()
inline operator fun BooleanBooleanPair.component2() = valueBoolean()

inline operator fun BooleanBytePair.component1() = keyBoolean()
inline operator fun BooleanBytePair.component2() = valueByte()

inline operator fun BooleanCharPair.component1() = keyBoolean()
inline operator fun BooleanCharPair.component2() = valueChar()

inline operator fun BooleanDoublePair.component1() = keyBoolean()
inline operator fun BooleanDoublePair.component2() = valueDouble()

inline operator fun BooleanFloatPair.component1() = keyBoolean()
inline operator fun BooleanFloatPair.component2() = valueFloat()

inline operator fun BooleanIntPair.component1() = keyBoolean()
inline operator fun BooleanIntPair.component2() = valueInt()

inline operator fun BooleanLongPair.component1() = keyBoolean()
inline operator fun BooleanLongPair.component2() = valueLong()

inline operator fun <V> BooleanObjectPair<V>.component1() = keyBoolean()
inline operator fun <V> BooleanObjectPair<V>.component2() = value()

inline operator fun <V> BooleanReferencePair<V>.component1() = keyBoolean()
inline operator fun <V> BooleanReferencePair<V>.component2() = value()

inline operator fun BooleanShortPair.component1() = keyBoolean()
inline operator fun BooleanShortPair.component2() = valueShort()

inline operator fun ByteBooleanPair.component1() = keyByte()
inline operator fun ByteBooleanPair.component2() = valueBoolean()

inline operator fun ByteBytePair.component1() = keyByte()
inline operator fun ByteBytePair.component2() = valueByte()

inline operator fun ByteCharPair.component1() = keyByte()
inline operator fun ByteCharPair.component2() = valueChar()

inline operator fun ByteDoublePair.component1() = keyByte()
inline operator fun ByteDoublePair.component2() = valueDouble()

inline operator fun ByteFloatPair.component1() = keyByte()
inline operator fun ByteFloatPair.component2() = valueFloat()

inline operator fun ByteIntPair.component1() = keyByte()
inline operator fun ByteIntPair.component2() = valueInt()

inline operator fun ByteLongPair.component1() = keyByte()
inline operator fun ByteLongPair.component2() = valueLong()

inline operator fun <V> ByteObjectPair<V>.component1() = keyByte()
inline operator fun <V> ByteObjectPair<V>.component2() = value()

inline operator fun <V> ByteReferencePair<V>.component1() = keyByte()
inline operator fun <V> ByteReferencePair<V>.component2() = value()

inline operator fun ByteShortPair.component1() = keyByte()
inline operator fun ByteShortPair.component2() = valueShort()

inline operator fun CharBooleanPair.component1() = keyChar()
inline operator fun CharBooleanPair.component2() = valueBoolean()

inline operator fun CharBytePair.component1() = keyChar()
inline operator fun CharBytePair.component2() = valueByte()

inline operator fun CharCharPair.component1() = keyChar()
inline operator fun CharCharPair.component2() = valueChar()

inline operator fun CharDoublePair.component1() = keyChar()
inline operator fun CharDoublePair.component2() = valueDouble()

inline operator fun CharFloatPair.component1() = keyChar()
inline operator fun CharFloatPair.component2() = valueFloat()

inline operator fun CharIntPair.component1() = keyChar()
inline operator fun CharIntPair.component2() = valueInt()

inline operator fun CharLongPair.component1() = keyChar()
inline operator fun CharLongPair.component2() = valueLong()

inline operator fun <V> CharObjectPair<V>.component1() = keyChar()
inline operator fun <V> CharObjectPair<V>.component2() = value()

inline operator fun <V> CharReferencePair<V>.component1() = keyChar()
inline operator fun <V> CharReferencePair<V>.component2() = value()

inline operator fun CharShortPair.component1() = keyChar()
inline operator fun CharShortPair.component2() = valueShort()

inline operator fun DoubleBooleanPair.component1() = keyDouble()
inline operator fun DoubleBooleanPair.component2() = valueBoolean()

inline operator fun DoubleBytePair.component1() = keyDouble()
inline operator fun DoubleBytePair.component2() = valueByte()

inline operator fun DoubleCharPair.component1() = keyDouble()
inline operator fun DoubleCharPair.component2() = valueChar()

inline operator fun DoubleDoublePair.component1() = keyDouble()
inline operator fun DoubleDoublePair.component2() = valueDouble()

inline operator fun DoubleFloatPair.component1() = keyDouble()
inline operator fun DoubleFloatPair.component2() = valueFloat()

inline operator fun DoubleIntPair.component1() = keyDouble()
inline operator fun DoubleIntPair.component2() = valueInt()

inline operator fun DoubleLongPair.component1() = keyDouble()
inline operator fun DoubleLongPair.component2() = valueLong()

inline operator fun <V> DoubleObjectPair<V>.component1() = keyDouble()
inline operator fun <V> DoubleObjectPair<V>.component2() = value()

inline operator fun <V> DoubleReferencePair<V>.component1() = keyDouble()
inline operator fun <V> DoubleReferencePair<V>.component2() = value()

inline operator fun DoubleShortPair.component1() = keyDouble()
inline operator fun DoubleShortPair.component2() = valueShort()

inline operator fun FloatBooleanPair.component1() = keyFloat()
inline operator fun FloatBooleanPair.component2() = valueBoolean()

inline operator fun FloatBytePair.component1() = keyFloat()
inline operator fun FloatBytePair.component2() = valueByte()

inline operator fun FloatCharPair.component1() = keyFloat()
inline operator fun FloatCharPair.component2() = valueChar()

inline operator fun FloatDoublePair.component1() = keyFloat()
inline operator fun FloatDoublePair.component2() = valueDouble()

inline operator fun FloatFloatPair.component1() = keyFloat()
inline operator fun FloatFloatPair.component2() = valueFloat()

inline operator fun FloatIntPair.component1() = keyFloat()
inline operator fun FloatIntPair.component2() = valueInt()

inline operator fun FloatLongPair.component1() = keyFloat()
inline operator fun FloatLongPair.component2() = valueLong()

inline operator fun <V> FloatObjectPair<V>.component1() = keyFloat()
inline operator fun <V> FloatObjectPair<V>.component2() = value()

inline operator fun <V> FloatReferencePair<V>.component1() = keyFloat()
inline operator fun <V> FloatReferencePair<V>.component2() = value()

inline operator fun FloatShortPair.component1() = keyFloat()
inline operator fun FloatShortPair.component2() = valueShort()

inline operator fun IntBooleanPair.component1() = keyInt()
inline operator fun IntBooleanPair.component2() = valueBoolean()

inline operator fun IntBytePair.component1() = keyInt()
inline operator fun IntBytePair.component2() = valueByte()

inline operator fun IntCharPair.component1() = keyInt()
inline operator fun IntCharPair.component2() = valueChar()

inline operator fun IntDoublePair.component1() = keyInt()
inline operator fun IntDoublePair.component2() = valueDouble()

inline operator fun IntFloatPair.component1() = keyInt()
inline operator fun IntFloatPair.component2() = valueFloat()

inline operator fun IntIntPair.component1() = keyInt()
inline operator fun IntIntPair.component2() = valueInt()

inline operator fun IntLongPair.component1() = keyInt()
inline operator fun IntLongPair.component2() = valueLong()

inline operator fun <V> IntObjectPair<V>.component1() = keyInt()
inline operator fun <V> IntObjectPair<V>.component2() = value()

inline operator fun <V> IntReferencePair<V>.component1() = keyInt()
inline operator fun <V> IntReferencePair<V>.component2() = value()

inline operator fun IntShortPair.component1() = keyInt()
inline operator fun IntShortPair.component2() = valueShort()

inline operator fun LongBooleanPair.component1() = keyLong()
inline operator fun LongBooleanPair.component2() = valueBoolean()

inline operator fun LongBytePair.component1() = keyLong()
inline operator fun LongBytePair.component2() = valueByte()

inline operator fun LongCharPair.component1() = keyLong()
inline operator fun LongCharPair.component2() = valueChar()

inline operator fun LongDoublePair.component1() = keyLong()
inline operator fun LongDoublePair.component2() = valueDouble()

inline operator fun LongFloatPair.component1() = keyLong()
inline operator fun LongFloatPair.component2() = valueFloat()

inline operator fun LongIntPair.component1() = keyLong()
inline operator fun LongIntPair.component2() = valueInt()

inline operator fun LongLongPair.component1() = keyLong()
inline operator fun LongLongPair.component2() = valueLong()

inline operator fun <V> LongObjectPair<V>.component1() = keyLong()
inline operator fun <V> LongObjectPair<V>.component2() = value()

inline operator fun <V> LongReferencePair<V>.component1() = keyLong()
inline operator fun <V> LongReferencePair<V>.component2() = value()

inline operator fun LongShortPair.component1() = keyLong()
inline operator fun LongShortPair.component2() = valueShort()

inline operator fun <K> ObjectBooleanPair<K>.component1() = key()
inline operator fun <K> ObjectBooleanPair<K>.component2() = valueBoolean()

inline operator fun <K> ObjectBytePair<K>.component1() = key()
inline operator fun <K> ObjectBytePair<K>.component2() = valueByte()

inline operator fun <K> ObjectCharPair<K>.component1() = key()
inline operator fun <K> ObjectCharPair<K>.component2() = valueChar()

inline operator fun <K> ObjectDoublePair<K>.component1() = key()
inline operator fun <K> ObjectDoublePair<K>.component2() = valueDouble()

inline operator fun <K> ObjectFloatPair<K>.component1() = key()
inline operator fun <K> ObjectFloatPair<K>.component2() = valueFloat()

inline operator fun <K> ObjectIntPair<K>.component1() = key()
inline operator fun <K> ObjectIntPair<K>.component2() = valueInt()

inline operator fun <K> ObjectLongPair<K>.component1() = key()
inline operator fun <K> ObjectLongPair<K>.component2() = valueLong()

inline operator fun <K, V> ObjectReferencePair<K, V>.component1() = key()
inline operator fun <K, V> ObjectReferencePair<K, V>.component2() = value()

inline operator fun <K> ObjectShortPair<K>.component1() = key()
inline operator fun <K> ObjectShortPair<K>.component2() = valueShort()

inline operator fun <K> ReferenceBooleanPair<K>.component1() = key()
inline operator fun <K> ReferenceBooleanPair<K>.component2() = valueBoolean()

inline operator fun <K> ReferenceBytePair<K>.component1() = key()
inline operator fun <K> ReferenceBytePair<K>.component2() = valueByte()

inline operator fun <K> ReferenceCharPair<K>.component1() = key()
inline operator fun <K> ReferenceCharPair<K>.component2() = valueChar()

inline operator fun <K> ReferenceDoublePair<K>.component1() = key()
inline operator fun <K> ReferenceDoublePair<K>.component2() = valueDouble()

inline operator fun <K> ReferenceFloatPair<K>.component1() = key()
inline operator fun <K> ReferenceFloatPair<K>.component2() = valueFloat()

inline operator fun <K> ReferenceIntPair<K>.component1() = key()
inline operator fun <K> ReferenceIntPair<K>.component2() = valueInt()

inline operator fun <K> ReferenceLongPair<K>.component1() = key()
inline operator fun <K> ReferenceLongPair<K>.component2() = valueLong()

inline operator fun <K, V> ReferenceObjectPair<K, V>.component1() = key()
inline operator fun <K, V> ReferenceObjectPair<K, V>.component2() = value()

inline operator fun <K, V> ReferenceReferencePair<K, V>.component1() = key()
inline operator fun <K, V> ReferenceReferencePair<K, V>.component2() = value()

inline operator fun <K> ReferenceShortPair<K>.component1() = key()
inline operator fun <K> ReferenceShortPair<K>.component2() = valueShort()

inline operator fun ShortBooleanPair.component1() = keyShort()
inline operator fun ShortBooleanPair.component2() = valueBoolean()

inline operator fun ShortBytePair.component1() = keyShort()
inline operator fun ShortBytePair.component2() = valueByte()

inline operator fun ShortCharPair.component1() = keyShort()
inline operator fun ShortCharPair.component2() = valueChar()

inline operator fun ShortDoublePair.component1() = keyShort()
inline operator fun ShortDoublePair.component2() = valueDouble()

inline operator fun ShortFloatPair.component1() = keyShort()
inline operator fun ShortFloatPair.component2() = valueFloat()

inline operator fun ShortIntPair.component1() = keyShort()
inline operator fun ShortIntPair.component2() = valueInt()

inline operator fun ShortLongPair.component1() = keyShort()
inline operator fun ShortLongPair.component2() = valueLong()

inline operator fun <V> ShortObjectPair<V>.component1() = keyShort()
inline operator fun <V> ShortObjectPair<V>.component2() = value()

inline operator fun <V> ShortReferencePair<V>.component1() = keyShort()
inline operator fun <V> ShortReferencePair<V>.component2() = value()

inline operator fun ShortShortPair.component1() = keyShort()
inline operator fun ShortShortPair.component2() = valueShort()
