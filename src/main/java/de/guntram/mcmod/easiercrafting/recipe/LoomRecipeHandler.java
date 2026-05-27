package de.guntram.mcmod.easiercrafting.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.guntram.mcmod.easiercrafting.EasierCrafting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;

import static de.guntram.mcmod.easiercrafting.EasierCrafting.info;
import static de.guntram.mcmod.easiercrafting.EasierCrafting.warn;

public class LoomRecipeHandler {
    private static final Gson GSON = new Gson();
    public static final List<LoomRecipe> LOADED_RECIPES = new ObjectArrayList<>();
    public static final List<LoomRecipe> customRecipes = new ObjectArrayList<>();

    public static void loadAll(ResourceManager manager, String ip) {
        LOADED_RECIPES.clear();

        // The Map now holds a List of Resources for each Identifier
        Map<Identifier, List<Resource>> resourceMap = manager.listResourceStacks("loom_recipes",
                id -> id.getPath().endsWith(".json") && !id.getPath().startsWith("_"));

        resourceMap.forEach((id, resources) -> {
            // Grab the last one in the list (the one with the highest priority)
            Resource resource = resources.getLast();

            try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                LoomRecipe recipe = GSON.fromJson(reader, LoomRecipe.class);
                if (recipe.serverIp()==null || !recipe.serverIp().equals(ip)) {
                    LOADED_RECIPES.add(recipe);
                    info("loading: "+id.getNamespace());
                } else {
                    info("skipped: "+id.getNamespace());
                }
            } catch (Exception e) {
                warn("Failed to load recipe: " + id);
            }
        });

        loadFromConfig(ip);
    }

    private static void loadFromConfig(String ip) {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("loom_recipes");
        File folder = configPath.toFile();

        if (!folder.exists()) return;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json") && !name.startsWith("_"));
        if (files == null) return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                LoomRecipe recipe = GSON.fromJson(reader, LoomRecipe.class);

                // Apply the same IP filtering logic
                if (recipe.serverIp() == null || !recipe.serverIp().equals(ip)) {
                    LOADED_RECIPES.add(recipe);
                    info("Loaded config recipe: " + file.getName());
                }
            } catch (Exception e) {
                warn("Failed to load config recipe: " + file.getName());
            }
        }
    }

    public static void saveRecipe(LoomRecipe recipe) {
        String fileName;

        if (recipe.name() != null && !recipe.name().isEmpty()) {
            // Use provided name, sanitize for Windows/Linux safety
            fileName = recipe.name().replaceAll("[^a-zA-Z0-9-_\\s]", "_");
        } else {
            // Start with base banner color
            StringBuilder sb = new StringBuilder(recipe.baseBanner().substring(0,2));

            for (var step : recipe.steps()) {
                sb.append("_");

                String pattern = step.pattern();
                String shortPattern = pattern.contains(":") ? pattern.split(":")[1] : pattern;

                if (shortPattern.length() >= 2) {
                    sb.append(shortPattern, 0, 2);
                } else {
                    sb.append(shortPattern); // Fallback if pattern name is 1 char
                }
            }
            fileName = sb.toString();
        }

        // Standard File Writing Logic
        Path path = FabricLoader.getInstance().getConfigDir().resolve("loom_recipes/" + fileName + ".json");
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(path.toFile())) {
            prettyGson.toJson(recipe, writer);
            info("Saved recipe: " + fileName);
        } catch (Exception e) {
            warn("Failed to save: " + fileName);
        }
    }

    public static LoomRecipe parseRawList(List<String> rawLines, String ip) {
        if (rawLines.isEmpty()) return null;


        // Parse by command
        if (rawLines.size()==1){
            String line = rawLines.getFirst();
            if (!line.contains("_banner")) return null;
            String[] preBannerName = line.substring(0, line.indexOf("_banner")+7).split(" ");
            String bannerName;
            if (preBannerName[preBannerName.length-1].contains("minecraft:")) {
                bannerName = preBannerName[preBannerName.length-1];
            } else {
                bannerName = "minecraft:"+preBannerName[preBannerName.length-1];
            }
            if (line.contains("banner_patterns=[")){
                // new command
                String[] arguments = line.substring(line.indexOf("banner_patterns=[")+85-68,line.indexOf("]")).replaceAll("[ \\[\\]}\"{]", "").split("[,:]");
                EasierCrafting.info(arguments[0]+arguments[1]);
                boolean colour = true;
                List<String> colourList = new ObjectArrayList<>(6);
                List<String> patternList = new ObjectArrayList<>(6);
                for (String argument : arguments) {
                    switch (argument) {
                        case "minecraft" -> {}
                        case "color" -> colour = true;
                        case "pattern" -> colour = false;
                        default -> {
                            info(argument);
                            if (colour) {
                                colourList.add(argument);
                            } else {
                                patternList.add("minecraft:"+argument);
                            }
                        }
                    }
                }

                if (colourList.size()!=patternList.size()) return null;
                List<LoomRecipe.BannerStep> bannerSteps = new ObjectArrayList<>(colourList.size());
                for (int i = 0; i < colourList.size(); i++) {
                    bannerSteps.add(new LoomRecipe.BannerStep(colourList.get(i), patternList.get(i)));
                }

                info(bannerName);

                return new LoomRecipe("parsed", ip, bannerName, bannerSteps);

            } else if (line.contains("BlockEntityTag")) {
                // old command

            }
            return null;
        }

        // Parse by NBT
        // 1. Parse the Base Banner (First Line)
        // "light_blue#wall_banner" -> base colour is light_blue
        String[] baseParts = rawLines.getFirst().replaceAll("[ ,\"]", "").split("#");
        nbt:
        if (baseParts.length == 2 && Objects.equals(baseParts[1], "wall_banner")) {
            String baseColor = baseParts[0];
            // Map "wall_banner" or "banner" to the actual item ID
            String baseBannerId = "minecraft:" + baseColor + "_banner";

            // 2. Parse the Steps (Remaining Lines)
            List<LoomRecipe.BannerStep> steps = new ArrayList<>();
            for (int i = 1; i < rawLines.size(); i++) {
                String[] stepParts = rawLines.get(i).replaceAll("[ ,\"]", "").split("#");
                if (stepParts.length != 2) break nbt;

                String color = stepParts[0];
                String pattern = stepParts[1];

                // Ensure pattern has namespace (e.g., "circle" -> "minecraft:circle")
                if (!pattern.contains(":")) {
                    pattern = "minecraft:" + pattern;
                }

                steps.add(new LoomRecipe.BannerStep(color, pattern));
            }

            return new LoomRecipe("parsed", ip, baseBannerId, steps);
        }

        EasierCrafting.warn("Not a loom recipe...");
        return null;
    }


    public static void onPasteButtonClicked() {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();

        String[] lines = clipboard.split("\\r?\\n");

        List<String> rawList = Arrays.asList(lines);
        LoomRecipe recipe = parseRawList(rawList, EasierCrafting.getIp());

        if (recipe != null) {
            EasierCrafting.info("Found recipe :D");
            EasierCrafting.info(recipe.baseBanner());
            EasierCrafting.info(recipe.steps().getFirst().dye());
            EasierCrafting.info(recipe.steps().getFirst().pattern());
            if (LoomRecipeHandler.customRecipes.contains(recipe)) {
                EasierCrafting.info("Recipe already saved...");
            } else {
                EasierCrafting.info("Added recipe to list");
                LoomRecipeHandler.customRecipes.add(recipe);
            }
        } else {
            EasierCrafting.warn("cant find recipe in clip board:(");
        }
    }
}
