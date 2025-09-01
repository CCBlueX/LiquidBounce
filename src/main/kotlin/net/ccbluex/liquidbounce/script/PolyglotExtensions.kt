package net.ccbluex.liquidbounce.script

import org.graalvm.polyglot.Value

inline fun <reified T> Value.asArray(): Array<T> = this.asType<Array<T>>()

fun Value.asBooleanArray(): BooleanArray = this.asType<BooleanArray>()

fun Value.asByteArray(): ByteArray = this.asType<ByteArray>()

fun Value.asCharArray(): CharArray = this.asType<CharArray>()

fun Value.asShortArray(): ShortArray = this.asType<ShortArray>()

fun Value.asIntArray(): IntArray = this.asType<IntArray>()

fun Value.asFloatArray(): FloatArray = this.asType<FloatArray>()

fun Value.asLongArray(): LongArray = this.asType<LongArray>()

fun Value.asDoubleArray(): DoubleArray = this.asType<DoubleArray>()

inline fun <reified T> Value.asType(): T = this.`as`(T::class.java)
