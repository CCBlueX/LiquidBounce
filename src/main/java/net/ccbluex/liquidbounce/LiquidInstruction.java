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

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.*;

public final class LiquidInstruction {

  public static void main(String[] args) {
    var rootFrame = new JFrame(LiquidBounce.CLIENT_NAME);
    rootFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    rootFrame.setSize(800, 600);
    rootFrame.setLocationRelativeTo(null);
    rootFrame.setVisible(true);

    var icons = Stream.of(
        "/resources/liquidbounce/icon_64x64.png",
        "/resources/liquidbounce/icon_32x32.png",
        "/resources/liquidbounce/icon_16x16.png"
    ).map(LiquidInstruction.class::getResource)
        .filter(Objects::nonNull)
        .map(it -> new ImageIcon(it).getImage())
        .toList();

    rootFrame.setIconImages(icons);

    JOptionPane.showMessageDialog(
        rootFrame,
        GitInfo.entries().stream()
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining("\n")),
        LiquidBounce.CLIENT_NAME,
        JOptionPane.INFORMATION_MESSAGE
    );
  }

}
