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
package net.ccbluex.liquidbounce.integration.task;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Runs in a short-lived JVM started with system proxy detection enabled. */
public final class SystemProxyQuery {
    private SystemProxyQuery() { }

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Expected one URI");
        for (Proxy proxy : ProxySelector.getDefault().select(URI.create(args[0]))) {
            if (proxy.type() == Proxy.Type.DIRECT) {
                System.out.println("DIRECT");
            } else if (proxy.address() instanceof InetSocketAddress address) {
                String host = Base64.getEncoder().encodeToString(address.getHostString().getBytes(StandardCharsets.UTF_8));
                System.out.println(proxy.type() + "\t" + host + "\t" + address.getPort());
            } else {
                throw new IllegalStateException("Unsupported system proxy address");
            }
        }
    }
}
