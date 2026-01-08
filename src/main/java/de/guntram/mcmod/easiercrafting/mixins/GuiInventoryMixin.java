package de.guntram.mcmod.easiercrafting.mixins;


import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiInventory;
import de.guntram.mcmod.easiercrafting.recipe.CraftingRecipeBook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.display.SlotDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class GuiInventoryMixin {
    
    @Shadow public ClientPlayerEntity player;
    @Shadow public void setScreen(Screen screenIn) {}
    
    @Inject(method="handleInputEvents", at=@At(value="INVOKE",
            target="Lnet/minecraft/client/tutorial/TutorialManager;onInventoryOpened()V"), cancellable = true)

    public void displayExtendedInventory(CallbackInfo ci) {
        ExtendedGuiInventory egi = new ExtendedGuiInventory(this.player);
        egi.setRecipeBook(new CraftingRecipeBook(egi, 1, 2, 0, 9, new SlotDisplay.StackSlotDisplay(new ItemStack(Items.CRAFTING_TABLE))));
        this.setScreen(egi);
        ci.cancel();
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen != null) {
            // This tells you exactly what class is opening
            //System.out.println("Opening screen: " + screen.getClass().getSimpleName());

            if (screen instanceof InventoryScreen) {
                // Logic specifically for the player inventory
            } else if (screen instanceof GenericContainerScreen) {
                // Logic for chests
            }
        }
    }
}
