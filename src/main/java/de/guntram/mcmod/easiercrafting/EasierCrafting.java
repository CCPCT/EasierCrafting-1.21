package de.guntram.mcmod.easiercrafting;

import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipebook.FurnaceRecipeBook;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class EasierCrafting implements ClientModInitializer 
{
    public static final String MODID="easiercrafting";
    public static final String MODNAME="EasierCrafting";
    public static RecipeBookCategory SPECIAL_CAT;
    private static Logger LOGGER;

    public static Logger getGeneralLogger(){
        return LOGGER;
    }

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        System.out.println("[EasierCrafting] Loaded");

        LOGGER = LogManager.getLogger(this.getClass());

        SPECIAL_CAT = Registry.register(
                Registries.RECIPE_BOOK_CATEGORY,
                Identifier.of(EasierCrafting.MODID, "special"),
                new RecipeBookCategory()
        );

        // do this when joining server/ world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // clear last fuel cache when joined new world/ server
            FurnaceRecipeBook.lastFuelUsed = null;
        });
    }
}
