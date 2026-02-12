package de.guntram.mcmod.easiercrafting;

import de.guntram.mcmod.easiercrafting.extendedScreen.*;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipe.LoomRecipeHandler;
import de.guntram.mcmod.easiercrafting.recipebook.FurnaceRecipeBook;
import de.guntram.mcmod.easiercrafting.recipebook.LoomRecipeBook;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;


public class EasierCrafting implements ClientModInitializer 
{
    public static final String MODID="easiercrafting";
    public static final String MODNAME="EasierCrafting";
    public static RecipeBookCategory SPECIAL_CAT;
    private static Logger LOGGER;
    public static boolean updateAllowed = true; // block update when crafting
    private static String ip;

    public static KeyBinding refreshRecipeKey;

    @Override
    public void onInitializeClient() {
        LOGGER = LogManager.getLogger(this.getClass());

        ModConfig.load();
        info("loaded config");

        SPECIAL_CAT = Registry.register(
                Registries.RECIPE_BOOK_CATEGORY,
                Identifier.of(EasierCrafting.MODID, "special"),
                new RecipeBookCategory()
        );

        refreshRecipeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Refresh Recipe List", // translation key
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_TAB,       // default key
                "Easier Crafting"       // category in controls menu
        ));

        // do this when joining server/ world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ip = "local";
            // Check if we are on a remote server
            if (handler.getServerInfo() != null) {
                ip = handler.getServerInfo().address;
            }

            LoomRecipeHandler.loadAll(MinecraftClient.getInstance().getResourceManager(), ip);
            info("Loaded loom recipes: "+ LoomRecipeHandler.LOADED_RECIPES.size());

            // clear last fuel cache when joined new world/ server
            FurnaceRecipeBook.lastFuelUsed = null;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LoomRecipeBook.onTick();
        });
    }

    public static void updateRecipe(){
        if (!updateAllowed) return;
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof ExtendedGuiCrafting screen){
            screen.updateRecipe();
        } else if (currentScreen instanceof ExtendedGuiInventory screen) {
            screen.updateRecipe();
        } else if (currentScreen instanceof ExtendedGuiFurnace screen) {
            screen.updateRecipe();
        } else if (currentScreen instanceof ExtendedGuiStonecutter screen) {
            screen.updateRecipe();
        } else if (currentScreen instanceof ExtendedGuiLoom screen) {
            screen.updateRecipe();
        }
    }

    public static void info(String message){
        if (LOGGER==null) return;
        LOGGER.info("[EC+] {}", message);
    }

    public static void warn(String message){
        if (LOGGER==null) return;
        LOGGER.warn("[EC+] {}", message);
    }

    public static void error(String message){
        if (LOGGER==null) return;
        LOGGER.error("[EC+] {}", message);
    }

    public static String getIp() {
        return ip;
    }

}
