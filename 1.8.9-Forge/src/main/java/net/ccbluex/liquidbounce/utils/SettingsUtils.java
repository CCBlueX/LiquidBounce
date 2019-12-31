package net.ccbluex.liquidbounce.utils;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleManager;
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
 * @game Minecraft
 * @author CCBlueX
 */
@SideOnly(Side.CLIENT)
public final class SettingsUtils {

    public static void executeScript(final List<String> script) {
        for(final String scriptLine : script) {
            final String[] split = scriptLine.split(" ");

            if(split.length > 1) {
                switch(split[0]) {
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
                            for(String line; (line = bufferedReader.readLine()) != null; )
                                if(!line.startsWith("#") && !line.isEmpty())
                                    nextScript.add(line);
                            bufferedReader.close();
                            SettingsUtils.executeScript(nextScript);

                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §7Loaded settings from §a§l" + url + "§7.");
                        }catch(final Exception e) {
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
                        if(split.length == 3) {
                            final String moduleName = split[0];
                            final String valueName = split[1];
                            final String value = split[2];

                            final Module module = ModuleManager.getModule(moduleName);
                            if(module != null) {
                                if(module.getCategory() != ModuleCategory.RENDER) {
                                    if(valueName.equalsIgnoreCase("toggle")) {
                                        module.setState(value.equalsIgnoreCase("true"));
                                        ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + module.getName() + " §7was toggled §c§l" + (module.getState() ? "on" : "off") + "§7.");
                                    }else if(valueName.equalsIgnoreCase("bind")) {
                                        module.setKeyBind(Keyboard.getKeyIndex(value));
                                        ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + module.getName() + " §7was bound to §c§l" + Keyboard.getKeyName(module.getKeyBind()) + "§7.");
                                    }else{
                                        final Value moduleValue = module.getValue(valueName);

                                        if(moduleValue != null) {
                                            try {
                                                if (moduleValue instanceof BoolValue)
                                                    moduleValue.changeValue(Boolean.parseBoolean(value));
                                                else if(moduleValue instanceof TextValue
                                                        || moduleValue instanceof ListValue)
                                                    moduleValue.changeValue(value);
                                                else if(moduleValue instanceof FloatValue)
                                                    moduleValue.changeValue(Float.parseFloat(value));
                                                else if(moduleValue instanceof IntegerValue)
                                                    moduleValue.changeValue(Integer.parseInt(value));
                                                else
                                                    throw new UnsupportedOperationException();

                                                ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + module.getName() + "§7 value §8§l" + moduleValue.getName() + "§7 set to §c§l" + value + "§7.");
                                            }catch(final Exception e) {
                                                ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §a§l" + e.getClass().getName() + "§7(" + e.getMessage() + ") §cexception while set §a§l" + value + "§c to §a§l" + moduleValue.getName() + "§c in §a§l" + module.getName() + "§c.");
                                            }
                                        }else
                                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §cValue §a§l" + valueName + "§c don't found in module §a§l" + moduleName + "§c.");
                                        break;
                                    }
                                }else
                                    ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §cModule §a§l" + moduleName + "§c is a render module!");
                            }else
                                ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §cModule §a§l" + moduleName + "§c was not found!");
                        }else
                            ClientUtils.displayChatMessage("§7[§3§lAutoSettings§7] §cSyntax error in setting script.\n§8§lLine: §7" + scriptLine);
                        break;
                }
            }
        }

        LiquidBounce.CLIENT.fileManager.saveConfig(LiquidBounce.CLIENT.fileManager.valuesConfig);
    }

    public static String generateScript(final boolean values, final boolean binds, final boolean states) {
        final StringBuilder stringBuilder = new StringBuilder();

        ModuleManager.getModules().stream().filter(module -> module.getCategory() != ModuleCategory.RENDER && !(module instanceof NameProtect) && !(module instanceof Spammer)).forEach(module -> {
            if(values)
                for(final Value value : module.getValues())
                    stringBuilder.append(module.getName()).append(" ").append(value.getName()).append(" ").append(value.get()).append("\n");

            if(states)
                stringBuilder.append(module.getName()).append(" toggle ").append(module.getState()).append("\n");

            if(binds)
                stringBuilder.append(module.getName()).append(" bind ").append(Keyboard.getKeyName(module.getKeyBind())).append("\n");
        });

        return stringBuilder.toString();
    }
}