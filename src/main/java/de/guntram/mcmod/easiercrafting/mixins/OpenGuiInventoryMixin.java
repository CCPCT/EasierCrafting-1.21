package de.guntram.mcmod.easiercrafting.mixins;


import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedInventoryScreen;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipebook.AbstractRecipeBook;
import de.guntram.mcmod.easiercrafting.recipebook.CraftingRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class OpenGuiInventoryMixin {

    @Final
    @Shadow public Gui gui;

    @Inject(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/tutorial/Tutorial;onOpenInventory()V"
            ),
            cancellable = true
    )

    public void displayExtendedInventory(CallbackInfo ci) {
        if (!ModConfig.get().modEnabled) return;

        Player player = Minecraft.getInstance().player;
        assert player != null;
        ExtendedInventoryScreen egi = new ExtendedInventoryScreen(player);
        egi.setRecipeBook(new CraftingRecipeBook(egi, 1, 2, 0, 9, AbstractRecipeBook.getSlotDisplay(Items.CRAFTING_TABLE)));
        this.gui.setScreen(egi);
        ci.cancel();
    }

}
