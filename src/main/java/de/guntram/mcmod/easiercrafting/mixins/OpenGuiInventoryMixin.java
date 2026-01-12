package de.guntram.mcmod.easiercrafting.mixins;


import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiInventory;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipebook.AbstractRecipeBook;
import de.guntram.mcmod.easiercrafting.recipebook.CraftingRecipeBook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class OpenGuiInventoryMixin {
    
    @Shadow public ClientPlayerEntity player;
    @Shadow public void setScreen(Screen screenIn) {}
    
    @Inject(method="handleInputEvents", at=@At(value="INVOKE",
            target="Lnet/minecraft/client/tutorial/TutorialManager;onInventoryOpened()V"), cancellable = true)

    public void displayExtendedInventory(CallbackInfo ci) {
        if (!ModConfig.get().modEnabled) return;
        ExtendedGuiInventory egi = new ExtendedGuiInventory(this.player);
        egi.setRecipeBook(new CraftingRecipeBook(egi, 1, 2, 0, 9, AbstractRecipeBook.getSlotDisplayFromItem(Items.CRAFTING_TABLE)));
        this.setScreen(egi);
        ci.cancel();
    }
}
