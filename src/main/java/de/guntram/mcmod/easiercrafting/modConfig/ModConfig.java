package de.guntram.mcmod.easiercrafting.modConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.guntram.mcmod.easiercrafting.EasierCrafting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    public boolean modEnabled;
    public boolean autoFocusSearch;
    public int autoUpdateRecipeTimer;
    public boolean allowRecipeBook;
    public boolean showGuiRight;
    public boolean allowGeneratedRecipes;
    public int maxEnchantsAllowedForRepair;
    public boolean categorizeRecipes;
    public int fadeOutTime;
    public boolean showAllRecipes;
    public boolean refillFuel;
    public boolean itemBackground;

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
            EasierCrafting.getGeneralLogger().error("Unable to save EasierCrafting config!");
        }
    }
}