package net.ccbluex.liquidbounce.injection.mixins.minecraft.text;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(MutableText.class)
public interface MixinMutableTextAccessor {

    /**
     * @param siblings should be mutable list, unless you are sure it will not be modified!
     */
    @Invoker("<init>")
    static MutableText create(TextContent content, List<Text> siblings, Style style) {
        throw new AssertionError();
    }

}
