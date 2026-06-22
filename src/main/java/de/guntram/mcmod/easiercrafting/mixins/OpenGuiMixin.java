package de.guntram.mcmod.easiercrafting.mixins;

import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedCraftingScreen;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedFurnaceScreen;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedLoomScreen;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedStoneCutterScreen;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipebook.CraftingRecipeBook;
import de.guntram.mcmod.easiercrafting.recipebook.FurnaceRecipeBook;
import de.guntram.mcmod.easiercrafting.recipebook.LoomRecipeBook;
import de.guntram.mcmod.easiercrafting.recipebook.StonecutterRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static de.guntram.mcmod.easiercrafting.recipebook.AbstractRecipeBook.getSlotDisplay;

@Mixin(MenuScreens.class)
public abstract class OpenGuiMixin {

    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static <T extends AbstractContainerMenu> void checkCraftScreen(MenuType<T> type, Minecraft client, int id, Component title, CallbackInfo ci) {
        assert client.player != null;
        if (!ModConfig.get().modEnabled) return;

        Inventory inventory = client.player.getInventory();

        // see which screen opened

        // crafting table
        if (type == MenuType.CRAFTING) {
            CraftingMenu menu = MenuType.CRAFTING.create(id, inventory);
            ExtendedCraftingScreen screen = new ExtendedCraftingScreen(menu, inventory, title);
            // Your custom recipe book setup
            screen.setRecipeBook(new CraftingRecipeBook(screen, 1, 3, 0, 10, getSlotDisplay(Items.CRAFTING_TABLE)));
            openScreen(client, menu, screen);
        }
        // stonecutter
        else if (type == MenuType.STONECUTTER) {
            StonecutterMenu menu = MenuType.STONECUTTER.create(id, inventory);
            ExtendedStoneCutterScreen screen = new ExtendedStoneCutterScreen(menu, inventory, title);
            screen.setRecipeBook(new StonecutterRecipeBook(screen));
            openScreen(client, menu, screen);
        }
        // loom
        else if (type == MenuType.LOOM) {
            LoomMenu menu = MenuType.LOOM.create(id, inventory);
            ExtendedLoomScreen screen = new ExtendedLoomScreen(menu, inventory, title);
            screen.setRecipeBook(new LoomRecipeBook(screen));
            openScreen(client, menu, screen);
        }
        // furnace
        else if (type == MenuType.FURNACE) {
            FurnaceMenu menu = MenuType.FURNACE.create(id, inventory);
            ExtendedFurnaceScreen screen = new ExtendedFurnaceScreen(menu, inventory, title);
            screen.setRecipeBook(new FurnaceRecipeBook(screen, getSlotDisplay(Items.FURNACE)));
            openScreen(client, menu, screen);
        }
        // smoker
        else if (type == MenuType.SMOKER) {
            FurnaceMenu menu = MenuType.FURNACE.create(id, inventory);
            ExtendedFurnaceScreen screen = new ExtendedFurnaceScreen(menu, inventory, title);
            screen.setRecipeBook(new FurnaceRecipeBook(screen, getSlotDisplay(Items.SMOKER)));
            openScreen(client, menu, screen);
        }
        // blast furnace
        else if (type == MenuType.BLAST_FURNACE) {
            FurnaceMenu menu = MenuType.FURNACE.create(id, inventory);
            ExtendedFurnaceScreen screen = new ExtendedFurnaceScreen(menu, inventory, title);
            screen.setRecipeBook(new FurnaceRecipeBook(screen, getSlotDisplay(Items.BLAST_FURNACE)));
            openScreen(client, menu, screen);
        } else {
            return;
        }
        ci.cancel();
    }

    // Helper to mirror vanilla behaviour correctly
    @Unique
    private static void openScreen(Minecraft client, AbstractContainerMenu menu, AbstractContainerScreen<?> screen) {
        assert client.player != null;
        client.player.containerMenu = menu;
        client.gui.setScreen(screen);
    }
}
