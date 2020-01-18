package net.ccbluex.liquidbounce.utils;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.modules.misc.NameProtect;
import net.ccbluex.liquidbounce.features.module.modules.misc.Spammer;
import net.ccbluex.liquidbounce.utils.misc.StringUtils;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.value.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game Minecraft
 */
@SideOnly(Side.CLIENT)
public final class SettingsUtils {

    public static void executeScript(final List<String> script) {
        for (String scriptLine : script) {
            String[] split = scriptLine.split(" ");

            if (split.length > 1) {
                switch (split[0]) {
                    case "chat":
                        ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §e" + ColorUtils.translateAlternateColorCodes(StringUtils.toCompleteString(split, 1)));
                        break;
                    case "load":
                        final String urlRaw = StringUtils.toCompleteString(split, 1);
                        final String url = urlRaw.startsWith("http") ? urlRaw : "https://ccbluex.github.io/FileCloud/" + LiquidBounce.CLIENT_NAME + "/autosettings/" + urlRaw.toLowerCase();

                        try {
                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §7Loading settings from §a§l" + url + "§7...");

                            final List<String> nextScript = new ArrayList<>();
                            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
                            for (String line; (line = bufferedReader.readLine()) != null; )
                                if (!line.startsWith("#") && !line.isEmpty())
                                    nextScript.add(line);
                            bufferedReader.close();
                            SettingsUtils.executeScript(nextScript);

                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §7Loaded settings from §a§l" + url + "§7.");
                        } catch (final Exception e) {
                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §7Failed to load settings from §a§l" + url + "§7.");
                        }
                        break;
                    case "targetPlayer":
                        EntityUtils.targetPlayer = split[1].equalsIgnoreCase("true");
                        ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + split[0] + "§7 set to §c§l" + EntityUtils.targetPlayer + "§7.");
                        break;
                    case "targetMobs":
                        EntityUtils.targetMobs = split[1].equalsIgnoreCase("true");
                        ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + split[0] + "§7 set to §c§l" + EntityUtils.targetMobs + "§7.");
                        break;
                    case "targetAnimals":
                        EntityUtils.targetAnimals = split[1].equalsIgnoreCase("true");
                        ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + split[0] + "§7 set to §c§l" + EntityUtils.targetAnimals + "§7.");
                        break;
                    case "targetInvisible":
                        EntityUtils.targetInvisible = split[1].equalsIgnoreCase("true");
                        ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + split[0] + "§7 set to §c§l" + EntityUtils.targetInvisible + "§7.");
                        break;
                    case "targetDead":
                        EntityUtils.targetDead = split[1].equalsIgnoreCase("true");
                        ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + split[0] + "§7 set to §c§l" + EntityUtils.targetDead + "§7.");
                        break;
                    default:
                        if (split.length != 3) {
                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §cSyntax error in setting script.\n§8§lLine: §7" + scriptLine);
                            break;
                        }

                        String moduleName = split[0];
                        String valueName = split[1];
                        String value = split[2];

                        Module module = LiquidBounce.moduleManager.getModule(moduleName);

                        if (module == null) {
                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §cModule §a§l" + moduleName + "§c was not found!");
                            break;
                        }
                        if (module.getCategory() == ModuleCategory.RENDER) {
                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §cModule §a§l" + moduleName + "§c is a render module!");
                            break;
                        }
                        if (valueName.equalsIgnoreCase("toggle")) {
                            module.setState(value.equalsIgnoreCase("true"));
                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + module.getName() + " §7was toggled §c§l" + (module.getState() ? "on" : "off") + "§7.");
                            break;
                        }
                        if (valueName.equalsIgnoreCase("bind")) {
                            module.setKeyBind(Keyboard.getKeyIndex(value));
                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + module.getName() + " §7was bound to §c§l" + Keyboard.getKeyName(module.getKeyBind()) + "§7.");
                            break;
                        }

                        Value<?> moduleValue = module.getValue(valueName);

                        if (moduleValue == null) {
                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §cValue §a§l" + valueName + "§c don't found in module §a§l" + moduleName + "§c.");
                            break;
                        }

                        try {
                            if (moduleValue instanceof BoolValue)
                                ((BoolValue) moduleValue).changeValue(Boolean.parseBoolean(value));
                            else if (moduleValue instanceof TextValue) {
                                ((TextValue) moduleValue).changeValue(value);
                            } else if (moduleValue instanceof ListValue) {
                                ((ListValue) moduleValue).changeValue(value);
                            } else {
                                if (moduleValue instanceof FloatValue)
                                    ((FloatValue) moduleValue).changeValue(Float.parseFloat(value));
                                else if (moduleValue instanceof IntegerValue)
                                    ((IntegerValue) moduleValue).changeValue(Integer.parseInt(value));
                                else
                                    throw new UnsupportedOperationException();
                            }

                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + module.getName() + "§7 value §8§l" + moduleValue.getName() + "§7 set to §c§l" + value + "§7.");
                        } catch (final Exception e) {
                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + e.getClass().getName() + "§7(" + e.getMessage() + ") §cAn Exception occurred while setting §a§l" + value + "§c to §a§l" + moduleValue.getName() + "§c in §a§l" + module.getName() + "§c.");

                        }
                        break;
                }
            }
        }

        LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
    }

    public static String generateScript(final boolean values, final boolean binds, final boolean states) {
        final StringBuilder stringBuilder = new StringBuilder();

        LiquidBounce.moduleManager.getModules().stream().filter(module -> module.getCategory() != ModuleCategory.RENDER && !(module instanceof NameProtect) && !(module instanceof Spammer)).forEach(module -> {
            if (values) {
                module.getValues().forEach(value -> stringBuilder.append(module.getName()).append(" ").append(value.getName()).append(" ").append(value.get()).append("\n"));
            }

            if (states)
                stringBuilder.append(module.getName()).append(" toggle ").append(module.getState()).append("\n");

            if (binds)
                stringBuilder.append(module.getName()).append(" bind ").append(Keyboard.getKeyName(module.getKeyBind())).append("\n");
        });

        return stringBuilder.toString();
    }
}