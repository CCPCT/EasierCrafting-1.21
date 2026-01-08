package de.guntram.mcmod.easiercrafting.modConfig;

import net.fabricmc.loader.api.FabricLoader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    public boolean autoFocusSearch;
    public int autoUpdateRecipeTimer;
    public boolean allowRecipeBook;
    public boolean showGuiRight;
    public boolean allowGeneratedRecipes;
    public int maxEnchantsAllowedForRepair;
    public boolean categorizeRecipes;
    public int loomClickSpeed;
    public int fadeOutTime;
    public boolean showAllRecipes;

    public static ModConfig getInstance() {
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
            e.printStackTrace();
            INSTANCE = new ModConfig();
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(getInstance()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ModConfig get() {
        return INSTANCE;
    }


    public static int getAutoUpdateRecipeTimer() {
        return getInstance().autoUpdateRecipeTimer;
    }
    public static boolean getAutoFocusSearch() {
        return getInstance().autoFocusSearch;
    }
    public static boolean getAllowMinecraftRecipeBook() {
        return getInstance().allowRecipeBook;
    }
    public static boolean getShowGuiRight() {
        return getInstance().showGuiRight;
    }
    public static boolean getAllowGeneratedRecipes() {
        return getInstance().allowGeneratedRecipes;
    }
    public static int getMaxEnchantsAllowedForRepair() {
        return getInstance().maxEnchantsAllowedForRepair;
    }
    public static boolean getCategorizeRecipes() {
        return getInstance().categorizeRecipes;
    }
    public static int getLoomClickSpeed() {
        return getInstance().loomClickSpeed;
    }
    public static int getFadeoutTime() {
        return getInstance().fadeOutTime;
    }
}