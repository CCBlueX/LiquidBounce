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
 *
 *
 */
package net.ccbluex.liquidbounce.injection.mixins.djl;

import ai.djl.util.Utils;
import net.ccbluex.liquidbounce.api.core.HttpClient;
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine;
import net.ccbluex.liquidbounce.mcef.listeners.OkHttpProgressInterceptor;
import net.ccbluex.liquidbounce.utils.client.ClientUtilsKt;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import static ai.djl.util.Utils.isOfflineMode;

@Pseudo
@Mixin(value = Utils.class)
public class MixinUtils {

    @Unique
    private static final ThreadLocal<String> CURRENT_URL = new ThreadLocal<>();

    @Unique
    private static final OkHttpClient CLIENT = HttpClient.getClient().newBuilder()
            .addNetworkInterceptor(new OkHttpProgressInterceptor((bytesRead, contentLength, done) -> {
                var url = CURRENT_URL.get();
                var mainTask = DeepLearningEngine.getTask();

                if (mainTask == null) {
                    ClientUtilsKt.getLogger().warn("Intercepted progress while no main task is running.");
                    return;
                }

                if (url == null) {
                    ClientUtilsKt.getLogger().warn("Intercepted progress while no URL is set.");
                    return;
                }

                var task = mainTask.getOrCreateFileTask(url);
                task.update(bytesRead, contentLength);

                if (done) {
                    task.setCompleted(true);
                    CURRENT_URL.remove();
                }
            }))
            .build();

    @Inject(
            method = "openUrl(Ljava/net/URL;Ljava/util/Map;)Ljava/io/InputStream;",
            at = @At("HEAD"),
            remap = false,
            cancellable = true
    )
    private static void openUrl(URL url, Map<String, String> headers, CallbackInfoReturnable<InputStream> cir) throws IOException {
        var protocol = url.getProtocol();
        if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
            if (isOfflineMode()) {
                throw new IOException("Offline model is enabled.");
            }

            var request = new Request.Builder()
                    .url(url)
                    .headers(Headers.of(headers))
                    .build();
            CURRENT_URL.set(url.toString());
            var response = CLIENT.newCall(request).execute();
            cir.setReturnValue(response.body().byteStream());
        }
    }

}
