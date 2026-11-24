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

package net.ccbluex.liquidbounce.utils.client;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * Lunar doesn't like Kotlin so they use OkHttp 3.x without `$Companion`
 * So these extensions are incompatible in Kotlin code.
 */
@NullMarked
public final class OkHttpUtils {

    private OkHttpUtils() {}

    public static HttpUrl toHttpUrl(String s) {
        return HttpUrl.get(s);
    }

    public static @Nullable HttpUrl toHttpUrlOrNull(@Nullable String s) {
        return s == null ? null : HttpUrl.parse(s);
    }

    public static MediaType toMediaType(String s) {
        return MediaType.get(s);
    }

    public static @Nullable MediaType toMediaTypeOrNull(@Nullable String s) {
        return s == null ? null : MediaType.parse(s);
    }

    @SuppressWarnings("deprecation")
    public static RequestBody toRequestBody(String s, MediaType contentType) {
        return RequestBody.create(contentType, s);
    }

    @SuppressWarnings("deprecation")
    public static RequestBody toRequestBody(byte[] s, MediaType contentType) {
        return RequestBody.create(contentType, s);
    }

    @SuppressWarnings("deprecation")
    public static RequestBody asRequestBody(File f, MediaType contentType) {
        return RequestBody.create(contentType, f);
    }

}
