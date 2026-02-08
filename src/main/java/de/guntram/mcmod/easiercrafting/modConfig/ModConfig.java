package de.guntram.mcmod.easiercrafting.modConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.guntram.mcmod.easiercrafting.EasierCrafting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    public boolean modEnabled = true;
    public boolean autoFocusSearch = false;
    public int autoUpdateRecipeTimer = 20;
    public boolean allowRecipeBook = false;
    public boolean showGuiRight = true;
    public boolean allowGeneratedRecipes = true;
    public int maxEnchantsAllowedForRepair = 0;
    public boolean categorizeRecipes = true;
    public int fadeOutTime = 10;
    public boolean showAllRecipes = true;
    public boolean refillFuel = true;
    public boolean recipeBackground = false;
    public int itemDisplaySpacing = 1;
    public boolean enableTrading = true;
    public int itemsPerRow = 9;
    public boolean storeYarnRecipes = false;

    public static ModConfig get() {
        if (INSTANCE==null)
            INSTANCE = new ModConfig();
        return INSTANCE;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ModConfig INSTANCE;


    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("easier-crafting.json");

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(Files.newBufferedReader(CONFIG_PATH), ModConfig.class);
            } else {
                INSTANCE = new ModConfig();
                save();
            }
        } catch (IOException e) {
            INSTANCE = new ModConfig();
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(get()));
        } catch (IOException e) {
            EasierCrafting.warn("Unable to save EasierCrafting config!");
        }
    }
}