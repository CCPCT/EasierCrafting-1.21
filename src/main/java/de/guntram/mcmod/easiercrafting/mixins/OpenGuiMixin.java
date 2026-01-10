package de.guntram.mcmod.easiercrafting.mixins;

import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiCrafting;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiFurnace;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiStonecutter;
import de.guntram.mcmod.easiercrafting.recipebook.CraftingRecipeBook;
import de.guntram.mcmod.easiercrafting.recipebook.FurnaceRecipeBook;
import de.guntram.mcmod.easiercrafting.recipebook.StonecutterRecipeBook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreens.class)
public class OpenGuiMixin {

    @Inject(method = "open", at = @At("HEAD"), cancellable = true)
    private static <T extends ScreenHandler> void checkCraftScreen(ScreenHandlerType<T> type, MinecraftClient client, int id, Text title, CallbackInfo ci) {
        assert client.player != null;
        // see which screen opened
        // crafting table
        if (type == ScreenHandlerType.CRAFTING) {
            CraftingScreenHandler handler = ScreenHandlerType.CRAFTING.create(id, client.player.getInventory());
            ExtendedGuiCrafting screen = new ExtendedGuiCrafting(handler, client.player.getInventory(), title);
            // Your custom recipe book setup
            screen.setRecipeBook(new CraftingRecipeBook(screen, 1, 3, 0, 10, new SlotDisplay.StackSlotDisplay(new ItemStack(Items.CRAFTING_TABLE))));
            openScreen(client, handler, screen);
            ci.cancel();
        }
        // stonecutter
        else if (type == ScreenHandlerType.STONECUTTER) {
            StonecutterScreenHandler handler = ScreenHandlerType.STONECUTTER.create(id, client.player.getInventory());
            ExtendedGuiStonecutter screen = new ExtendedGuiStonecutter(handler, client.player.getInventory(), title);
            screen.setRecipeBook(new StonecutterRecipeBook(screen, 0, 1, 1, 2, new SlotDisplay.StackSlotDisplay(new ItemStack(Items.STONECUTTER))));
            openScreen(client, handler, screen);
            ci.cancel();
        }
        // furnace
        else if (type == ScreenHandlerType.FURNACE) {
            FurnaceScreenHandler handler = ScreenHandlerType.FURNACE.create(id, client.player.getInventory());
            ExtendedGuiFurnace screen = new ExtendedGuiFurnace(handler, client.player.getInventory(), title);
            screen.setRecipeBook(new FurnaceRecipeBook(screen, 0, 1, 2, 3, new SlotDisplay.StackSlotDisplay(new ItemStack(Items.FURNACE)),1));
            openScreen(client, handler, screen);
            ci.cancel();
        }
        // smoker
        else if (type == ScreenHandlerType.SMOKER) {
            //ci.cancel();
        }
        // blast furnace
        else if (type == ScreenHandlerType.BLAST_FURNACE) {
            //ci.cancel();
        }
    }

    // Helper to mirror vanilla behavior correctly
    @Unique
    private static void openScreen(MinecraftClient client, ScreenHandler handler, HandledScreen<?> screen) {
        assert client.player != null;
        client.player.currentScreenHandler = handler;
        client.setScreen(screen);
    }
}
