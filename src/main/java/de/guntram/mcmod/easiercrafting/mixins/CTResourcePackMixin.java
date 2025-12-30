package de.guntram.mcmod.easiercrafting.mixins;

import de.guntram.mcmod.crowdintranslate.CTResourcePack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CTResourcePack.class)
public class CTResourcePackMixin {
    /**
     * @author taoge407
     * @reason fucking mojang, privatization the construction method of Identifier, using Identifier.of(namespace, path) instead.
     */
    @Overwrite()
    private static Identifier fromPath(String path)
    {
        if (path.startsWith("assets/"))
            path = path.substring("assets/".length());
        String[] split = path.split("/", 2);
        return Identifier.of(split[0], split[1]);
    }
}
