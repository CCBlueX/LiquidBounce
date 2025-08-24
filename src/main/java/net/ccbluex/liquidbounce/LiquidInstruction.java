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
 */
package net.ccbluex.liquidbounce;

import net.ccbluex.liquidbounce.utils.client.GitInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.*;

/**
 * This class is for manual open of the jar file.
 * It should not use any other external libraries such as Kotlin stdlib, JavaFX or Minecraft
 * because they are not included in the jar.
 */
public final class LiquidInstruction {

  private LiquidInstruction() {}

  private static @NotNull List<Image> loadIcons() {
    return Stream.of(
            "/resources/liquidbounce/icon_64x64.png",
            "/resources/liquidbounce/icon_32x32.png",
            "/resources/liquidbounce/icon_16x16.png"
        ).map(LiquidInstruction.class::getResource)
        .filter(Objects::nonNull)
        .map(it -> new ImageIcon(it).getImage())
        .toList();
  }

  private static boolean browse(@NotNull URI uri) {
    try {
      Desktop.getDesktop().browse(uri);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public static void main(String[] args) {
    var buttons = new String[]{"Manual installation guide", "Download LiquidLauncher", "Open GitHub", "Open Discord", "Close"};

    var result = JOptionPane.showOptionDialog(
        null,
        """
            Welcome to %s!
            
            This file is a Fabric mod, you should use it with a launcher and Fabric.
            
            You can click the button below to open the manual installation guide, or download the LiquidLauncher.
            If you need help, you can join our Discord server for support.
            If you found any bugs or have any feature requests, please report them on our GitHub repository.
            """.formatted(LiquidBounce.CLIENT_NAME),
        LiquidBounce.CLIENT_NAME + " by " + LiquidBounce.CLIENT_AUTHOR + " (" + GitInfo.version() + ")",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.INFORMATION_MESSAGE,
        loadIcons().stream().findFirst().map(ImageIcon::new).orElse(null),
        buttons,
        buttons[0]
    );

    switch (result) {
      case 0 -> browse(URI.create("https://liquidbounce.net/docs/get-started/manual-installation"));
      case 1 -> browse(URI.create("https://liquidbounce.net/download"));
      case 2 -> browse(URI.create("https://github.com/CCBlueX/LiquidBounce"));
      case 3 -> browse(URI.create("https://liquidbounce.net/discord"));
      default -> System.exit(0);
    }
  }

}
