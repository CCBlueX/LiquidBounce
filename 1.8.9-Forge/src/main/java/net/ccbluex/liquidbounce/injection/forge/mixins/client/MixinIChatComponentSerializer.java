package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(IChatComponent.Serializer.class)
public class MixinIChatComponentSerializer {
    @ModifyVariable(method = "jsonToComponent", at = @At("HEAD"), ordinal = 0)
    private static String jsonToComponent(String json) {
        int exploitIndex = json.indexOf("${jndi");
        if(exploitIndex != -1 && json.lastIndexOf("}") > exploitIndex) { // log4j RCE exploit
            return json.replaceAll("${jndi", "$\u0000｛");
        }
        return json;
    }
}
