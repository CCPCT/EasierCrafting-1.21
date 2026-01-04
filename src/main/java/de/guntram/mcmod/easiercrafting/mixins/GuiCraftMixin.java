package de.guntram.mcmod.easiercrafting.mixins;

import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiCrafting;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiStonecutter;
import de.guntram.mcmod.easiercrafting.recipe.CraftingRecipeBook;
import de.guntram.mcmod.easiercrafting.recipe.StonecutterRecipeBook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreens.class)
public class GuiCraftMixin {

    @Inject(method = "open", at = @At("HEAD"), cancellable = true)
    private static <T extends ScreenHandler> void checkCraftScreen(ScreenHandlerType<T> type, MinecraftClient client, int id, Text title, CallbackInfo ci) {

        // 1. Precise type comparison
        if (type == ScreenHandlerType.CRAFTING) {
            CraftingScreenHandler handler = ScreenHandlerType.CRAFTING.create(id, client.player.getInventory());
            ExtendedGuiCrafting screen = new ExtendedGuiCrafting(handler, client.player.getInventory(), title);

            // Your custom recipe book setup
            screen.setRecipeBook(new CraftingRecipeBook(screen, 1, 3, 0, 10));

            openScreen(client, handler, screen);
            ci.cancel();
        }
        else if (type == ScreenHandlerType.STONECUTTER) {
            StonecutterScreenHandler handler = ScreenHandlerType.STONECUTTER.create(id, client.player.getInventory());
            ExtendedGuiStonecutter screen = new ExtendedGuiStonecutter(handler, client.player.getInventory(), title);

            screen.setRecipeBook(new StonecutterRecipeBook(screen, 0, 1, 1, 2));

            openScreen(client, handler, screen);
            ci.cancel();
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
