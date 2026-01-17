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

package net.ccbluex.liquidbounce.common;

import com.mojang.blaze3d.platform.NativeImage;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.Objects;

/**
 * LiquidBounce Splash Screen Logo
 * <p>
 * Should be drawn using [CustomRenderPhase::getTextureBilinear] to make it look smoother.
 */
@Environment(EnvType.CLIENT)
public final class ClientLogoTexture extends ReloadableTexture {

    public static final Identifier CLIENT_LOGO = LiquidBounce.identifier("logo");
    public static final int WIDTH = 1920;
    public static final int HEIGHT = 721;

    public ClientLogoTexture() {
        super(CLIENT_LOGO);
    }

    @Override
    public TextureContents loadContents(ResourceManager resourceManager) {
        try (var stream = LiquidBounce.class.getResourceAsStream("/resources/liquidbounce/logo_banner.png")) {
            var nativeImage = NativeImage.read(Objects.requireNonNull(stream));

            return new TextureContents(nativeImage, new TextureMetadataSection(true, false, MipmapStrategy.AUTO, TextureMetadataSection.DEFAULT_ALPHA_CUTOFF_BIAS));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
